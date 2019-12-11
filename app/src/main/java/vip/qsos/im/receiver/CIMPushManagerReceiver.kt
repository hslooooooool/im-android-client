package vip.qsos.im.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo

import androidx.core.app.NotificationCompat
import com.farsunset.ichat.example.R

import vip.qsos.im.lib.CIMEventBroadcastReceiver
import vip.qsos.im.lib.IMListenerManager
import vip.qsos.im.lib.model.Message
import vip.qsos.im.lib.model.ReplyBody
import vip.qsos.im.ui.SystemMessageActivity

/**
 * @author : 华清松
 * 消息接收广播服务
 */
class CIMPushManagerReceiver : CIMEventBroadcastReceiver() {

    //当收到消息时，会执行onMessageReceived，这里是消息第一入口
    override fun onMessageReceived(message: Message, intent: Intent) {
        //调用分发消息监听
        IMListenerManager.notifyOnMessageReceived(message)
        //以9开头的消息无须广播,如被强行下线消息Constant.ACTION_999
        if (message.action?.startsWith("9") == true) {
            return
        }
        showNotify(context, message)
    }

    /**消息广播*/
    private fun showNotify(context: Context, msg: Message) {
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        var channelId: String? = null
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channelId = "system"
            val channel =
                NotificationChannel(channelId, "message", NotificationManager.IMPORTANCE_DEFAULT)
            channel.enableLights(true) //是否在桌面icon右上角展示小红点   
            notificationManager.createNotificationChannel(channel)
        }
        val title = "系统消息"
        val contentIntent = PendingIntent.getActivity(
            context,
            1,
            Intent(context, SystemMessageActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(context, channelId!!)
        builder.setAutoCancel(true)
        builder.setDefaults(Notification.DEFAULT_ALL)
        builder.setWhen(msg.timestamp)
        builder.setSmallIcon(R.drawable.icon)
        builder.setTicker(title)
        builder.setContentTitle(title)
        builder.setContentText(msg.content)
        builder.setDefaults(Notification.DEFAULT_LIGHTS)
        builder.setContentIntent(contentIntent)
        val notification = builder.build()
        notificationManager.notify(R.drawable.icon, notification)
    }

    fun onNetworkChanged(info: NetworkInfo) {
        IMListenerManager.notifyOnNetworkChanged(info)
    }

    override fun onConnectionSuccessed(hasAutoBind: Boolean) {
        IMListenerManager.notifyOnConnectionSuccess(hasAutoBind)
    }

    override fun onConnectionClosed() {
        IMListenerManager.notifyOnConnectionClosed()
    }

    override fun onReplyReceived(body: ReplyBody) {
        IMListenerManager.notifyOnReplyReceived(body)
    }

    override fun onConnectionFailed() {
        IMListenerManager.notifyOnConnectionFailed()
    }

}
