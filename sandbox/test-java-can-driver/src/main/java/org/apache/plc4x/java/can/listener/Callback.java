package org.apache.plc4x.java.can.listener;

import org.apache.plc4x.java.socketcan.readwrite.SocketCANFrame;

public interface Callback {
    void receive(SocketCANFrame frame);
}

