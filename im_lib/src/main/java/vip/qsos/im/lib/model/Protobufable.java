package vip.qsos.im.lib.model;

/**
 * 需要向另一端发送的结构体
 */
public interface Protobufable {

	byte[] getByteArray();

	byte getType();
}
