package vip.qsos.im.lib.model

import java.io.Serializable
import java.util.*

/**
 * @author : 华清松
 * 服务器应答对象
 */
class ReplyBody : Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }

    /**请求key*/
    var key: String? = null
    /**返回码*/
    var code: String? = null
    /**返回说明*/
    var message: String? = null
    /**返回时间*/
    var timestamp: Long = 0L
    /**返回数据集合*/
    val data: Hashtable<String, String> = Hashtable()

    private val keySet: Set<String>
        get() = data.keys

    operator fun get(k: String): String? {
        return data[k]
    }

    fun put(k: String?, v: String?) {
        if (k == null || v == null) {
            return
        }
        data[k] = v
    }

    fun remove(k: String) {
        data.remove(k)
    }

    fun putAll(map: Map<String, String>) {
        data.putAll(map)
    }

    override fun toString(): String {
        val buffer = StringBuffer()
        buffer.append("\n#ReplyBody#")
        buffer.append("\nkey:$key")
        buffer.append("\ntimestamp:$timestamp")
        buffer.append("\ncode:$code")
        if (!data.isEmpty) {
            buffer.append("\ndata{")
            for (key in keySet) {
                buffer.append("\t\t\n$key").append(":").append(this[key])
            }
            buffer.append("\n}")
        }
        return buffer.toString()
    }

}
