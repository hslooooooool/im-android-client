package vip.qsos.im.lib.model;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

import vip.qsos.im.lib.constant.CIMConstant;

/**
 * 客户端心跳响应
 */
public class HeartbeatResponse implements Serializable, Protobufable {

    private static final long serialVersionUID = 1L;
    private static final String TAG = "CLIENT_HEARTBEAT_RESPONSE";
    private static final String CMD_HEARTBEAT_RESPONSE = "CR";

    private static HeartbeatResponse object = new HeartbeatResponse();

    private HeartbeatResponse() {

    }

    public static HeartbeatResponse getInstance() {
        return object;
    }

    @Override
    public byte[] getByteArray() {
        return CMD_HEARTBEAT_RESPONSE.getBytes();
    }

    @Override
    @NotNull
    public String toString() {
        return TAG;
    }

    @Override
    public byte getType() {
        return CIMConstant.ProtobufType.Companion.getC_H_RS();
    }

}
