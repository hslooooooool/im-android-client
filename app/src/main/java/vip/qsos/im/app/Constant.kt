package vip.qsos.im.app

interface Constant {

    /**自定义消息 Action 类型*/
    interface MessageAction {
        companion object {
            /**下线类型*/
            const val ACTION_999 = "999"
        }
    }

    companion object {
        /**服务端IP地址*/
        var CIM_SERVER_HOST = "192.168.1.8"
        /**服务端消息端口*/
        var CIM_SERVER_PORT = 23456
    }
}
