package org.apache.plc4x.java.can.listener;

import org.apache.plc4x.java.can.canopen.CANOpenFrame;

public interface Callback {
    void receive(CANOpenFrame frame);
}

