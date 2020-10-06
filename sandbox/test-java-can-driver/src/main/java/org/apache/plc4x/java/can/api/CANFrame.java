package org.apache.plc4x.java.can.api;

public interface CANFrame {

    int getIdentifier();
    boolean getExtended();
    boolean getRemote();
    boolean getError();
    byte[] getData();

}
