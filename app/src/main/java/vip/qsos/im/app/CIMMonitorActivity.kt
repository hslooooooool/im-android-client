package vip.qsos.im.app

import android.app.Activity
import android.net.NetworkInfo
import android.os.Bundle

import vip.qsos.im.lib.IMEventListener
import vip.qsos.im.lib.IMListenerManager
import vip.qsos.im.lib.model.Message
import vip.qsos.im.lib.model.ReplyBody
import vip.qsos.im.lib.model.SendBody

abstract class CIMMonitorActivity : Activity(), IMEventListener {

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

    override fun onMessageReceived(arg0: Message) {}

    override fun onNetworkChanged(info: NetworkInfo?) {}

    override fun onConnectionClosed() {}

    override fun onConnectionFailed() {}

    override val eventDispatchOrder: Int
        get() = 0

    override fun onConnectionSuccess(arg0: Boolean) {}

    override fun onReplyReceived(arg0: ReplyBody) {}

    override fun onSentSuccess(sentBody: SendBody) {}
}
