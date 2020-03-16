package vip.qsos.im.lib

import android.net.NetworkInfo
import android.util.Log
import vip.qsos.im.lib.coder.IMLogUtils
import vip.qsos.im.lib.model.Message
import vip.qsos.im.lib.model.ReplyBody
import vip.qsos.im.lib.model.SendBody
import java.util.*

/**
 * @author : 华清松
 * 消息监听器管理
 */
object IMListenerManager {

    private val mListenerList = ArrayList<IMEventListener>()
    private val mReceiveComparator = IMMessageReceiveComparator()

    /**注册消息广播监听*/
    fun registerMessageListener(listener: IMEventListener) {
        if (!mListenerList.contains(listener)) {
            mListenerList.add(listener)
            Collections.sort(mListenerList, mReceiveComparator)
        }
    }

    /**注销消息广播监听*/
    fun removeMessageListener(listener: IMEventListener) {
        mListenerList.forEachIndexed { index, _ ->
            if (listener.javaClass == mListenerList[index].javaClass) {
                mListenerList.removeAt(index)
            }
        }
    }

    /**注册消息广播监听*/
    fun notifyOnNetworkChanged(info: NetworkInfo) {
        IMLogUtils.LOGGER.networkState(info.isConnected)
        for (listener in mListenerList) {
            listener.onNetworkChanged(info)
        }
    }

    /**服务已连接监听*/
    fun notifyOnConnectionSuccess(hasAutoBind: Boolean) {
        for (listener in mListenerList) {
            listener.onConnectionSuccess(hasAutoBind)
        }
    }

    /**收到消息监听*/
    fun notifyOnMessageReceived(message: Message) {
        for (listener in mListenerList) {
            listener.onMessageReceived(message)
        }
    }

    /**服务已关闭监听*/
    fun notifyOnConnectionClosed() {
        for (listener in mListenerList) {
            listener.onConnectionClosed()
        }
    }

    /**服务连接失败监听*/
    fun notifyOnConnectionFailed() {
        for (listener in mListenerList) {
            listener.onConnectionFailed()
        }
    }

    /**收到服务器回执监听*/
    fun notifyOnReplyReceived(body: ReplyBody) {
        for (listener in mListenerList) {
            listener.onReplyReceived(body)
        }
    }

    /**消息发送成功监听*/
    fun notifyOnSentSucceed(body: SendBody) {
        for (listener in mListenerList) {
            listener.onSentSuccess(body)
        }
    }

    /**销毁监听器*/
    fun destroy() {
        mListenerList.clear()
    }

    /**打印监听器列表*/
    fun logListenerListName() {
        for (l in mListenerList) {
            Log.i(IMEventListener::class.java.simpleName, "#${l.javaClass.name}#")
        }
    }

    /**消息接收优先级排序策略*/
    private class IMMessageReceiveComparator : Comparator<IMEventListener> {

        override fun compare(arg1: IMEventListener, arg2: IMEventListener): Int {
            val order1 = arg1.eventDispatchOrder
            val order2 = arg2.eventDispatchOrder
            return order2 - order1
        }
    }

}
