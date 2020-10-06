package org.apache.plc4x.java.can.api.conversation.canopen;

public interface CANFrameBuilder<W> {

    CANFrameBuilder<W> node(int node);

    CANFrameBuilder<W> data(byte[] data);

    W build();

}