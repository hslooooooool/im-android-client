package vip.qsos.im.ui

import android.net.NetworkInfo
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_message.*
import vip.qsos.im.AppApplication
import vip.qsos.im.adapter.MessageAdapter
import vip.qsos.im.app.AbsIMActivity
import vip.qsos.im.app.Constant
import vip.qsos.im.demo.R
import vip.qsos.im.lib.IMManagerHelper
import vip.qsos.im.lib.constant.IMConstant
import vip.qsos.im.lib.model.Message
import vip.qsos.im.lib.model.ReplyBody
import vip.qsos.im.lib.model.SendBody
import java.util.*

/**
 * @author : 华清松
 * 消息界面
 */
class MessageActivity : AbsIMActivity() {

    private lateinit var mAdapter: MessageAdapter
    private var mList: ArrayList<Message> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)
        initViews()
    }

    private fun initViews() {
        mList = ArrayList()
        mAdapter = MessageAdapter(this, mList)
        chat_list.adapter = mAdapter
        chat_send.setOnClickListener {
            val content = chat_content.text.toString().trim()
            if (TextUtils.isEmpty(content)) {
                Toast.makeText(this, "请输入内容!", Toast.LENGTH_LONG).show()
            } else {
                sendMsg(content)
            }
        }
    }

    override fun onConnectionSuccess(hasAutoBind: Boolean) {
        if (!hasAutoBind) {
            IMManagerHelper.bindAccount(this, AppApplication.testAccount)
        }
    }

    override fun onReplyReceived(replyBody: ReplyBody) {
        if (replyBody.key == IMConstant.RequestKey.CLIENT_BIND && replyBody.code == IMConstant.ReturnCode.CODE_200) {
            Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMessageReceived(message: Message) {
        if (message.action == Constant.MessageAction.ACTION_999) {
            Toast.makeText(this, "你被系统强制下线!", Toast.LENGTH_LONG).show()
            this.finish()
        } else {
            mList.add(message)
            mAdapter.notifyDataSetChanged()
        }
    }

    override fun onNetworkChanged(networkInfo: NetworkInfo?) {
        if (networkInfo == null) {
            Toast.makeText(this, "网络已断开!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "网络已恢复，重新连接....", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendMsg(content: String) {
        val sent = SendBody()
        sent.key = "order"
        sent.put("data", content)
        IMManagerHelper.sendRequest(this, sent)
    }
}