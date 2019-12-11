package vip.qsos.im.lib.model

import vip.qsos.im.lib.constant.IMConstant
import vip.qsos.im.lib.model.proto.SentBodyProto
import java.util.*

/**
 * @author : 华清松
 * 客户端请求对象
 */
class SendBody : IProtobufAble {

    companion object {
        private const val serialVersionUID = 1L
    }

    /**请求key*/
    var key: String? = null
    /**发送时间*/
    var timestamp: Long = 0L
    /**发送数据集合*/
    val data: Hashtable<String, String> = Hashtable()

    init {
        timestamp = System.currentTimeMillis()
    }

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

    override val byteArray: ByteArray
        get() {
            val builder = SentBodyProto.Model.newBuilder()
            builder.key = key
            builder.timestamp = timestamp
            if (!data.isEmpty) {
                builder.putAllData(data)
            }
            return builder.build().toByteArray()
        }

    override val type: Byte = IMConstant.ProtobufType.SEND_BODY

    override fun toString(): String {
        val buffer = StringBuffer()
        buffer.append("\n#SendBody#")
        buffer.append("\nkey:$key")
        buffer.append("\ntimestamp:").append(timestamp)
        if (!data.isEmpty) {
            buffer.append("\ndata{").append("\n")
            for (key in keySet) {
                buffer.append("\t\t\n$key").append(":").append(this[key])
            }
            buffer.append("\n}")
        }
        return buffer.toString()
    }

}
