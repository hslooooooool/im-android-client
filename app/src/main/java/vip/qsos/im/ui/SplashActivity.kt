package vip.qsos.im.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast

import androidx.annotation.RequiresApi

import com.farsunset.ichat.example.BuildConfig
import com.farsunset.ichat.example.R

import vip.qsos.im.app.AbsIMActivity
import vip.qsos.im.app.Constant
import vip.qsos.im.lib.CIMPushManager

class SplashActivity : AbsIMActivity() {

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CIMPushManager.setLoggerEnable(this, BuildConfig.DEBUG)
        CIMPushManager.connect(
            this@SplashActivity,
            Constant.CIM_SERVER_HOST,
            Constant.CIM_SERVER_PORT
        )
        val view = View.inflate(this, R.layout.activity_splansh, null)
        setContentView(view)
    }

    override fun onConnectionSuccess(arg0: Boolean) {
        val intent = Intent(this@SplashActivity, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }


    override fun onBackPressed() {
        finish()
        CIMPushManager.destroy(this)
    }

    override fun onConnectionFailed() {
        Toast.makeText(this, "连接服务器失败，请检查当前设备是否能连接上服务器IP和端口", Toast.LENGTH_LONG).show()
    }
}
