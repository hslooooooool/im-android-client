package vip.qsos.im.lib

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import vip.qsos.im.lib.coder.ClientMessageDecoder
import vip.qsos.im.lib.coder.ClientMessageEncoder
import vip.qsos.im.lib.coder.ImLogUtils
import vip.qsos.im.lib.constant.CIMConstant
import vip.qsos.im.lib.model.*
import java.io.IOException
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicLong

/**
 * @author : 华清松
 * 连接服务端管理与消息处理
 */
@SuppressLint("StaticFieldLeak")
class CIMConnectorManager private constructor(private val context: Context) {

    companion object {
        const val READ_BUFFER_SIZE = 2048
        const val WRITE_BUFFER_SIZE = 1024
        const val READ_IDLE_TIME = 120 * 1000
        const val CONNECT_TIME_OUT = 10 * 1000
        const val CONNECT_ALIVE_TIME_OUT = 150 * 1000

        private val IDLE_HANDLER_THREAD =
            HandlerThread("READ-IDLE", Process.THREAD_PRIORITY_BACKGROUND)

        init {
            IDLE_HANDLER_THREAD.start()
        }

        @Volatile
        private var manager: CIMConnectorManager? = null

        @Synchronized
        fun getManager(context: Context): CIMConnectorManager {
            return manager ?: CIMConnectorManager(context.applicationContext).apply {
                manager = this
            }
        }

    }

    private val lastReadTime = AtomicLong(0)
    private val semaphore = Semaphore(1, true)

    private var socketChannel: SocketChannel? = null
    private var readBuffer = ByteBuffer.allocate(READ_BUFFER_SIZE)
    private val workerExecutor = Executors.newFixedThreadPool(1) { r -> Thread(r, "worker-") }
    private val bossExecutor = Executors.newFixedThreadPool(1) { r -> Thread(r, "boss-") }
    private val messageEncoder = ClientMessageEncoder()
    private val messageDecoder = ClientMessageDecoder()

    val isConnected: Boolean
        get() = socketChannel != null && socketChannel!!.isConnected

    private val idleHandler = object : Handler(IDLE_HANDLER_THREAD.looper) {
        override fun handleMessage(m: android.os.Message) {
            sessionIdle()
        }
    }

    private val heartbeatResponse: HeartbeatResponse
        get() = HeartbeatResponse.getInstance()

    fun connect(host: String, port: Int) {
        if (!CIMPushManager.isNetworkConnected(context)) {
            val intent = Intent()
            intent.setPackage(context.packageName)
            intent.action = CIMConstant.IntentAction.ACTION_CONNECTION_FAILED
            context.sendBroadcast(intent)
            return
        }
        if (isConnected) {
            return
        }
        bossExecutor.execute(Runnable {
            if (isConnected) {
                return@Runnable
            }
            ImLogUtils.logger.startConnect(host, port)
            CIMCacheManager.putBoolean(context, CIMCacheManager.KEY_CIM_CONNECTION_STATE, false)
            semaphore.acquire()
            try {
                socketChannel = SocketChannel.open()
                socketChannel!!.configureBlocking(true)
                socketChannel!!.socket().tcpNoDelay = true
                socketChannel!!.socket().keepAlive = true
                socketChannel!!.socket().receiveBufferSize = READ_BUFFER_SIZE
                socketChannel!!.socket().sendBufferSize = WRITE_BUFFER_SIZE
                socketChannel!!.socket().connect(InetSocketAddress(host, port), CONNECT_TIME_OUT)
                handelConnectedEvent()
                var result = 1
                while (result > 0) {
                    result = socketChannel!!.read(readBuffer)
                    if (result > 0) {
                        if (readBuffer.position() == readBuffer.capacity()) {
                            extendByteBuffer()
                        }
                        handelSocketReadEvent(result)
                    }
                }
                handelSocketReadEvent(result)
            } catch (ignore: ConnectException) {
                handleConnectAbortedEvent()
            } catch (ignore: SocketTimeoutException) {
                handleConnectAbortedEvent()
            } catch (ignore: IOException) {
                handelDisconnectedEvent()
            } catch (ignore: Exception) {
            } finally {
                semaphore.release()
            }
        })
    }

    fun destroy() {
        closeSession()
    }

    fun closeSession() {
        if (!isConnected) {
            return
        }
        try {
            socketChannel!!.close()
        } catch (ignore: IOException) {
        } finally {
            this.sessionClosed()
        }
    }

