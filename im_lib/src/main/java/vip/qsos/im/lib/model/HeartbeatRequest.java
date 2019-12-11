package vip.qsos.im.lib.model;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

import vip.qsos.im.lib.constant.CIMConstant;

/**
 * 服务端心跳请求
 */
public class HeartbeatRequest implements Serializable, Protobufable {

    private static final long serialVersionUID = 1L;
    private static final String TAG = "SERVER_HEARTBEAT_REQUEST";
    private static final String CMD_HEARTBEAT_REQUEST = "SR";

    private static HeartbeatRequest object = new HeartbeatRequest();

    private HeartbeatRequest() {

    }

    public static HeartbeatRequest getInstance() {
        return object;
    }

    @Override
    public byte[] getByteArray() {
        return CMD_HEARTBEAT_REQUEST.getBytes();
    }

    @Override
    @NotNull
    public String toString() {
        return TAG;
    }

    @Override
    public byte getType() {
        return CIMConstant.ProtobufType.Companion.getS_H_RQ();
    }

}
