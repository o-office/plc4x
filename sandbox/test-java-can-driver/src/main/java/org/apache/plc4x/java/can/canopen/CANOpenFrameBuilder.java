package org.apache.plc4x.java.can.canopen;

import org.apache.plc4x.java.canopen.readwrite.CANOpenPayload;
import org.apache.plc4x.java.canopen.readwrite.types.CANOpenService;

public interface CANOpenFrameBuilder {

    CANOpenFrameBuilder withNodeId(int node);

    CANOpenFrameBuilder withService(CANOpenService service);

    CANOpenFrameBuilder withPayload(CANOpenPayload payload);

    CANOpenFrame build();

}
