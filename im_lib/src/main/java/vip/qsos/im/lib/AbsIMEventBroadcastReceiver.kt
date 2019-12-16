package vip.qsos.im.lib

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build

import vip.qsos.im.lib.constant.IMConstant
import vip.qsos.im.lib.model.Message
import vip.qsos.im.lib.model.ReplyBody
import vip.qsos.im.lib.model.SendBody

/**
 * @author : 华清松
 * 消息接收基础广播
 */
abstract class AbsIMEventBroadcastReceiver : BroadcastReceiver() {
    lateinit var context: Context

    override fun onReceive(context: Context, intent: Intent) {
        this.context = context
        when (intent.action) {
            Intent.ACTION_USER_PRESENT, Intent.ACTION_POWER_CONNECTED, Intent.ACTION_POWER_DISCONNECTED -> {
                /**操作事件广播，用于提高 PushService 存活率*/
                startPushService()
            }
            IMConstant.IntentAction.ACTION_NETWORK_CHANGED, ConnectivityManager.CONNECTIVITY_ACTION -> {
                /**设备网络状态变化事件*/
                onDevicesNetworkChanged()
            }
            IMConstant.IntentAction.ACTION_CONNECTION_CLOSED -> {
                /**消息服务器断开事件*/
                onInnerConnectionClosed()
            }
            IMConstant.IntentAction.ACTION_CONNECTION_FAILED -> {
                /**连接服务器失败事件*/
                onConnectionFailed(IMConstant.RECONNECT_INTERVAL_TIME)
            }
            IMConstant.IntentAction.ACTION_CONNECTION_SUCCESS -> {
                /**连接服务器成功事件*/
                onInnerConnectionSuccess()
            }
            IMConstant.IntentAction.ACTION_MESSAGE_RECEIVED -> {
                /**收到服务器自定义消息事件*/
                onInnerMessageReceived(
                    intent.getSerializableExtra(Message::class.java.name) as Message,
                    intent
                )
            }
            IMConstant.IntentAction.ACTION_REPLY_RECEIVED -> {
                /**收到服务器回执事件*/
                onReplyReceived(intent.getSerializableExtra(ReplyBody::class.java.name) as ReplyBody)
            }
            IMConstant.IntentAction.ACTION_SENT_SUCCESS -> {
                /**发送成功事件*/
                onSentSucceed(intent.getSerializableExtra(SendBody::class.java.name) as SendBody)
            }
            IMConstant.IntentAction.ACTION_CONNECTION_RECOVERY -> {
                /*重新连接，如果断开的话*/
                connect(0)
            }
        }
    }

    private fun startPushService() {
        val intent = Intent(context, IMService::class.java)
        intent.action = IMManagerHelper.ACTION_ACTIVATE_PUSH_SERVICE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun onInnerConnectionClosed() {
        IMCacheHelper.putBoolean(context, IMCacheHelper.KEY_IM_CONNECTION_STATE, false)
        if (IMManagerHelper.isNetworkConnected(context)) {
            connect(0)
        }
        onConnectionClosed()
    }

    private fun onConnectionFailed(reInterval: Long) {
        if (IMManagerHelper.isNetworkConnected(context)) {
            onConnectionFailed()
            connect(reInterval)
        }
    }

    private fun onInnerConnectionSuccess() {
        IMCacheHelper.putBoolean(context, IMCacheHelper.KEY_IM_CONNECTION_STATE, true)
        val autoBind = IMManagerHelper.autoBindAccount(context)
        onConnectionSuccess(autoBind)
    }

    private fun onDevicesNetworkChanged() {
        if (IMManagerHelper.isNetworkConnected(context)) {
            connect(0)
        }
        onNetworkChanged()
    }

    private fun connect(delay: Long) {
        val serviceIntent = Intent(context, IMService::class.java)
        serviceIntent.putExtra(IMService.KEY_DELAYED_TIME, delay)
        serviceIntent.action = IMManagerHelper.ACTION_CREATE_CONNECTION
        IMManagerHelper.startService(context, serviceIntent)
    }

    private fun onInnerMessageReceived(message: Message, intent: Intent) {
        if (isForceOfflineMessage(message.action)) {
            IMManagerHelper.stop(context)
        }
        onMessageReceived(message, intent)
    }

    private fun isForceOfflineMessage(action: String?): Boolean {
        return IMConstant.MessageAction.ACTION_999 == action
    }

    /**收到消息*/
    abstract fun onMessageReceived(message: Message, intent: Intent)

    open fun onNetworkChanged() {
        IMListenerManager.notifyOnNetworkChanged(IMManagerHelper.getNetworkInfo(context)!!)
    }

    open fun onConnectionSuccess(hasAutoBind: Boolean) {
        IMListenerManager.notifyOnConnectionSuccess(hasAutoBind)
    }

    open fun onConnectionClosed() {
        IMListenerManager.notifyOnConnectionClosed()
    }

    open fun onConnectionFailed() {
        IMListenerManager.notifyOnConnectionFailed()
    }

    open fun onReplyReceived(body: ReplyBody) {
        IMListenerManager.notifyOnReplyReceived(body)
    }

    open fun onSentSucceed(body: SendBody) {
        IMListenerManager.notifyOnSentSucceed(body)
    }
}
