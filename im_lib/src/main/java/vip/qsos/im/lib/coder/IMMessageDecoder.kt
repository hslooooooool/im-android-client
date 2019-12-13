package vip.qsos.im.lib.coder

import com.google.protobuf.InvalidProtocolBufferException
import vip.qsos.im.lib.constant.IMConstant
import vip.qsos.im.lib.model.HeartbeatRequest
import vip.qsos.im.lib.model.Message
import vip.qsos.im.lib.model.ReplyBody
import vip.qsos.im.lib.model.proto.MessageProto
import vip.qsos.im.lib.model.proto.ReplyBodyProto
import java.nio.ByteBuffer

/**
 * @author : 华清松
 * 服务端发送的消息解码
 */
class IMMessageDecoder {

    /**服务器消息解析*/
    fun decode(buffer: ByteBuffer): Any? {
        /**消息头3位，小于3位消息体为 null */
        if (buffer.remaining() < IMConstant.DATA_HEADER_LENGTH) {
            return null
        }
        buffer.mark()
        /**获取消息类型*/
        val type = buffer.get()
        /**获取消息体长度低位*/
        val lv = buffer.get()
        /**获取消息体长度高位*/
        val hv = buffer.get()
        /**获取到消息体长度*/
        val length = getContentLength(lv.toInt(), hv.toInt())

        /**消息体没有接收完整，则重置读取，等待下一次重新读取*/
        if (length > buffer.remaining()) {
            buffer.reset()
            return null
        }

        /**获取到消息体数组*/
        val dataBytes = ByteArray(length)
        buffer.get(dataBytes, 0, length)
        buffer.position(0)
        return try {
            mappingMessageObject(dataBytes, type)
        } catch (e: InvalidProtocolBufferException) {
            null
        }
    }

    /**构建消息实体*/
    @Throws(InvalidProtocolBufferException::class)
    private fun mappingMessageObject(bytes: ByteArray, type: Byte): Any? {
        return when (type) {
            IMConstant.ProtobufType.HEART_RQ -> HeartbeatRequest.instance
            IMConstant.ProtobufType.REPLY_BODY -> {
                val bodyProto = ReplyBodyProto.Model.parseFrom(bytes)
                val body = ReplyBody()
                body.key = bodyProto.key
                body.timestamp = bodyProto.timestamp
                body.putAll(bodyProto.dataMap)
                body.code = bodyProto.code
                body.message = bodyProto.message

                body
            }
            IMConstant.ProtobufType.MESSAGE -> {
                val bodyProto = MessageProto.Model.parseFrom(bytes)
                val message = Message()
                message.id = bodyProto.id
                message.action = bodyProto.action
                message.content = bodyProto.content
                message.sender = bodyProto.sender
                message.receiver = bodyProto.receiver
                message.title = bodyProto.title
                message.extra = bodyProto.extra
                message.timestamp = bodyProto.timestamp
                message.format = bodyProto.format

                message
            }
            else -> null
        }
    }

    /**解析消息体长度*/
    private fun getContentLength(lv: Int, hv: Int): Int {
        val l = lv and 0xff
        val h = hv and 0xff
        return l or (h shl 8)
    }

}
