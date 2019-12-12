package vip.qsos.im.lib.model

import java.io.Serializable

/**
 * @author : 华清松
 * 自定义消息对象。服务器主动推送到客户端的消息。
 */
class Message : Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }

    /**消息ID*/
    var id: Long = 0
    /**消息类型，自定义消息类型*/
    var action: String? = null
    /**消息标题*/
    var title: String? = null
    /**消息类容，content 根据 format 数据格式进行解析*/
    var content: String? = null
    /**消息发送者账号*/
    var sender: String? = null
    /**消息发送者接收者*/
    var receiver: String? = null
    /**content 内容格式，如 text,json,xml数据格式*/
    var format: String? = null
    /**附加内容*/
    var extra: String? = null
    /**消息发送时间*/
    var timestamp: Long = 0

    init {
        timestamp = System.currentTimeMillis()
    }

    override fun toString(): String {
        return "\n#Message#" +
                "\nid:" + id +
                "\naction:" + action +
                "\ntitle:" + title +
                "\ncontent:" + content +
                "\nextra:" + extra +
                "\nsender:" + sender +
                "\nreceiver:" + receiver +
                "\nformat:" + format +
                "\ntimestamp:" + timestamp
    }

    fun empty(txt: String?): Boolean {
        return txt?.trim { it <= ' ' }?.isEmpty() ?: true
    }

}
