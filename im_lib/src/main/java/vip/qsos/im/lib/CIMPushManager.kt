package vip.qsos.im.lib

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.text.TextUtils
import vip.qsos.im.lib.coder.ImLogUtils
import vip.qsos.im.lib.constant.CIMConstant
import vip.qsos.im.lib.model.SentBody
import java.util.*

/**
 * @author : 华清松
 * IM消息推送服务管理类
 */
object CIMPushManager {
    const val ACTION_ACTIVATE_PUSH_SERVICE = "ACTION_ACTIVATE_PUSH_SERVICE"
    const val ACTION_CREATE_CIM_CONNECTION = "ACTION_CREATE_CIM_CONNECTION"
    const val ACTION_SEND_REQUEST_BODY = "ACTION_SEND_REQUEST_BODY"
    const val ACTION_CLOSE_CIM_CONNECTION = "ACTION_CLOSE_CIM_CONNECTION"
    const val ACTION_SET_LOGGER_EANABLE = "ACTION_SET_LOGGER_EANABLE"
    const val KEY_SEND_BODY = "KEY_SEND_BODY"
    const val KEY_CIM_CONNECTION_STATUS = "KEY_CIM_CONNECTION_STATUS"

    /**
     * 初始化,连接服务端，在程序启动页或者 在Application里调用
     *
     * @param context
     * @param host
     * @param port
     */
    fun connect(context: Context, host: String, port: Int) {
        if (TextUtils.isEmpty(host) || port == 0) {
            ImLogUtils.logger.invalidHostPort(host, port)
            return
        }
        CIMCacheManager.putString(context, CIMCacheManager.KEY_CIM_SERVIER_HOST, host)
        CIMCacheManager.putInt(context, CIMCacheManager.KEY_CIM_SERVIER_PORT, port)
        CIMCacheManager.putBoolean(context, CIMCacheManager.KEY_CIM_DESTROYED, false)
        CIMCacheManager.putBoolean(context, CIMCacheManager.KEY_MANUAL_STOP, false)
        CIMCacheManager.remove(context, CIMCacheManager.KEY_ACCOUNT)

        val serviceIntent = Intent(context, CIMPushService::class.java)
        serviceIntent.action = ACTION_CREATE_CIM_CONNECTION
        startService(context, serviceIntent)

    }

    fun setLoggerEnable(context: Context, enable: Boolean) {
        val serviceIntent = Intent(context, CIMPushService::class.java)
        serviceIntent.putExtra(CIMPushService.KEY_LOGGER_ENABLE, enable)
        serviceIntent.action = ACTION_SET_LOGGER_EANABLE
        startService(context, serviceIntent)
    }


    /**
     * 设置一个账号登录到服务端
     *
     * @param account
     * 用户唯一ID
     */
    fun bindAccount(context: Context, account: String?) {

        if (isDestoryed(context) || account == null || account.trim { it <= ' ' }.length == 0) {
            return
        }

        sendBindRequest(context, account)

    }

    private fun sendBindRequest(context: Context, account: String) {

        CIMCacheManager.putBoolean(context, CIMCacheManager.KEY_MANUAL_STOP, false)
        CIMCacheManager.putString(context, CIMCacheManager.KEY_ACCOUNT, account)

        var deviceId = CIMCacheManager.getString(context, CIMCacheManager.KEY_DEVICE_ID)
        if (TextUtils.isEmpty(deviceId)) {
            deviceId = UUID.randomUUID().toString().replace("-".toRegex(), "").toUpperCase()
            CIMCacheManager.putString(context, CIMCacheManager.KEY_DEVICE_ID, deviceId)
        }

        val sent = SentBody()
        sent.key = CIMConstant.RequestKey.CLIENT_BIND
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
        val account = CIMCacheManager.getString(context, CIMCacheManager.KEY_ACCOUNT)
        if (account == null || account.trim { it <= ' ' }.isEmpty() || isDestoryed(context)) {
            return false
        }
        sendBindRequest(context, account)
        return true
    }

    /**
     * 发送一个CIM请求
     *
     * @param context
     * @body
     */
    fun sendRequest(context: Context, body: SentBody) {

        if (isDestoryed(context) || isStoped(context)) {
            return
        }

        val serviceIntent = Intent(context, CIMPushService::class.java)
        serviceIntent.putExtra(KEY_SEND_BODY, body)
        serviceIntent.action = ACTION_SEND_REQUEST_BODY
        startService(context, serviceIntent)

    }

    /**
     * 停止接受推送，将会退出当前账号登录，端口与服务端的连接
     *
     * @param context
     */
    fun stop(context: Context) {

        if (isDestoryed(context)) {
            return
        }

        CIMCacheManager.putBoolean(context, CIMCacheManager.KEY_MANUAL_STOP, true)

        val serviceIntent = Intent(context, CIMPushService::class.java)
        serviceIntent.action = ACTION_CLOSE_CIM_CONNECTION
        startService(context, serviceIntent)

    }

    /**
     * 完全销毁CIM，一般用于完全退出程序，调用resume将不能恢复
     *
     * @param context
     */
    fun destroy(context: Context) {

        CIMCacheManager.putBoolean(context, CIMCacheManager.KEY_CIM_DESTROYED, true)
        CIMCacheManager.putString(context, CIMCacheManager.KEY_ACCOUNT, null)

        context.stopService(Intent(context, CIMPushService::class.java))

    }

    /**
     * 重新恢复接收推送，重新连接服务端，并登录当前账号
     *
     * @param context
     */
    fun resume(context: Context) {

        if (isDestoryed(context)) {
            return
        }

        autoBindAccount(context)
    }

    fun isDestoryed(context: Context): Boolean {
        return CIMCacheManager.getBoolean(context, CIMCacheManager.KEY_CIM_DESTROYED)
    }

    fun isStoped(context: Context): Boolean {
        return CIMCacheManager.getBoolean(context, CIMCacheManager.KEY_MANUAL_STOP)
    }

    fun isConnected(context: Context): Boolean {
        return CIMCacheManager.getBoolean(context, CIMCacheManager.KEY_CIM_CONNECTION_STATE)
    }

    fun isNetworkConnected(context: Context): Boolean {
        val networkInfo = getNetworkInfo(context)
        return networkInfo != null && networkInfo.isConnected
    }

    fun getNetworkInfo(context: Context): NetworkInfo? {
        return (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo
    }


    fun startService(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }


    private fun getVersionName(context: Context): String? {
        var versionName: String? = null
        try {
            val mPackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            versionName = mPackageInfo.versionName
        } catch (ignore: NameNotFoundException) {
        }

        return versionName
    }


}
