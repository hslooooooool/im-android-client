package vip.qsos.im.lib

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri

/**
 * @author : 华清松
 * 消息缓存帮助类
 */
object IMCacheHelper {
    /**消息账号*/
    const val KEY_ACCOUNT = "KEY_ACCOUNT"
    /**客户端设备ID*/
    const val KEY_DEVICE_ID = "KEY_DEVICE_ID"
    /**是否手动断开服务*/
    const val KEY_MANUAL_STOP = "KEY_MANUAL_STOP"
    /**消息服务是否已销毁*/
    const val KEY_IM_DESTROYED = "KEY_IM_DESTROYED"
    /**服务器域名*/
    const val KEY_IM_SERVER_HOST = "KEY_IM_SERVER_HOST"
    /**服务器端口*/
    const val KEY_IM_SERVER_PORT = "KEY_IM_SERVER_PORT"
    /**服务器连接状态*/
    const val KEY_CIM_CONNECTION_STATE = "KEY_CIM_CONNECTION_STATE"
    /**共享文件地址*/
    private const val CONTENT_URI = "content://%s.provider"

    fun remove(context: Context, key: String) {
        val resolver = context.contentResolver
        resolver.delete(Uri.parse(String.format(CONTENT_URI, context.packageName)), key, null)
    }

    fun putString(context: Context, key: String, value: String?) {
        val resolver = context.contentResolver
        val values = ContentValues()
        values.put("value", value)
        values.put("key", key)
        resolver.insert(Uri.parse(String.format(CONTENT_URI, context.packageName)), values)
    }

    fun getString(context: Context, key: String): String? {
        var value: String? = null
        val resolver = context.contentResolver
        val cursor = resolver.query(
            Uri.parse(String.format(CONTENT_URI, context.packageName)),
            arrayOf(key),
            null,
            null,
            null
        )
        if (cursor != null && cursor.moveToFirst()) {
            value = cursor.getString(0)
        }
        closeQuietly(cursor)
        return value
    }

    fun putBoolean(context: Context, key: String, value: Boolean) {
        putString(context, key, java.lang.Boolean.toString(value))
    }

    fun getBoolean(context: Context, key: String): Boolean {
        val value = getString(context, key)
        return value != null && java.lang.Boolean.parseBoolean(value)
    }

    fun putInt(context: Context, key: String, value: Int) {
        putString(context, key, value.toString())
    }

    fun getInt(context: Context, key: String): Int {
        val value = getString(context, key)
        return if (value == null) 0 else Integer.parseInt(value)
    }

    private fun closeQuietly(cursor: Cursor?) {
        try {
            cursor?.close()
        } catch (ignore: Exception) {
        }
    }

}
