/**
 * Copyright 2013-2019 Xia Jun(3979434@qq.com).
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * **************************************************************************************
 * *
 * Website : http://www.farsunset.com                           *
 * *
 * **************************************************************************************
 */
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
import vip.qsos.im.lib.CIMListenerManager
import vip.qsos.im.lib.model.Message
import vip.qsos.im.lib.model.ReplyBody
import vip.qsos.im.ui.SystemMessageActivity

/**
 * 消息入口，所有消息都会经过这里
 *
 * @author 3979434
 */
class CIMPushManagerReceiver : CIMEventBroadcastReceiver() {

    //当收到消息时，会执行onMessageReceived，这里是消息第一入口

    override fun onMessageReceived(message: Message, intent: Intent) {

        //调用分发消息监听
        CIMListenerManager.notifyOnMessageReceived(message)

        //以开头的为动作消息，无须显示,如被强行下线消息Constant.ACTION_999
        if (message.action.startsWith("9")) {
            return
        }

        showNotify(context, message)
    }


    private fun showNotify(context: Context, msg: Message) {

        val notificationManager =
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
        CIMListenerManager.notifyOnNetworkChanged(info)
    }


    override fun onConnectionSuccessed(hasAutoBind: Boolean) {
        CIMListenerManager.notifyOnConnectionSuccessed(hasAutoBind)
    }

    override fun onConnectionClosed() {
        CIMListenerManager.notifyOnConnectionClosed()
    }


    override fun onReplyReceived(body: ReplyBody) {
        CIMListenerManager.notifyOnReplyReceived(body)
    }


    override fun onConnectionFailed() {
        // TODO Auto-generated method stub
        CIMListenerManager.notifyOnConnectionFailed()
    }

}
