package vip.qsos.im.ui

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_splansh.*
import vip.qsos.im.AppApplication
import vip.qsos.im.app.AbsIMActivity
import vip.qsos.im.app.Constant
import vip.qsos.im.demo.BuildConfig
import vip.qsos.im.demo.R
import vip.qsos.im.lib.IMManagerHelper
import vip.qsos.im.lib.constant.IMConstant
import vip.qsos.im.lib.model.ReplyBody

class SplashActivity : AbsIMActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        IMManagerHelper.setLoggerEnable(this, BuildConfig.DEBUG)
        IMManagerHelper.connect(this, Constant.IM_SERVER_HOST, Constant.IM_SERVER_PORT)
        val view = View.inflate(this, R.layout.activity_splansh, null)
        setContentView(view)
        im_login.setOnClickListener {
            doLogin()
        }
    }

    override fun onConnectionSuccess(hasAutoBind: Boolean) {
        if (!hasAutoBind) {
            IMManagerHelper.bindAccount(this, AppApplication.testAccount)
        }
    }

    override fun onReplyReceived(replyBody: ReplyBody) {
        super.onReplyReceived(replyBody)
        if (replyBody.key == IMConstant.RequestKey.CLIENT_BIND && replyBody.code == IMConstant.ReturnCode.CODE_200) {
            val intent = Intent(this@SplashActivity, MessageActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onBackPressed() {
        IMManagerHelper.destroy(this)
        finish()
    }

    override fun onConnectionFailed() {
        Toast.makeText(this, "连接服务器失败，请检查当前设备是否能连接上服务器IP和端口", Toast.LENGTH_LONG).show()
    }

    private fun doLogin() {
        val account = im_account.text.toString().trim()
        AppApplication.testAccount = account

        if (!TextUtils.isEmpty(account)) {
            if (IMManagerHelper.isConnected(this)) {
                IMManagerHelper.bindAccount(this, account)
            } else {
                IMManagerHelper.connect(this, Constant.IM_SERVER_HOST, Constant.IM_SERVER_PORT)
            }
        }
    }
}
