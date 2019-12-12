package vip.qsos.im.lib

import android.content.Context
import android.os.Handler
import vip.qsos.im.lib.IMConnectManager.Companion.CONNECT_READ_IDLE_TIME
import vip.qsos.im.lib.coder.ClientMessageDecoder
import vip.qsos.im.lib.coder.ClientMessageEncoder
import vip.qsos.im.lib.model.IProtobufAble
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicLong

/**
 * @author : 华清松
 * 服务端连接管理与消息处理接口
 */
interface IConnectManager {
    val mContext: Context
    /**最近读取时间*/
    val mLastReadTime: AtomicLong
    /**消息服务连接并发控制*/
    val mSemaphore: Semaphore
    /**消息通道*/
    var mSocketChannel: SocketChannel?
    /**消息发送编码*/
    val mMessageEncoder: ClientMessageEncoder
    /**消息接收编码*/
    val mMessageDecoder: ClientMessageDecoder
    /**消息读取暂存*/
    var mReadBuffer: ByteBuffer
    /**消息服务发送线程*/
    val mSocketSendExecutor: ExecutorService
    /**消息服务连接线程*/
    val mSocketConnectExecutor: ExecutorService
    /**消息服务是否已连接*/
    var isConnected: Boolean
    /**消息读取闲置管理，到达闲置时长后将收到断开连接消息
     * @sample Handler.sendEmptyMessageDelayed 发送断开连接消息
     * @see CONNECT_READ_IDLE_TIME 闲置时长
     * */
    val mConnectCloseHandler: Handler

    /**连接消息服务器*/
    fun connect(host: String, port: Int)

    /**消息通道已建立*/
    fun channelCreated()

    /**客户端发送消息*/
    fun send(body: IProtobufAble)

    /**销毁消息连接*/
    fun destroy()

    /**关闭消息会话*/
    fun closeConnect()

    /**清除资源占用*/
    fun clearAll()

    /**当前消息读取闲置时长判断。确认后将主动断开连接*/
    fun checkAndCloseConnect()

    /**收到服务器消息*/
    fun messageReceived(message: Any)

    /**消息发送成功*/
    fun messageSentSuccess(message: Any)

    /**消息发送失败*/
    fun messageSentFailed(message: Any)

    /**捕获到消息服务器断开*/
    fun handelDisconnectedEvent()

    /**捕获到消息连接失败或超时，此处应发起重连*/
    fun handleConnectTimeoutEvent()

    /**捕获到服务器消息数据*/
    @Throws(IOException::class)
    fun handelSocketReadEvent(result: Int)

    /**消息数据读取*/
    @Throws(IOException::class)
    fun extendByteBuffer(mReadBuffer: ByteBuffer): ByteBuffer

    /**重设最近消息接收时间与重连计时*/
    fun markLastReadTime()
}
