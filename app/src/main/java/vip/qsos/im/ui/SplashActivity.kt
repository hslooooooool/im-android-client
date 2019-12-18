package vip.qsos.im.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import vip.qsos.im.app.AbsIMActivity
import vip.qsos.im.app.Constant
import vip.qsos.im.demo.BuildConfig
import vip.qsos.im.demo.R
import vip.qsos.im.lib.IMManagerHelper

class SplashActivity : AbsIMActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        IMManagerHelper.setLoggerEnable(this, BuildConfig.DEBUG)
        IMManagerHelper.connect(this, Constant.IM_SERVER_HOST, Constant.IM_SERVER_PORT)
        val view = View.inflate(this, R.layout.activity_splansh, null)
        setContentView(view)
    }

    override fun onConnectionSuccess(hasAutoBind: Boolean) {
        val intent = Intent(this@SplashActivity, MessageActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        IMManagerHelper.destroy(this)
        finish()
    }

    override fun onConnectionFailed() {
        Toast.makeText(this, "连接服务器失败，请检查当前设备是否能连接上服务器IP和端口", Toast.LENGTH_LONG).show()
    }
}
