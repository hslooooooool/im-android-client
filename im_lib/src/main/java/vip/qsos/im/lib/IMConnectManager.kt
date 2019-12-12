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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicLong

/**
 * @author : 华清松
 * 服务端连接管理与消息处理
 */
@SuppressLint("StaticFieldLeak")
class IMConnectManager private constructor(override val mContext: Context) : IConnectManager {

    companion object {
        const val READ_BUFFER_SIZE = 2048
        const val WRITE_BUFFER_SIZE = 1024
        /**消息读取间隔时长，毫秒。
         * 应比链接活跃时长小，以保证重连功能的正确性，读取线程将闲置【120】秒，超过后将发起手动断开连接请求
         * @see CONNECT_ALIVE_TIME_OUT
         * */
        const val CONNECT_READ_IDLE_TIME = 120 * 1000L
        /**连接服务器的超时时长，毫秒*/
        const val CONNECT_TIME_OUT = 10 * 1000
        /**连接活跃时长，毫秒*/
        const val CONNECT_ALIVE_TIME_OUT = 150 * 1000

        /**消息读取闲置管理线程，当消息读取闲置时间一到，将主动断开连接，保证下一次重连成功*/
        private val CONNECT_READ_IDLE_THREAD =
            HandlerThread("SocketReadIdle-", Process.THREAD_PRIORITY_BACKGROUND)

        init {
            CONNECT_READ_IDLE_THREAD.start()
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

    override val mLastReadTime = AtomicLong(0)
    override val mSemaphore = Semaphore(1, true)
    override var mSocketChannel: SocketChannel? = null
    override val mMessageEncoder: ClientMessageEncoder = ClientMessageEncoder()
    override val mMessageDecoder: ClientMessageDecoder = ClientMessageDecoder()
    override var mReadBuffer: ByteBuffer = ByteBuffer.allocate(READ_BUFFER_SIZE)
    override val mSocketSendExecutor: ExecutorService = Executors.newFixedThreadPool(1) { r ->
        Thread(r, "SocketSend-")
    }
    override val mSocketConnectExecutor: ExecutorService = Executors.newFixedThreadPool(1) { r ->
        Thread(r, "SocketConnect-")
    }
    override var isConnected: Boolean = false
        get() {
            field = mSocketChannel != null && mSocketChannel!!.isConnected
            LogUtils.logger.connectState(field)
            return field
        }

    override val mConnectCloseHandler = Handler(CONNECT_READ_IDLE_THREAD.looper) {
        checkAndCloseConnect()
        true
    }

    override fun connect(host: String, port: Int) {
        if (!CIMPushManager.isNetworkConnected(mContext)) {
            val intent = Intent()
            intent.setPackage(mContext.packageName)
            intent.action = IMConstant.IntentAction.ACTION_CONNECTION_FAILED
            mContext.sendBroadcast(intent)
            return
        }
        if (isConnected) {
            return
        }
        mSocketConnectExecutor.execute(Runnable {
            if (isConnected) {
                return@Runnable
            }
            LogUtils.logger.connectStart(host, port)

            IMCacheHelper.putBoolean(mContext, IMCacheHelper.KEY_CIM_CONNECTION_STATE, false)
            mSemaphore.acquire()
            try {
                mSocketChannel = SocketChannel.open()
                mSocketChannel!!.configureBlocking(true)
                mSocketChannel!!.socket().tcpNoDelay = true
                mSocketChannel!!.socket().keepAlive = true
                mSocketChannel!!.socket().receiveBufferSize = READ_BUFFER_SIZE
                mSocketChannel!!.socket().sendBufferSize = WRITE_BUFFER_SIZE
                mSocketChannel!!.socket().connect(InetSocketAddress(host, port), CONNECT_TIME_OUT)
                mSemaphore.release()

                this.channelCreated()

                var result = 1
                /**链接成功，开启循环，读取通道传递的数据*/
                while (result > 0) {
                    /**下方代码将在Socket传递消息时主动读取一次*/
                    result = mSocketChannel!!.read(mReadBuffer)
                    if (result > 0) {
                        if (mReadBuffer.position() == mReadBuffer.capacity()) {
                            mReadBuffer = extendByteBuffer(mReadBuffer)
                        }
                        this.handelSocketReadEvent(result)
                    }
                }
                /**链接失败，无数据*/
                this.handelSocketReadEvent(result)
            } catch (ignore: ConnectException) {
                this.handleConnectTimeoutEvent()
            } catch (ignore: SocketTimeoutException) {
                this.handleConnectTimeoutEvent()
            } catch (ignore: IOException) {
                this.handelDisconnectedEvent()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                mSemaphore.release()
            }
        })
    }

    override fun destroy() {
        closeConnect()
    }

    override fun closeConnect() {
        if (isConnected) {
            try {
                mSocketChannel?.close()
            } catch (ignore: IOException) {
            } finally {
                this.clearAll()
            }
        }
    }

    override fun send(body: IProtobufAble) {
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
                LogUtils.logger.sendException(e)
                result = -1
            } finally {
                mSemaphore.release()
                if (result <= 0) {
                    closeConnect()
                } else {
                    messageSentSuccess(body)
                }
            }
        })
    }

    override fun channelCreated() {
        LogUtils.logger.connectCreated(mSocketChannel!!)
        mLastReadTime.set(System.currentTimeMillis())
        mConnectCloseHandler.sendEmptyMessageDelayed(0, CONNECT_READ_IDLE_TIME)
        val intent = Intent()
        intent.setPackage(mContext.packageName)
        intent.action = IMConstant.IntentAction.ACTION_CONNECTION_SUCCESS
        mContext.sendBroadcast(intent)
    }

    override fun clearAll() {
        LogUtils.logger.connectClosed(mSocketChannel!!)
        mConnectCloseHandler.removeMessages(0)
        mLastReadTime.set(0)
        mReadBuffer.clear()
        mSemaphore.release()
        if (mReadBuffer.capacity() > READ_BUFFER_SIZE) {
            mReadBuffer = ByteBuffer.allocate(READ_BUFFER_SIZE)
        }
        val intent = Intent()
        intent.setPackage(mContext.packageName)
        intent.action = IMConstant.IntentAction.ACTION_CONNECTION_CLOSED
        mContext.sendBroadcast(intent)
    }

    override fun checkAndCloseConnect() {
        val idle = System.currentTimeMillis() - mLastReadTime.get()
        LogUtils.logger.connectReadIdle(mSocketChannel!!, idle)
        if (idle >= CONNECT_ALIVE_TIME_OUT) {
            /**当前时间距离最近一次通信的时间如果大于配置的连接活跃时间，则表明连接可能已断开，
             * 无论是否连接都应主动断开连接，以保证下次连接时成功*/
            closeConnect()
        }
    }

    override fun messageReceived(message: Any) {
        LogUtils.logger.received(mSocketChannel!!, message)
        when (message) {
            is HeartbeatRequest -> {
                this.send(HeartbeatResponse.instance)
            }
            is Message -> {
                val intent = Intent()
                intent.setPackage(mContext.packageName)
                intent.action = IMConstant.IntentAction.ACTION_MESSAGE_RECEIVED
                intent.putExtra(Message::class.java.name, message)
                mContext.sendBroadcast(intent)
            }
            is ReplyBody -> {
                val intent = Intent()
                intent.setPackage(mContext.packageName)
                intent.action = IMConstant.IntentAction.ACTION_REPLY_RECEIVED
                intent.putExtra(ReplyBody::class.java.name, message)
                mContext.sendBroadcast(intent)
            }
        }
    }

    override fun messageSentSuccess(message: Any) {
        LogUtils.logger.sendSuccess(mSocketChannel!!, message)
        if (message is SendBody) {
            val intent = Intent()
            intent.setPackage(mContext.packageName)
            intent.action = IMConstant.IntentAction.ACTION_SENT_SUCCESS
            intent.putExtra(SendBody::class.java.name, message)
            mContext.sendBroadcast(intent)
        }
    }

    override fun messageSentFailed(message: Any) {
        LogUtils.logger.sendFailed(mSocketChannel!!, message)
        if (message is SendBody) {
            val intent = Intent()
            intent.setPackage(mContext.packageName)
            intent.action = IMConstant.IntentAction.ACTION_SENT_FAILED
            intent.putExtra(SendBody::class.java.name, message)
            mContext.sendBroadcast(intent)
        }
    }

    override fun handelDisconnectedEvent() {
        closeConnect()
    }

    override fun handleConnectTimeoutEvent() {
        LogUtils.logger.connectFailed(IMConstant.RECONNECT_INTERVAL_TIME)
        val intent = Intent()
        intent.setPackage(mContext.packageName)
        intent.action = IMConstant.IntentAction.ACTION_CONNECTION_FAILED
        mContext.sendBroadcast(intent)
    }

    override fun handelSocketReadEvent(result: Int) {
        if (result == -1) {
            /**消息读取错误，关闭连接*/
            closeConnect()
        } else {
            /**刷新最近消息读取时间*/
            this.markLastReadTime()

            mReadBuffer.position(0)
            /**编码读取的消息*/
            val message = mMessageDecoder.decode(mReadBuffer) ?: return

            this.messageReceived(message)
        }
    }

    override fun extendByteBuffer(mReadBuffer: ByteBuffer): ByteBuffer {
        val newBuffer = ByteBuffer.allocate(mReadBuffer.capacity() + READ_BUFFER_SIZE / 2)
        mReadBuffer.position(0)
        newBuffer.put(mReadBuffer)
        mReadBuffer.clear()
        return newBuffer
    }

    override fun markLastReadTime() {
        mLastReadTime.set(System.currentTimeMillis())
        mConnectCloseHandler.removeMessages(0)
        mConnectCloseHandler.sendEmptyMessageDelayed(0, CONNECT_READ_IDLE_TIME)
    }

}
