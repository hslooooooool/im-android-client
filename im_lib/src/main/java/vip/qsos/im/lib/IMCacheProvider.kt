package vip.qsos.im.lib

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

/**
 * @author : 华清松
 * 消息服务缓存
 */
class IMCacheProvider : ContentProvider() {

    companion object {
        const val MODEL_KEY = "PRIVATE_IM_CACHE"
    }

    override fun delete(arg0: Uri, key: String?, arg2: Array<String>?): Int {
        context?.getSharedPreferences(MODEL_KEY, Context.MODE_PRIVATE)?.edit()?.remove(key)?.apply()
        return 0
    }

    override fun getType(arg0: Uri): String? {
        return null
    }

    override fun insert(arg0: Uri, values: ContentValues?): Uri? {
        val key = values?.getAsString("key")
        val value = values?.getAsString("value")
        context?.getSharedPreferences(MODEL_KEY, Context.MODE_PRIVATE)?.edit()
            ?.putString(key, value)?.apply()
        return null
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun query(
        p0: Uri,
        p1: Array<out String>?,
        p2: String?,
        p3: Array<out String>?,
        p4: String?
    ): Cursor? {
        val cursor = MatrixCursor(arrayOf("value"))
        var value: String? = null
        p1?.let {
            value = context?.getSharedPreferences(MODEL_KEY, Context.MODE_PRIVATE)
                ?.getString(p1[0], null)
        }
        cursor.addRow(arrayOf<Any?>(value))
        return cursor
    }

    override fun update(arg0: Uri, arg1: ContentValues?, arg2: String?, arg3: Array<String>?): Int {
        return 0
    }
}
