package vip.qsos.im.lib

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import vip.qsos.im.lib.coder.ClientMessageDecoder
import vip.qsos.im.lib.coder.ClientMessageEncoder
import vip.qsos.im.lib.coder.LogUtils
import vip.qsos.im.lib.constant.IMConstant
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
 * 服务端连接管理与消息处理
 */
@SuppressLint("StaticFieldLeak")
class IMConnectManager private constructor(val context: Context) {

    companion object {
        const val READ_BUFFER_SIZE = 2048
        const val WRITE_BUFFER_SIZE = 1024
        const val READ_IDLE_TIME = 120 * 1000
        /**链接超时时长，毫秒*/
        const val CONNECT_TIME_OUT = 10 * 1000
        /**链接活跃时长，毫秒*/
        const val CONNECT_ALIVE_TIME_OUT = 150 * 1000

        /**启动一个闲置线程，用于维护消息 Handler 发送*/
        private val IDLE_HANDLER_THREAD =
            HandlerThread("READ-IDLE", Process.THREAD_PRIORITY_BACKGROUND)

        init {
            IDLE_HANDLER_THREAD.start()
        }

        @Volatile
        private var manager: IMConnectManager? = null

        @Synchronized
        fun getManager(context: Context): IMConnectManager {
            return manager ?: IMConnectManager(context.applicationContext).apply {
                manager = this
            }
        }
    }

    /**最近读取时间*/
    private val mLastReadTime = AtomicLong(0)
    /**消息服务连接并发控制*/
    private val mSemaphore = Semaphore(1, true)
    /**消息通道*/
    private var mSocketChannel: SocketChannel? = null
    /**消息发送编码*/
    private val mMessageEncoder = ClientMessageEncoder()
    /**消息接收编码*/
    private val mMessageDecoder = ClientMessageDecoder()

    /**消息读取暂存*/
    private var mReadBuffer = ByteBuffer.allocate(READ_BUFFER_SIZE)

    /**消息服务发送线程*/
    private val mSocketSendExecutor = Executors.newFixedThreadPool(1) { r ->
        Thread(r, "SocketSend-")
    }
    /**消息服务连接线程*/
    private val mSocketConnectExecutor = Executors.newFixedThreadPool(1) { r ->
        Thread(r, "SocketConnect-")
    }

    /**消息服务是否已连接*/
    var isConnected: Boolean = false
        get() {
            field = mSocketChannel != null && mSocketChannel!!.isConnected
            LogUtils.logger.networkState(field)
            return field
        }

    /**消息会话生命周期控制*/
    private val mSessionIdleHandler = Handler(IDLE_HANDLER_THREAD.looper) {
        sessionIdle()
        true
    }

    private val heartbeatResponse: HeartbeatResponse
        get() = HeartbeatResponse.instance

    /**连接消息服务器*/
    fun connect(host: String, port: Int) {
        if (!CIMPushManager.isNetworkConnected(context)) {
            val intent = Intent()
            intent.setPackage(context.packageName)
            intent.action = IMConstant.IntentAction.ACTION_CONNECTION_FAILED
            context.sendBroadcast(intent)
            return
        }
        if (isConnected) {
            return
        }
        mSocketConnectExecutor.execute(Runnable {
            if (isConnected) {
                return@Runnable
            }
            LogUtils.logger.startConnect(host, port)

            CIMCacheManager.putBoolean(context, CIMCacheManager.KEY_CIM_CONNECTION_STATE, false)
            mSemaphore.acquire()
            try {
                mSocketChannel = SocketChannel.open()
                mSocketChannel!!.configureBlocking(true)
                mSocketChannel!!.socket().tcpNoDelay = true
                mSocketChannel!!.socket().keepAlive = true
                mSocketChannel!!.socket().receiveBufferSize = READ_BUFFER_SIZE
                mSocketChannel!!.socket().sendBufferSize = WRITE_BUFFER_SIZE
                mSocketChannel!!.socket().connect(InetSocketAddress(host, port), CONNECT_TIME_OUT)

                handelConnectedEvent()

                var result = 1
                /**链接成功，开启循环，读取通道传递的数据*/
                while (result > 0) {
                    result = mSocketChannel!!.read(mReadBuffer)
                    if (result > 0) {
                        if (mReadBuffer.position() == mReadBuffer.capacity()) {
                            mReadBuffer = extendByteBuffer(mReadBuffer)
                        }
                        handelSocketReadEvent(result)
                    }
                }
                /**链接失败，无数据*/
                handelSocketReadEvent(result)
            } catch (ignore: ConnectException) {
                handleConnectAbortedEvent()
            } catch (ignore: SocketTimeoutException) {
                handleConnectAbortedEvent()
            } catch (ignore: IOException) {
                handelDisconnectedEvent()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                mSemaphore.release()
            }
        })
    }

    /**销毁消息会话*/
    fun destroy() {
        closeSession()
    }

    /**关闭消息会话*/
    fun closeSession() {
        if (!isConnected) {
            return
        }
        try {
            mSocketChannel!!.close()
        } catch (ignore: IOException) {
        } finally {
            this.sessionClosed()
        }
    }

