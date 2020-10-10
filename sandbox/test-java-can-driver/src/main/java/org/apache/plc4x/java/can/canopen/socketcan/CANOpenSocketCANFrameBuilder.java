package org.apache.plc4x.java.can.canopen.socketcan;

import org.apache.plc4x.java.can.canopen.CANOpenFrame;
import org.apache.plc4x.java.can.canopen.CANOpenFrameBuilder;
import org.apache.plc4x.java.canopen.readwrite.CANOpenPayload;
import org.apache.plc4x.java.canopen.readwrite.types.CANOpenService;

public class CANOpenSocketCANFrameBuilder implements CANOpenFrameBuilder {

    private int node;
    private CANOpenPayload payload;
    private CANOpenService service;

    @Override
    public CANOpenFrameBuilder withNodeId(int node) {
        this.node = node;
        return this;
    }

    @Override
    public CANOpenFrameBuilder withService(CANOpenService service) {
        this.service = service;
        return this;
    }

    @Override
    public CANOpenFrameBuilder withPayload(CANOpenPayload payload) {
        this.payload = payload;
        return this;
    }

    @Override
    public CANOpenFrame build() {
        return new CANOpenSocketCANFrame(node, service, payload);
    }

}
