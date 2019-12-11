package vip.qsos.im.app

import android.app.Activity
import android.net.NetworkInfo
import android.os.Bundle

import vip.qsos.im.lib.CIMEventListener
import vip.qsos.im.lib.CIMListenerManager
import vip.qsos.im.lib.model.Message
import vip.qsos.im.lib.model.ReplyBody
import vip.qsos.im.lib.model.SentBody

abstract class CIMMonitorActivity : Activity(), CIMEventListener {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CIMListenerManager.registerMessageListener(this)
    }

    override fun finish() {
        super.finish()
        CIMListenerManager.removeMessageListener(this)
    }

    public override fun onRestart() {
        super.onRestart()
        CIMListenerManager.registerMessageListener(this)
    }

    override fun onMessageReceived(arg0: Message) {}

    override fun onNetworkChanged(info: NetworkInfo?) {}

    override fun onConnectionClosed() {}

    override fun onConnectionFailed() {}

    override fun getEventDispatchOrder(): Int {
        return 0
    }

    override fun onConnectionSuccessed(arg0: Boolean) {}

    override fun onReplyReceived(arg0: ReplyBody) {}

    override fun onSentSuccessed(sentBody: SentBody) {}
}
