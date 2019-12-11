package vip.qsos.im.lib.coder

import android.util.Log
import java.nio.channels.SocketChannel

/**
 * @author : 华清松
 * 日志打印工具类
 */
class ImLogUtils {

    companion object {
        const val TAG = "IM服务"

        val logger: ImLogUtils by lazy { Holder.logger }
    }

    private var debug = true

    private object Holder {
        val logger = ImLogUtils()
    }

    fun open(mode: Boolean) {
        debug = mode
    }

    fun messageReceived(session: SocketChannel, message: Any) {
        if (debug) {
            Log.i(TAG, String.format("[RECEIVED]" + getSessionInfo(session) + "\n%s", message))
        }
    }

    fun messageSent(session: SocketChannel, message: Any) {
        if (debug) {
            Log.i(TAG, String.format("[  SENT  ]" + getSessionInfo(session) + "\n%s", message))
        }
    }

    fun sessionCreated(session: SocketChannel) {
        if (debug) {
            Log.i(TAG, "[ OPENED ]" + getSessionInfo(session))
        }
    }

    fun sessionIdle(session: SocketChannel) {
        if (debug) {
            Log.d(TAG, "[  IDLE  ]" + getSessionInfo(session))
        }
    }

    fun sessionClosed(session: SocketChannel) {
        if (debug) {
            Log.w(TAG, "[ CLOSED ] ID = " + session.hashCode())
        }
    }

    fun connectFailure(interval: Long) {
        if (debug) {
            Log.d(TAG, "CONNECT FAILURE, TRY RECONNECT AFTER " + interval + "ms")
        }
    }

    fun startConnect(host: String, port: Int) {
        if (debug) {
            Log.i(TAG, "START CONNECT REMOTE HOST:$host PORT:$port")
        }
    }

    fun invalidHostPort(host: String, port: Int) {
        if (debug) {
            Log.d(TAG, "INVALID SOCKET ADDRESS -> HOST:$host PORT:$port")
        }
    }

    fun connectState(isConnected: Boolean) {
        if (debug) {
            Log.d(TAG, "CONNECTED:$isConnected")
        }
    }

    fun connectState(isConnected: Boolean, isManualStop: Boolean, isDestroyed: Boolean) {
        if (debug) {
            Log.d(TAG, "CONNECTED:$isConnected STOPED:$isManualStop DESTROYED:$isDestroyed")
        }
    }

    private fun getSessionInfo(session: SocketChannel?): String {
        val builder = StringBuilder()
        if (session == null) {
            return ""
        }
        builder.append(" [")
        builder.append("id:").append(session.hashCode())
        try {
            if (session.socket().localAddress != null) {
                builder.append(" L:")
                    .append(session.socket().localAddress.toString() + ":" + session.socket().localPort)
            }
        } catch (ignore: Exception) {
        }

        try {
            if (session.socket().remoteSocketAddress != null) {
                builder.append(" R:").append(session.socket().remoteSocketAddress.toString())
            }
        } catch (ignore: Exception) {
        }

        builder.append("]")
        return builder.toString()
    }

}
