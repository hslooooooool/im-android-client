package vip.qsos.im.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import kotlinx.android.synthetic.main.item_message.view.*
import vip.qsos.im.demo.R
import vip.qsos.im.lib.model.Message
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author : 华清松
 * 消息列表容器
 */
class MessageAdapter(var context: Context, var list: List<Message>) : BaseAdapter() {

    companion object {
        @SuppressLint("SimpleDateFormat")
        fun getDateTimeString(t: Long): String {
            val sdf = SimpleDateFormat("yyy-MM-dd HH:mm:ss")
            return sdf.format(Date(t))
        }
    }

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
        val itemView = LayoutInflater.from(context).inflate(R.layout.item_message, null)
        val msg = getItem(position)
        itemView.time.text = getDateTimeString(msg.timestamp)
        itemView.content.text = msg.content
        return itemView
    }

}
