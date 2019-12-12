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
import android.text.TextUtils
import vip.qsos.im.lib.coder.LogUtils
import vip.qsos.im.lib.constant.IMConstant
import vip.qsos.im.lib.model.SendBody

/**
 * @author : 华清松
 * 消息服务
 */
class IMService : Service() {

    companion object {
        private const val NOTIFICATION_ID = Integer.MAX_VALUE
        const val KEY_DELAYED_TIME = "KEY_DELAYED_TIME"
        const val KEY_LOGGER_ENABLE = "KEY_LOGGER_ENABLE"
    }

    private lateinit var mConnectManager: IMConnectManager
    private var mKeepAliveReceiver: KeepAliveBroadcastReceiver? = null
    private var mConnectivityManager: ConnectivityManager? = null

    /**网络情况监听*/
    private var networkCallback: ConnectivityManager.NetworkCallback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val intent = Intent()
                intent.setPackage(packageName)
                intent.action = IMConstant.IntentAction.ACTION_NETWORK_CHANGED
                sendBroadcast(intent)
            }

            override fun onUnavailable() {
                val intent = Intent()
                intent.setPackage(packageName)
                intent.action = IMConstant.IntentAction.ACTION_NETWORK_CHANGED
                sendBroadcast(intent)
            }
        }

    /**服务连接*/
    private var mConnectHandler: Handler = Handler {
        this.connect()
        true
    }

    /**消息通知*/
    private var mNotificationHandler: Handler = Handler {
        stopForeground(true)
        true
    }

    override fun onCreate() {
        mConnectManager = IMConnectManager.getManager(this.applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mKeepAliveReceiver = KeepAliveBroadcastReceiver()
            registerReceiver(mKeepAliveReceiver, mKeepAliveReceiver!!.intentFilter)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            /**安卓版本大于N时，可以注册网络监控，在网络变化时采取一些策略*/
            mConnectivityManager =
                getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            mConnectivityManager!!.registerDefaultNetworkCallback(networkCallback)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            /**开启前台服务*/
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
                .setContentTitle("IM消息服务")
                .setContentText("IM消息服务正在运行，请保持")
                .build()
            startForeground(NOTIFICATION_ID, notification)
        }
        val mIntent: Intent = intent ?: Intent(CIMPushManager.ACTION_ACTIVATE_PUSH_SERVICE)
        when (mIntent.action) {
            CIMPushManager.ACTION_CREATE_CONNECTION -> {
                this.delayConnect(mIntent.getLongExtra(KEY_DELAYED_TIME, 0))
            }
            CIMPushManager.ACTION_SEND_REQUEST_BODY -> {
                mConnectManager.send(mIntent.getSerializableExtra(CIMPushManager.KEY_SEND_BODY) as SendBody)
            }
            CIMPushManager.ACTION_CLOSE_CONNECTION -> {
                mConnectManager.closeConnect()
            }
            CIMPushManager.ACTION_ACTIVATE_PUSH_SERVICE -> {
                this.handleKeepAlive()
            }
            CIMPushManager.ACTION_SET_LOGGER_ENABLE -> {
                val enable = mIntent.getBooleanExtra(KEY_LOGGER_ENABLE, true)
                LogUtils.logger.open(enable)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            /**延迟【1】秒后关闭通知栏通知*/
            mNotificationHandler.sendEmptyMessageDelayed(0, 1000)
        }
        return super.onStartCommand(mIntent, flags, startId)
    }

    /**延迟连接服务器
     * @param delayMillis 延迟毫秒数
     * */
    private fun delayConnect(delayMillis: Long) {
        if (delayMillis <= 0) {
            this.connect()
        } else {
            mConnectHandler.sendEmptyMessageDelayed(0, delayMillis)
        }
    }

    /**连接消息服务器*/
    private fun connect() {
        if (CIMPushManager.isDestroyed(this) || CIMPushManager.isStop(this)) {
            LogUtils.logger.connectState(false)
            return
        }
        val host = IMCacheHelper.getString(this, IMCacheHelper.KEY_IM_SERVER_HOST)
        val port = IMCacheHelper.getInt(this, IMCacheHelper.KEY_IM_SERVER_PORT)
        if (TextUtils.isEmpty(host) || port <= 0) {
            LogUtils.logger.invalidHostPort(host, port)
        } else {
            mConnectManager.connect(host!!, port)
        }
    }

    /**捕获到保活请求，判断一次服务连接情况，未连接时发起连接*/
    private fun handleKeepAlive() {
        if (!mConnectManager.isConnected) {
            this.connect()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        mConnectManager.destroy()
        mConnectHandler.removeMessages(0)
        mNotificationHandler.removeMessages(0)
        mKeepAliveReceiver?.let { unregisterReceiver(mKeepAliveReceiver) }
        mConnectivityManager?.unregisterNetworkCallback(networkCallback)
    }

    /**创建一个观察手机自身状态的广播接收器，以此保活消息服务*/
    inner class KeepAliveBroadcastReceiver : BroadcastReceiver() {
        val intentFilter: IntentFilter
            get() {
                val intentFilter = IntentFilter()
                intentFilter.addAction(Intent.ACTION_POWER_CONNECTED)
                intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED)
                intentFilter.addAction(Intent.ACTION_USER_PRESENT)
                return intentFilter
            }

        override fun onReceive(context: Context, intent: Intent) {
            handleKeepAlive()
        }
    }
}
