package vip.qsos.im.lib.constant

/**常量*/
interface CIMConstant {

    interface ReturnCode {
        companion object {
            val CODE_404 = "404"
            val CODE_403 = "403"
            val CODE_405 = "405"
            val CODE_200 = "200"
            val CODE_206 = "206"
            val CODE_500 = "500"
        }
    }

    interface ProtobufType {
        companion object {
            val C_H_RS: Byte = 0
            val S_H_RQ: Byte = 1
            val MESSAGE: Byte = 2
            val SENTBODY: Byte = 3
            val REPLYBODY: Byte = 4
        }
    }

    interface RequestKey {
        companion object {
            val CLIENT_BIND = "client_bind"
            val CLIENT_LOGOUT = "client_logout"
        }
    }

    interface MessageAction {
        companion object {
            // 被其他设备登录挤下线消息
            val ACTION_999 = "999"
        }
    }

    interface IntentAction {
        companion object {
            // 消息广播action
            val ACTION_MESSAGE_RECEIVED = "com.farsunset.cim.MESSAGE_RECEIVED"
            // 发送sendbody成功广播
            val ACTION_SENT_SUCCESSED = "com.farsunset.cim.SENT_SUCCESSED"
            // 链接意外关闭广播
            val ACTION_CONNECTION_CLOSED = "com.farsunset.cim.CONNECTION_CLOSED"
            // 链接失败广播
            val ACTION_CONNECTION_FAILED = "com.farsunset.cim.CONNECTION_FAILED"
            // 链接成功广播
            val ACTION_CONNECTION_SUCCESSED = "com.farsunset.cim.CONNECTION_SUCCESSED"
            // 发送sendbody成功后获得replaybody回应广播
            val ACTION_REPLY_RECEIVED = "com.farsunset.cim.REPLY_RECEIVED"
            // 网络变化广播
            val ACTION_NETWORK_CHANGED = "com.farsunset.cim.NETWORK_CHANGED"
            // 重试连接
            val ACTION_CONNECTION_RECOVERY = "com.farsunset.cim.CONNECTION_RECOVERY"
        }
    }

    companion object {
        val RECONN_INTERVAL_TIME = 30 * 1000L
        // 消息头长度为3个字节，第一个字节为消息类型，第二，第三字节 转换int后为消息长度
        val DATA_HEADER_LENGTH = 3
    }

}
