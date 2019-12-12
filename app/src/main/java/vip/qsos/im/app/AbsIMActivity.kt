package vip.qsos.im.app

import android.app.Activity
import android.net.NetworkInfo
import android.os.Bundle

import vip.qsos.im.lib.IMEventListener
import vip.qsos.im.lib.IMListenerManager
import vip.qsos.im.lib.model.Message
import vip.qsos.im.lib.model.ReplyBody
import vip.qsos.im.lib.model.SendBody

/**
 * @author : 华清松
 * 消息服务活动基类
 */
abstract class AbsIMActivity : Activity(), IMEventListener {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        IMListenerManager.registerMessageListener(this)
    }

    override fun finish() {
        super.finish()
        IMListenerManager.removeMessageListener(this)
    }

    public override fun onRestart() {
        super.onRestart()
        IMListenerManager.registerMessageListener(this)
    }

    override fun onMessageReceived(message: Message) {}

    override fun onNetworkChanged(networkInfo: NetworkInfo?) {}

    override fun onConnectionClosed() {}

    override fun onConnectionFailed() {}

    override val eventDispatchOrder: Int = 0

    override fun onConnectionSuccess(hasAutoBind: Boolean) {}

    override fun onReplyReceived(replyBody: ReplyBody) {}

    override fun onSentSuccess(sendBody: SendBody) {}
}
