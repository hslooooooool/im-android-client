package vip.qsos.im.lib

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.text.TextUtils
import vip.qsos.im.lib.coder.IMLogUtils
import vip.qsos.im.lib.constant.IMConstant
import vip.qsos.im.lib.model.SendBody
import java.util.*

/**
 * @author : 华清松
 * IM消息推送服务管理类
 */
object IMManagerHelper {
    /**【动作】开启消息服务日志*/
    const val ACTION_SET_LOGGER_ENABLE = "ACTION_SET_LOGGER_ENABLE"
    /**【动作】发送消息到服务器*/
    const val ACTION_SEND_REQUEST_BODY = "ACTION_SEND_REQUEST_BODY"
    /**【动作】关闭服务器连接*/
    const val ACTION_CLOSE_CONNECTION = "ACTION_CLOSE_CONNECTION"
    /**【动作】连接消息服务器*/
    const val ACTION_CREATE_CONNECTION = "ACTION_CREATE_CONNECTION"
    /**【动作】消息服务器活跃检测，死掉将重连*/
    const val ACTION_ACTIVATE_PUSH_SERVICE = "ACTION_ACTIVATE_PUSH_SERVICE"

    const val KEY_SEND_BODY = "KEY_SEND_BODY"
    const val KEY_CONNECTION_STATUS = "KEY_CONNECTION_STATUS"

    /**
     * 初始化,连接服务端，在程序启动页或在 Application 里调用
     *
     * @param context 上下文
     * @param host 域名
     * @param port 端口
     */
    fun connect(context: Context, host: String, port: Int) {
        if (TextUtils.isEmpty(host) || port == 0) {
            IMLogUtils.LOGGER.invalidHostPort(host, port)
            return
        }
        IMCacheHelper.putString(context, IMCacheHelper.KEY_IM_SERVER_HOST, host)
        IMCacheHelper.putInt(context, IMCacheHelper.KEY_IM_SERVER_PORT, port)
        IMCacheHelper.putBoolean(context, IMCacheHelper.KEY_IM_DESTROYED, false)
        IMCacheHelper.putBoolean(context, IMCacheHelper.KEY_MANUAL_STOP, false)
        IMCacheHelper.remove(context, IMCacheHelper.KEY_ACCOUNT)

        val serviceIntent = Intent(context.applicationContext, IMService::class.java)
        serviceIntent.action = ACTION_CREATE_CONNECTION
        startService(context, serviceIntent)
    }

    fun setLoggerEnable(context: Context, enable: Boolean) {
        val serviceIntent = Intent(context.applicationContext, IMService::class.java)
        serviceIntent.putExtra(IMService.KEY_LOGGER_ENABLE, enable)
        serviceIntent.action = ACTION_SET_LOGGER_ENABLE
        startService(context, serviceIntent)
    }

    /**
     * 设置一个账号登录到服务端
     *
     * @param account 用户唯一ID
     */
    fun bindAccount(context: Context, account: String?) {
        if (isDestroyed(context) || TextUtils.isEmpty(account)) {
            return
        }
        sendBindRequest(context, account!!)
    }

    private fun sendBindRequest(context: Context, account: String) {
        IMCacheHelper.putBoolean(context, IMCacheHelper.KEY_MANUAL_STOP, false)
        IMCacheHelper.putString(context, IMCacheHelper.KEY_ACCOUNT, account)
        var deviceId = IMCacheHelper.getString(context, IMCacheHelper.KEY_DEVICE_ID)
        if (TextUtils.isEmpty(deviceId)) {
            deviceId =
                UUID.randomUUID().toString().replace("-".toRegex(), "").toUpperCase(Locale.ENGLISH)
            IMCacheHelper.putString(context, IMCacheHelper.KEY_DEVICE_ID, deviceId)
        }
        val sent = SendBody()
        sent.key = IMConstant.RequestKey.CLIENT_BIND
        sent.put("account", account)
        sent.put("deviceId", deviceId)
        sent.put("channel", "android")
        sent.put("device", Build.MODEL)
        sent.put("version", getVersionName(context))
        sent.put("osVersion", Build.VERSION.RELEASE)
        sent.put("packageName", context.packageName)
        sendRequest(context, sent)
    }

    fun autoBindAccount(context: Context): Boolean {
        val account = IMCacheHelper.getString(context, IMCacheHelper.KEY_ACCOUNT)
        if (TextUtils.isEmpty(account) || isDestroyed(context)) {
            return false
        }
        sendBindRequest(context, account!!)
        return true
    }

    /**
     * 往服务器发送消息
     *
     * @param context 上下文
     * @param body 发送消息实体
     */
    fun sendRequest(context: Context, body: SendBody) {
        if (isDestroyed(context) || isStop(context)) {
            return
        }
        val serviceIntent = Intent(context.applicationContext, IMService::class.java)
        serviceIntent.putExtra(KEY_SEND_BODY, body)
        serviceIntent.action = ACTION_SEND_REQUEST_BODY
        startService(context, serviceIntent)
    }

    /**停止接受推送，将会退出当前账号登录，断开与服务端的连接，可重连，需重新登录与连接，
     * 调用 resume 将可恢复
     * @see resume
     * */
    fun stop(context: Context) {
        if (isDestroyed(context)) {
            return
        }
        IMCacheHelper.putBoolean(context, IMCacheHelper.KEY_MANUAL_STOP, true)
        val serviceIntent = Intent(context.applicationContext, IMService::class.java)
        serviceIntent.action = ACTION_CLOSE_CONNECTION
        startService(context, serviceIntent)
    }

    /**完全销毁消息服务，一般用于完全退出程序，调用 resume 将不能恢复*/
    fun destroy(context: Context) {
        IMCacheHelper.putBoolean(context, IMCacheHelper.KEY_IM_DESTROYED, true)
        IMCacheHelper.putString(context, IMCacheHelper.KEY_ACCOUNT, null)
        context.stopService(Intent(context.applicationContext, IMService::class.java))
    }

    /**重新恢复接收推送，重新连接服务端，登录缓存的账号*/
    fun resume(context: Context) {
        if (isDestroyed(context)) {
            return
        }
        autoBindAccount(context)
    }

    /**判断服务已销毁*/
    fun isDestroyed(context: Context): Boolean {
        return IMCacheHelper.getBoolean(context, IMCacheHelper.KEY_IM_DESTROYED)
    }

    /**判断消息服务已停止*/
    fun isStop(context: Context): Boolean {
        return IMCacheHelper.getBoolean(context, IMCacheHelper.KEY_MANUAL_STOP)
    }

    /**判断消息服务已连接*/
    fun isConnected(context: Context): Boolean {
        return IMCacheHelper.getBoolean(context, IMCacheHelper.KEY_CIM_CONNECTION_STATE)
    }

    /**判断网络已连接*/
    fun isNetworkConnected(context: Context): Boolean {
        val networkInfo = getNetworkInfo(context)
        return networkInfo != null && networkInfo.isConnected
    }

    /**获取网络信息*/
    fun getNetworkInfo(context: Context): NetworkInfo? {
        return (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo
    }

    /**启动服务*/
    fun startService(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    /**获取APP版本名称*/
    private fun getVersionName(context: Context): String? {
        val versionName: String?
        versionName = try {
            val mPackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            mPackageInfo.versionName
        } catch (ignore: NameNotFoundException) {
            "unknown"
        }
        return versionName
    }

}
