package vip.qsos.im.lib

import android.net.NetworkInfo
import android.util.Log
import vip.qsos.im.lib.model.Message
import vip.qsos.im.lib.model.ReplyBody
import vip.qsos.im.lib.model.SentBody
import java.util.*

/**
 * @author : 华清松
 * 消息监听器管理
 */
object CIMListenerManager {

    private val cimListeners = ArrayList<CIMEventListener>()
    private val comparator = CIMMessageReceiveComparator()

    fun registerMessageListener(listener: CIMEventListener) {
        if (!cimListeners.contains(listener)) {
            cimListeners.add(listener)
            Collections.sort(cimListeners, comparator)
        }
    }

    fun removeMessageListener(listener: CIMEventListener) {
        for (i in cimListeners.indices) {
            if (listener.javaClass == cimListeners[i].javaClass) {
                cimListeners.removeAt(i)
            }
        }
    }

    fun notifyOnNetworkChanged(info: NetworkInfo) {
        for (listener in cimListeners) {
            listener.onNetworkChanged(info)
        }
    }

    fun notifyOnConnectionSuccessed(hasAutoBind: Boolean) {
        for (listener in cimListeners) {
            listener.onConnectionSuccessed(hasAutoBind)
        }
    }

    fun notifyOnMessageReceived(message: Message) {
        for (listener in cimListeners) {
            listener.onMessageReceived(message)
        }
    }

    fun notifyOnConnectionClosed() {
        for (listener in cimListeners) {
            listener.onConnectionClosed()
        }
    }

    fun notifyOnConnectionFailed() {
        for (listener in cimListeners) {
            listener.onConnectionFailed()
        }
    }

    fun notifyOnReplyReceived(body: ReplyBody) {
        for (listener in cimListeners) {
            listener.onReplyReceived(body)
        }
    }

    fun notifyOnSentSucceed(body: SentBody) {
        for (listener in cimListeners) {
            listener.onSentSuccessed(body)
        }
    }

    fun destory() {
        cimListeners.clear()
    }

    fun logListenersName() {
        for (listener in cimListeners) {
            Log.i(
                CIMEventListener::class.java.simpleName,
                "#######" + listener.javaClass.name + "#######"
            )
        }
    }

    /**消息接收activity的接收顺序排序，CIM_RECEIVE_ORDER倒序*/
    private class CIMMessageReceiveComparator : Comparator<CIMEventListener> {

        override fun compare(arg1: CIMEventListener, arg2: CIMEventListener): Int {
            val order1 = arg1.eventDispatchOrder
            val order2 = arg2.eventDispatchOrder
            return order2 - order1
        }
    }

}