    /**客户端发送消息*/
    fun send(body: IProtobufAble) {
        if (!isConnected) {
            return
        }
        mSocketSendExecutor.execute(Runnable {
            if (!isConnected) {
                messageSentFailed(body)
                return@Runnable
            }
            mSemaphore.acquire()
            var result = 0
            try {
                val buffer = mMessageEncoder.encode(body)
                while (buffer.hasRemaining()) {
                    result += mSocketChannel!!.write(buffer)
                }
            } catch (e: Exception) {
                LogUtils.logger.messageSentException(e)
                result = -1
            } finally {
                mSemaphore.release()
                if (result <= 0) {
                    closeSession()
                } else {
                    messageSentSuccess(body)
                }
            }
        })
    }

    private fun sessionCreated() {
        LogUtils.logger.sessionCreated(mSocketChannel!!)

        mLastReadTime.set(System.currentTimeMillis())
        val intent = Intent()
        intent.setPackage(context.packageName)
        intent.action = IMConstant.IntentAction.ACTION_CONNECTION_SUCCESS
        context.sendBroadcast(intent)
    }

    /**关闭会话*/
    private fun sessionClosed() {
        mSessionIdleHandler.removeMessages(0)
        mLastReadTime.set(0)
        LogUtils.logger.sessionClosed(mSocketChannel!!)
        mReadBuffer.clear()
        if (mReadBuffer.capacity() > READ_BUFFER_SIZE) {
            mReadBuffer = ByteBuffer.allocate(READ_BUFFER_SIZE)
        }
        val intent = Intent()
        intent.setPackage(context.packageName)
        intent.action = IMConstant.IntentAction.ACTION_CONNECTION_CLOSED
        context.sendBroadcast(intent)
    }

    /**会话生命周期判断*/
    private fun sessionIdle() {
        LogUtils.logger.sessionIdle(mSocketChannel!!)
        /*用于解决，wifi情况下。偶而路由器与服务器断开连接时，客户端并没及时收到关闭事件，导致这样的情况下当前连接无效也不会重连的问题*/
        if (System.currentTimeMillis() - mLastReadTime.get() >= CONNECT_ALIVE_TIME_OUT) {
            closeSession()
        }
    }

    private fun messageReceived(obj: Any) {
        if (obj is Message) {
            val intent = Intent()
            intent.setPackage(context.packageName)
            intent.action = IMConstant.IntentAction.ACTION_MESSAGE_RECEIVED
            intent.putExtra(Message::class.java.name, obj)
            context.sendBroadcast(intent)
        }
        if (obj is ReplyBody) {
            val intent = Intent()
            intent.setPackage(context.packageName)
            intent.action = IMConstant.IntentAction.ACTION_REPLY_RECEIVED
            intent.putExtra(ReplyBody::class.java.name, obj)
            context.sendBroadcast(intent)
        }
    }

    private fun messageSentSuccess(message: Any) {
        LogUtils.logger.messageSentSuccess(mSocketChannel!!, message)
        if (message is SendBody) {
            val intent = Intent()
            intent.setPackage(context.packageName)
            intent.action = IMConstant.IntentAction.ACTION_SENT_SUCCESS
            intent.putExtra(SendBody::class.java.name, message)
            context.sendBroadcast(intent)
        }
    }

    private fun messageSentFailed(message: Any) {
        LogUtils.logger.messageSentFailed(mSocketChannel!!, message)
        if (message is SendBody) {
            val intent = Intent()
            intent.setPackage(context.packageName)
            intent.action = IMConstant.IntentAction.ACTION_SENT_FAILED
            intent.putExtra(SendBody::class.java.name, message)
            context.sendBroadcast(intent)
        }
    }

    private fun handelDisconnectedEvent() {
        closeSession()
    }

    private fun handleConnectAbortedEvent() {
        val interval =
            IMConstant.RECONNECT_INTERVAL_TIME - (5 * 1000 - Random().nextInt(15 * 1000))
        LogUtils.logger.connectFailure(interval)
        val intent = Intent()
        intent.setPackage(context.packageName)
        intent.action = IMConstant.IntentAction.ACTION_CONNECTION_FAILED
        intent.putExtra("interval", interval)
        context.sendBroadcast(intent)
    }

    private fun handelConnectedEvent() {
        this.sessionCreated()
        mSessionIdleHandler.sendEmptyMessageDelayed(0, READ_IDLE_TIME.toLong())
    }

    @Throws(IOException::class)
    private fun handelSocketReadEvent(result: Int) {
        if (result == -1) {
            /**链接失败，无数据，关闭会话*/
            closeSession()
            return
        }
        /**刷新消息读取时间*/
        this.markLastReadTime()

        mReadBuffer.position(0)
        /**编码读取的消息*/
        val message = mMessageDecoder.decode(mReadBuffer) ?: return

        LogUtils.logger.messageReceived(mSocketChannel!!, message)
        if (message is HeartbeatRequest) {
            /**识别为服务器期望客户端回执一条心跳消息，发送给服务器*/
            send(heartbeatResponse)
            return
        }
        this.messageReceived(message)
    }

    private fun extendByteBuffer(mReadBuffer: ByteBuffer): ByteBuffer {
        val newBuffer = ByteBuffer.allocate(mReadBuffer.capacity() + READ_BUFFER_SIZE / 2)
        mReadBuffer.position(0)
        newBuffer.put(mReadBuffer)
        mReadBuffer.clear()
        return newBuffer
    }

    /**重设最近消息接收时间与重连计时*/
    private fun markLastReadTime() {
        mLastReadTime.set(System.currentTimeMillis())
        mSessionIdleHandler.removeMessages(0)
        mSessionIdleHandler.sendEmptyMessageDelayed(0, READ_IDLE_TIME.toLong())
    }

}
