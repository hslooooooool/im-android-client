package vip.qsos.im.adapter

import java.util.Comparator

import vip.qsos.im.lib.model.Message

class MessageTimeDescComparator : Comparator<Message> {

    override fun compare(arg0: Message, arg1: Message): Int {

        return (arg1.timestamp - arg0.timestamp).toInt()
    }


}
