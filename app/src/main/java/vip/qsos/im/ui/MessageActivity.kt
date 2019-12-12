package vip.qsos.im.ui

import android.content.Intent
import android.net.NetworkInfo
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.widget.Toast
import com.farsunset.ichat.example.R
import kotlinx.android.synthetic.main.activity_system_chat.*
import vip.qsos.im.adapter.SystemMsgListViewAdapter
import vip.qsos.im.app.AbsIMActivity
import vip.qsos.im.app.Constant
import vip.qsos.im.lib.IMManagerHelper
import vip.qsos.im.lib.model.Message
import java.util.*

/**
 * @author : 华清松
 * 消息界面
 */
class MessageActivity : AbsIMActivity(), OnClickListener {

    lateinit var adapter: SystemMsgListViewAdapter
    private var list: ArrayList<Message> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_system_chat)
        initViews()
    }

    private fun initViews() {
        list = ArrayList()
        top_back.setOnClickListener(this)
        top_back.visibility = View.VISIBLE
        top_back.text = "登录"
        title_txt.text = "系统消息"
        account_1.text = this.intent.getStringExtra("account")
        adapter = SystemMsgListViewAdapter(this, list)
        chat_list.adapter = adapter
        Toast.makeText(this, "登录成功", Toast.LENGTH_LONG).show()
    }

    override fun onMessageReceived(message: Message) {
        if (message.action == Constant.MessageAction.ACTION_999) {
            //返回登录页面，停止接受消息
            IMManagerHelper.stop(this)
            Toast.makeText(this, "你被系统强制下线!", Toast.LENGTH_LONG).show()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            this.finish()
        } else {
            list.add(message)
            adapter.notifyDataSetChanged()
        }
    }

    override fun onNetworkChanged(networkInfo: NetworkInfo?) {
        if (networkInfo == null) {
            Toast.makeText(this, "网络已断开!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "网络已恢复，重新连接....", Toast.LENGTH_LONG).show()
        }
    }

    override fun onClick(v: View) {
        onBackPressed()
    }

    override fun onBackPressed() {
        //返回登录页面，停止接受消息
        IMManagerHelper.stop(this)
        startActivity(Intent(this, LoginActivity::class.java))
        super.onBackPressed()
    }

}
