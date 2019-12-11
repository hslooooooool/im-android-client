package vip.qsos.im.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.farsunset.ichat.example.R
import kotlinx.android.synthetic.main.item_chat_sysmsg.view.*
import vip.qsos.im.lib.model.Message
import java.text.SimpleDateFormat
import java.util.*

class SystemMsgListViewAdapter(
    var context: Context,
    var list: List<Message>
) : BaseAdapter() {

    override fun getCount(): Int {
        return list.size
    }

    override fun getItem(position: Int): Message {
        return list[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, chatItemView: View?, parent: ViewGroup?): View? {
        val itemView = LayoutInflater.from(context).inflate(R.layout.item_chat_sysmsg, null)
        val msg = getItem(position)
        itemView.textMsgType.text = "系统消息"
        itemView.time.text = getDateTimeString(msg.timestamp)
        itemView.content.text = msg.content
        itemView.headImageView.setImageResource(R.drawable.icon)
        return itemView
    }

    companion object {

        @SuppressLint("SimpleDateFormat")
        fun getDateTimeString(t: Long): String {
            val sdf = SimpleDateFormat("yyy-MM-dd HH:mm:ss")
            return sdf.format(Date(t))
        }
    }

}
