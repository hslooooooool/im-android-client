package vip.qsos.im.lib.model

import vip.qsos.im.lib.constant.IMConstant

/**
 * @author : 华清松
 * 客户端心跳响应实体
 */
class HeartbeatResponse private constructor() : IProtobufAble {

    companion object {
        private const val serialVersionUID = 1L
        const val TAG = "CLIENT_HEARTBEAT_RESPONSE"
        const val CLIENT_HEARTBEAT_RESPONSE = "CR"
        val instance = HeartbeatResponse()
    }

    override val byteArray: ByteArray
        get() = CLIENT_HEARTBEAT_RESPONSE.toByteArray()

    override val type: Byte
        get() = IMConstant.ProtobufType.HEART_CR

    override fun toString(): String {
        return TAG
    }

}
