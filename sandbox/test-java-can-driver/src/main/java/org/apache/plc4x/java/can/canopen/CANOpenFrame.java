package org.apache.plc4x.java.can.canopen;

import org.apache.plc4x.java.canopen.readwrite.CANOpenPayload;
import org.apache.plc4x.java.canopen.readwrite.types.CANOpenService;
import org.apache.plc4x.java.spi.generation.Message;

public interface CANOpenFrame extends Message {

    int getNodeId();

    CANOpenService getService();

    CANOpenPayload getPayload();

}
