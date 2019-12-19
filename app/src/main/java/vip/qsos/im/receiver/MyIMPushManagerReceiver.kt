package vip.qsos.im.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import vip.qsos.im.demo.R
import vip.qsos.im.lib.AbsIMEventBroadcastReceiver
import vip.qsos.im.lib.IMListenerManager
import vip.qsos.im.lib.model.Message
import vip.qsos.im.lib.model.ReplyBody
import vip.qsos.im.ui.MessageActivity

/**
 * @author : 华清松
 * 消息接收广播服务
 */
class MyIMPushManagerReceiver : AbsIMEventBroadcastReceiver() {

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
            Intent(context, MessageActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(context, channelId!!)
        builder.setAutoCancel(true)
        builder.setDefaults(Notification.DEFAULT_ALL)
        builder.setWhen(msg.timestamp)
        builder.setTicker(title)
        builder.setContentTitle(title)
        builder.setContentText(msg.content)
        builder.setDefaults(Notification.DEFAULT_LIGHTS)
        builder.setContentIntent(contentIntent)
        val notification = builder.build()
        notificationManager.notify(R.drawable.ic_launcher, notification)
    }

    override fun onConnectionSuccess(hasAutoBind: Boolean) {
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
