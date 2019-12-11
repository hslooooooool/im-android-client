package vip.qsos.im.lib

import android.net.NetworkInfo

import vip.qsos.im.lib.model.Message
import vip.qsos.im.lib.model.ReplyBody
import vip.qsos.im.lib.model.SentBody

/**
 * CIM 主要事件接口
 */
interface CIMEventListener {

    /**
     * 监听器在容器里面的排序。值越大则越先接收
     */
    val eventDispatchOrder: Int

    /**
     * 当收到服务端推送过来的消息时调用
     *
     * @param message
     */
    fun onMessageReceived(message: Message)

    /**
     * 当调用CIMPushManager.sendRequest()向服务端发送请求，获得相应时调用
     *
     * @param replybody
     */
    fun onReplyReceived(replybody: ReplyBody)

    /**
     * 当调用CIMPushManager.sendRequest()向服务端发送请求成功时
     *
     * @param body
     */
    fun onSentSuccessed(body: SentBody)

    /**
     * 当手机网络发生变化时调用
     *
     * @param networkinfo
     */
    fun onNetworkChanged(networkinfo: NetworkInfo)

    /**
     * 当连接服务器成功时回调
     *
     * @param hasAutoBind
     * : true 已经自动绑定账号到服务器了，不需要再手动调用bindAccount
     */
    fun onConnectionSuccessed(hasAutoBind: Boolean)

    /**
     * 当断开服务器连接的时候回调
     *
     */
    fun onConnectionClosed()

    /**
     * 当连接服务器失败的时候回调
     *
     */
    fun onConnectionFailed()
}
