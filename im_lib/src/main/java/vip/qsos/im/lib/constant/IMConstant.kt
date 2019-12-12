package vip.qsos.im.lib.constant

/**
 * @author : 华清松
 * 消息常量
 */
interface IMConstant {

    companion object {
        /**【定义】消息服务重连时长，毫秒*/
        var RECONNECT_INTERVAL_TIME: Long = 30 * 1000L
        /**【定义】消息头长度3个字节，第一个字节为消息类型，第二、三字节转换 int 后为消息长度*/
        const val DATA_HEADER_LENGTH: Int = 3
    }

    interface ReturnCode {
        companion object {
            const val CODE_404 = "404"
            const val CODE_403 = "403"
            const val CODE_405 = "405"
            const val CODE_200 = "200"
            const val CODE_206 = "206"
            const val CODE_500 = "500"
        }
    }

    /**消息类型*/
    interface ProtobufType {
        companion object {
            /**客户端心跳*/
            const val HEART_CR: Byte = 0
            /**服务端心跳*/
            const val HEART_RQ: Byte = 1
            /**消息*/
            const val MESSAGE: Byte = 2
            /**客户端消息发送*/
            const val SEND_BODY: Byte = 3
            /**服务端消息回执*/
            const val REPLY_BODY: Byte = 4
        }
    }

    interface RequestKey {
        companion object {
            const val CLIENT_BIND = "client_bind"
            const val CLIENT_LOGOUT = "client_logout"
        }
    }

    interface MessageAction {
        companion object {
            // 被其他设备登录挤下线消息
            const val ACTION_999 = "999"
        }
    }

    interface IntentAction {
        companion object {
            // 收到消息广播
            const val ACTION_MESSAGE_RECEIVED = "ACTION_MESSAGE_RECEIVED"
            // 消息发送成功广播
            const val ACTION_SENT_SUCCESS = "ACTION_SENT_SUCCESS"
            // 消息发送失败广播
            const val ACTION_SENT_FAILED = "ACTION_SENT_FAILED"
            // 链接关闭广播
            const val ACTION_CONNECTION_CLOSED = "ACTION_CONNECTION_CLOSED"
            // 链接失败广播
            const val ACTION_CONNECTION_FAILED = "ACTION_CONNECTION_FAILED"
            // 链接成功广播
            const val ACTION_CONNECTION_SUCCESS = "ACTION_CONNECTION_SUCCESS"
            // 消息发送成功后，获得服务器回执广播
            const val ACTION_REPLY_RECEIVED = "ACTION_REPLY_RECEIVED"
            // 网络变化广播
            const val ACTION_NETWORK_CHANGED = "ACTION_NETWORK_CHANGED"
            // 重试连接
            const val ACTION_CONNECTION_RECOVERY = "ACTION_CONNECTION_RECOVERY"
        }
    }

}
