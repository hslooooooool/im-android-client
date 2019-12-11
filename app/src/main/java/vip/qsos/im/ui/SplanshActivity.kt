package vip.qsos.im.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.Toast

import androidx.annotation.RequiresApi

import com.farsunset.ichat.example.BuildConfig
import com.farsunset.ichat.example.R

import vip.qsos.im.app.CIMMonitorActivity
import vip.qsos.im.app.Constant
import vip.qsos.im.lib.CIMPushManager

class SplanshActivity : CIMMonitorActivity() {

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CIMPushManager.setLoggerEnable(this, BuildConfig.DEBUG)
        //连接服务端
        CIMPushManager.connect(
            this@SplanshActivity,
            Constant.CIM_SERVER_HOST,
            Constant.CIM_SERVER_PORT
        )
        val view = View.inflate(this, R.layout.activity_splansh, null)
        setContentView(view)
        val aa = AlphaAnimation(0.3f, 1.0f)
        aa.duration = 2000
        view.startAnimation(aa)
    }

    override fun onConnectionSuccessed(autoBind: Boolean) {

        val intent = Intent(this@SplanshActivity, LoginActivity::class.java)
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
