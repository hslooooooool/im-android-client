package vip.qsos.im.lib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Build;

import vip.qsos.im.lib.constant.CIMConstant;
import vip.qsos.im.lib.model.Message;
import vip.qsos.im.lib.model.ReplyBody;
import vip.qsos.im.lib.model.SentBody;

/**
 * 消息入口，所有消息都会经过这里
 */
public abstract class CIMEventBroadcastReceiver extends BroadcastReceiver {
    protected Context context;
    @Override
    public void onReceive(Context context, Intent intent) {

        this.context = context;

        /*
         * 操作事件广播，用于提高service存活率
         */
        if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)
                || intent.getAction().equals(Intent.ACTION_POWER_CONNECTED)
                || intent.getAction().equals(Intent.ACTION_POWER_DISCONNECTED)) {
            startPushService();
        }

        /*
         * 设备网络状态变化事件
         */
        if (intent.getAction().equals(CIMConstant.IntentAction.Companion.getACTION_NETWORK_CHANGED())
                || intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {

            onDevicesNetworkChanged();
        }

        /*
         * cim断开服务器事件
         */
        if (intent.getAction().equals(CIMConstant.IntentAction.Companion.getACTION_CONNECTION_CLOSED())) {
            onInnerConnectionClosed();
        }

        /*
         * cim连接服务器失败事件
         */
        if (intent.getAction().equals(CIMConstant.IntentAction.Companion.getACTION_CONNECTION_FAILED())) {
            long interval = intent.getLongExtra("interval", CIMConstant.Companion.getRECONN_INTERVAL_TIME());
            onConnectionFailed(interval);
        }

        /*
         * cim连接服务器成功事件
         */
        if (intent.getAction().equals(CIMConstant.IntentAction.Companion.getACTION_CONNECTION_SUCCESSED())) {
            onInnerConnectionSuccessed();
        }

        /*
         * 收到推送消息事件
         */
        if (intent.getAction().equals(CIMConstant.IntentAction.Companion.getACTION_MESSAGE_RECEIVED())) {
            onInnerMessageReceived((Message) intent.getSerializableExtra(Message.class.getName()), intent);
        }

        /*
         * 获取收到replybody成功事件
         */
        if (intent.getAction().equals(CIMConstant.IntentAction.Companion.getACTION_REPLY_RECEIVED())) {
            onReplyReceived((ReplyBody) intent.getSerializableExtra(ReplyBody.class.getName()));
        }


        /*
         * 获取sendbody发送成功事件
         */
        if (intent.getAction().equals(CIMConstant.IntentAction.Companion.getACTION_SENT_SUCCESSED())) {
            onSentSucceed((SentBody) intent.getSerializableExtra(SentBody.class.getName()));
        }

        /*
         * 重新连接，如果断开的话
         */
        if (intent.getAction().equals(CIMConstant.IntentAction.Companion.getACTION_CONNECTION_RECOVERY())) {
            connect(0);
        }
    }

    private void startPushService() {

        Intent intent = new Intent(context, CIMPushService.class);
        intent.setAction(CIMPushManager.ACTION_ACTIVATE_PUSH_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }

    }

    private void onInnerConnectionClosed() {
        CIMCacheManager.putBoolean(context, CIMCacheManager.KEY_CIM_CONNECTION_STATE, false);

        if (CIMPushManager.isNetworkConnected(context)) {
            connect(0);
        }

        onConnectionClosed();
    }

    private void onConnectionFailed(long reinterval) {

        if (CIMPushManager.isNetworkConnected(context)) {

            onConnectionFailed();

            connect(reinterval);
        }
    }

    private void onInnerConnectionSuccessed() {
        CIMCacheManager.putBoolean(context, CIMCacheManager.KEY_CIM_CONNECTION_STATE, true);

        boolean autoBind = CIMPushManager.autoBindAccount(context);
        onConnectionSuccessed(autoBind);
    }

    private void onDevicesNetworkChanged() {

        if (CIMPushManager.isNetworkConnected(context)) {
            connect(0);
        }

        onNetworkChanged();
    }

    private void connect(long delay) {
        Intent serviceIntent = new Intent(context, CIMPushService.class);
        serviceIntent.putExtra(CIMPushService.KEY_DELAYED_TIME, delay);
        serviceIntent.setAction(CIMPushManager.ACTION_CREATE_CIM_CONNECTION);
        CIMPushManager.startService(context, serviceIntent);
    }

    private void onInnerMessageReceived(Message message, Intent intent) {
        if (isForceOfflineMessage(message.getAction())) {
            CIMPushManager.stop(context);
        }

        onMessageReceived(message, intent);
    }

    private boolean isForceOfflineMessage(String action) {
        return CIMConstant.MessageAction.Companion.getACTION_999().equals(action);
    }

    public abstract void onMessageReceived(Message message, Intent intent);

    public void onNetworkChanged() {
        CIMListenerManager.notifyOnNetworkChanged(CIMPushManager.getNetworkInfo(context));
    }

    public void onConnectionSuccessed(boolean hasAutoBind) {
        CIMListenerManager.notifyOnConnectionSuccessed(hasAutoBind);
    }

    public void onConnectionClosed() {
        CIMListenerManager.notifyOnConnectionClosed();
    }

    public void onConnectionFailed() {
        CIMListenerManager.notifyOnConnectionFailed();
    }

    public void onReplyReceived(ReplyBody body) {
        CIMListenerManager.notifyOnReplyReceived(body);
    }

    public void onSentSucceed(SentBody body) {
        CIMListenerManager.notifyOnSentSucceed(body);
    }
}
