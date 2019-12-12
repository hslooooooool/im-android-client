package vip.qsos.im.lib.coder

import vip.qsos.im.lib.constant.IMConstant
import vip.qsos.im.lib.model.IProtobufAble
import java.nio.ByteBuffer

/**
 * @author : 华清松
 * 客户端消息发送前编码
 */
class IMMessageEncoder {

    /**对消息数据进行编码 */
    fun encode(obj: Any): ByteBuffer {
        val data = obj as IProtobufAble
        val byteArray = data.byteArray
        val buffer = ByteBuffer.allocate(IMConstant.DATA_HEADER_LENGTH + byteArray.size)
        buffer.put(createHeader(data.type, byteArray.size))
        buffer.put(byteArray)
        buffer.flip()
        return buffer
    }

    /**组建消息体数据，将消息类型与消息长度加入。【定义】消息体最大为65535 */
    private fun createHeader(type: Byte, length: Int): ByteArray {
        val header = ByteArray(IMConstant.DATA_HEADER_LENGTH)
        header[0] = type
        header[1] = (length and 0xff).toByte()
        header[2] = (length shr 8 and 0xff).toByte()
        return header
    }

}
