package org.apache.plc4x.java.can.socketcan;

import org.apache.plc4x.java.can.api.CANFrame;
import org.apache.plc4x.java.socketcan.readwrite.SocketCANFrame;

public class SocketCANDelegateFrame implements CANFrame {

    private final SocketCANFrame frame;

    public SocketCANDelegateFrame(SocketCANFrame frame) {
        this.frame = frame;
    }

    @Override
    public int getIdentifier() {
        return frame.getIdentifier();
    }

    @Override
    public boolean getExtended() {
        return frame.getExtended();
    }

    @Override
    public boolean getRemote() {
        return frame.getRemote();
    }

    @Override
    public boolean getError() {
        return frame.getError();
    }

    @Override
    public byte[] getData() {
        return frame.getData();
    }

    public SocketCANFrame getFrame() {
        return frame;
    }

}
