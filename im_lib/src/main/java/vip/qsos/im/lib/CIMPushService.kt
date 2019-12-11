package vip.qsos.im.lib

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import vip.qsos.im.lib.coder.ImLogUtils
import vip.qsos.im.lib.constant.CIMConstant
import vip.qsos.im.lib.model.SentBody

/**
 * @author : 华清松
 * 消息服务连接服务
 */
class CIMPushService : Service() {
    private var manager: CIMConnectorManager? = null
    private var keepAliveReceiver: KeepAliveBroadcastReceiver? = null
    private var connectivityManager: ConnectivityManager? = null

    private var networkCallback: ConnectivityManager.NetworkCallback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val intent = Intent()
                intent.setPackage(packageName)
                intent.action = CIMConstant.IntentAction.ACTION_NETWORK_CHANGED
                sendBroadcast(intent)
            }

            override fun onUnavailable() {
                val intent = Intent()
                intent.setPackage(packageName)
                intent.action = CIMConstant.IntentAction.ACTION_NETWORK_CHANGED
                sendBroadcast(intent)
            }
        }

    var connectHandler: Handler = Handler {
        connect()
        true
    }

    var notificationHandler: Handler = Handler {
        stopForeground(true)
        true
    }

    override fun onCreate() {
        manager = CIMConnectorManager.getManager(this.applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            keepAliveReceiver = KeepAliveBroadcastReceiver()
            registerReceiver(keepAliveReceiver, keepAliveReceiver!!.intentFilter)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            connectivityManager =
                getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            connectivityManager!!.registerDefaultNetworkCallback(networkCallback)

        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        var mIntent = intent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                javaClass.simpleName,
                javaClass.simpleName,
                NotificationManager.IMPORTANCE_LOW
            )
            channel.enableLights(false)
            channel.enableVibration(false)
            channel.setSound(null, null)
            notificationManager.createNotificationChannel(channel)
            val notification = Notification.Builder(this, channel.id)
                .setContentTitle("Push service")
                .setContentText("Push service is running")
                .build()
            startForeground(NOTIFICATION_ID, notification)
        }

        mIntent = mIntent ?: Intent(CIMPushManager.ACTION_ACTIVATE_PUSH_SERVICE)

        val action = mIntent.action

        if (CIMPushManager.ACTION_CREATE_CIM_CONNECTION == action) {
            connect(mIntent.getLongExtra(KEY_DELAYED_TIME, 0))
        }

        if (CIMPushManager.ACTION_SEND_REQUEST_BODY == action) {
            manager!!.send(mIntent.getSerializableExtra(CIMPushManager.KEY_SEND_BODY) as SentBody)
        }

        if (CIMPushManager.ACTION_CLOSE_CIM_CONNECTION == action) {
            manager!!.closeSession()
        }

        if (CIMPushManager.ACTION_ACTIVATE_PUSH_SERVICE == action) {
            handleKeepAlive()
        }

        if (CIMPushManager.ACTION_SET_LOGGER_EANABLE == action) {
            val enable = mIntent.getBooleanExtra(KEY_LOGGER_ENABLE, true)
            ImLogUtils.logger.open(enable)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationHandler.sendEmptyMessageDelayed(0, 1000)
        }

        return super.onStartCommand(mIntent, flags, startId)
    }

    private fun connect(delayMillis: Long) {

        if (delayMillis <= 0) {
            connect()
            return
        }

        connectHandler.sendEmptyMessageDelayed(0, delayMillis)

    }

    private fun connect() {

        if (CIMPushManager.isDestoryed(this) || CIMPushManager.isStoped(this)) {
            return
        }

        val host = CIMCacheManager.getString(this, CIMCacheManager.KEY_CIM_SERVIER_HOST)
        val port = CIMCacheManager.getInt(this, CIMCacheManager.KEY_CIM_SERVIER_PORT)

        if (host == null || host.trim { it <= ' ' }.length == 0 || port <= 0) {
            Log.e(this.javaClass.simpleName, "Invalid hostname or port. host:$host port:$port")
            return
        }

        manager!!.connect(host, port)

    }

    private fun handleKeepAlive() {

        if (manager!!.isConnected) {
            ImLogUtils.logger.connectState(true)
            return
        }

        connect()

    }

    override fun onBind(arg0: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        manager!!.destroy()
        connectHandler.removeMessages(0)
        notificationHandler.removeMessages(0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            unregisterReceiver(keepAliveReceiver)

        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager!!.unregisterNetworkCallback(networkCallback)
        }
    }

    inner class KeepAliveBroadcastReceiver : BroadcastReceiver() {

        val intentFilter: IntentFilter
            get() {
                val intentFilter = IntentFilter()
                intentFilter.addAction(Intent.ACTION_POWER_CONNECTED)
                intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED)
                intentFilter.addAction(Intent.ACTION_USER_PRESENT)
                return intentFilter
            }

        override fun onReceive(arg0: Context, arg1: Intent) {
            handleKeepAlive()
        }

    }

    companion object {
        val KEY_DELAYED_TIME = "KEY_DELAYED_TIME"
        val KEY_LOGGER_ENABLE = "KEY_LOGGER_ENABLE"

        private val NOTIFICATION_ID = Integer.MAX_VALUE
    }

}