    fun send(body: Protobufable) {
        if (!isConnected) {
            return
        }
        workerExecutor.execute {
            var result = 0
            try {
                semaphore.acquire()
                val buffer = messageEncoder.encode(body)
                while (buffer.hasRemaining()) {
                    result += socketChannel!!.write(buffer)
                }
            } catch (e: Exception) {
                result = -1
            } finally {
                semaphore.release()
                if (result <= 0) {
                    closeSession()
                } else {
                    messageSent(body)
                }
            }
        }
    }

    private fun sessionCreated() {
        ImLogUtils.logger.sessionCreated(socketChannel!!)
        lastReadTime.set(System.currentTimeMillis())
        val intent = Intent()
        intent.setPackage(context.packageName)
        intent.action = CIMConstant.IntentAction.ACTION_CONNECTION_SUCCESSED
        context.sendBroadcast(intent)
    }

    private fun sessionClosed() {
        idleHandler.removeMessages(0)
        lastReadTime.set(0)
        ImLogUtils.logger.sessionClosed(socketChannel!!)
        readBuffer.clear()
        if (readBuffer.capacity() > READ_BUFFER_SIZE) {
            readBuffer = ByteBuffer.allocate(READ_BUFFER_SIZE)
        }
        val intent = Intent()
        intent.setPackage(context.packageName)
        intent.action = CIMConstant.IntentAction.ACTION_CONNECTION_CLOSED
        context.sendBroadcast(intent)
    }

    private fun sessionIdle() {
        ImLogUtils.logger.sessionIdle(socketChannel!!)
        /*用于解决，wifi情况下。偶而路由器与服务器断开连接时，客户端并没及时收到关闭事件，导致这样的情况下当前连接无效也不会重连的问题*/
        if (System.currentTimeMillis() - lastReadTime.get() >= CONNECT_ALIVE_TIME_OUT) {
            closeSession()
        }
    }

    private fun messageReceived(obj: Any) {
        if (obj is Message) {
            val intent = Intent()
            intent.setPackage(context.packageName)
            intent.action = CIMConstant.IntentAction.ACTION_MESSAGE_RECEIVED
            intent.putExtra(Message::class.java.name, obj)
            context.sendBroadcast(intent)
        }
        if (obj is ReplyBody) {
            val intent = Intent()
            intent.setPackage(context.packageName)
            intent.action = CIMConstant.IntentAction.ACTION_REPLY_RECEIVED
            intent.putExtra(ReplyBody::class.java.name, obj)
            context.sendBroadcast(intent)
        }
    }

    private fun messageSent(message: Any) {
        ImLogUtils.logger.messageSent(socketChannel!!, message)
        if (message is SentBody) {
            val intent = Intent()
            intent.setPackage(context.packageName)
            intent.action = CIMConstant.IntentAction.ACTION_SENT_SUCCESSED
            intent.putExtra(SentBody::class.java.name, message)
            context.sendBroadcast(intent)
        }
    }

    private fun handelDisconnectedEvent() {
        closeSession()
    }

    private fun handleConnectAbortedEvent() {
        val interval = CIMConstant.RECONN_INTERVAL_TIME - (5 * 1000 - Random().nextInt(15 * 1000))
        ImLogUtils.logger.connectFailure(interval)
        val intent = Intent()
        intent.setPackage(context.packageName)
        intent.action = CIMConstant.IntentAction.ACTION_CONNECTION_FAILED
        intent.putExtra("interval", interval)
        context.sendBroadcast(intent)
    }

    private fun handelConnectedEvent() {
        sessionCreated()
        idleHandler.sendEmptyMessageDelayed(0, READ_IDLE_TIME.toLong())
    }

    @Throws(IOException::class)
    private fun handelSocketReadEvent(result: Int) {
        if (result == -1) {
            closeSession()
            return
        }
        markLastReadTime()
        readBuffer.position(0)
        val message = messageDecoder.doDecode(readBuffer) ?: return

        ImLogUtils.logger.messageReceived(socketChannel!!, message)
        if (isHeartbeatRequest(message)) {
            send(heartbeatResponse)
            return
        }
        this.messageReceived(message)
    }

    private fun extendByteBuffer() {
        val newBuffer = ByteBuffer.allocate(readBuffer.capacity() + READ_BUFFER_SIZE / 2)
        readBuffer.position(0)
        newBuffer.put(readBuffer)
        readBuffer.clear()
        readBuffer = newBuffer
    }

    private fun markLastReadTime() {
        lastReadTime.set(System.currentTimeMillis())
        idleHandler.removeMessages(0)
        idleHandler.sendEmptyMessageDelayed(0, READ_IDLE_TIME.toLong())
    }

    private fun isHeartbeatRequest(data: Any): Boolean {
        return data is HeartbeatRequest
    }

}
