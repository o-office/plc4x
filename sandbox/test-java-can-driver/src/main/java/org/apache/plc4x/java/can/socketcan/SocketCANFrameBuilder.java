package org.apache.plc4x.java.can.socketcan;

import org.apache.plc4x.java.can.api.CANFrame;
import org.apache.plc4x.java.can.api.conversation.canopen.CANFrameBuilder;
import org.apache.plc4x.java.socketcan.readwrite.SocketCANFrame;

public class SocketCANFrameBuilder implements CANFrameBuilder<CANFrame> {

    private int node;
    private byte[] data;

    @Override
    public CANFrameBuilder<CANFrame> node(int node) {
        this.node = node;
        return this;
    }

    @Override
    public CANFrameBuilder<CANFrame> data(byte[] data) {
        this.data = data;
        return this;
    }

    @Override
    public CANFrame build() {
        return new SocketCANDelegateFrame(new SocketCANFrame(node, data));
    }

}
