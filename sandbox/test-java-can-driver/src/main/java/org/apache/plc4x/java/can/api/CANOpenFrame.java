package org.apache.plc4x.java.can.api;

import org.apache.plc4x.java.api.exceptions.PlcRuntimeException;
import org.apache.plc4x.java.canopen.readwrite.CANOpenPayload;
import org.apache.plc4x.java.canopen.readwrite.io.CANOpenPayloadIO;
import org.apache.plc4x.java.canopen.readwrite.types.CANOpenService;
import org.apache.plc4x.java.spi.generation.ParseException;
import org.apache.plc4x.java.spi.generation.ReadBuffer;

public class CANOpenFrame implements CANFrame {

    private final CANFrame frame;

    private final CANOpenPayload payload;

    public CANOpenFrame(CANFrame frame) {
        this.frame = frame;
        try {
            this.payload = CANOpenPayloadIO.staticParse(new ReadBuffer(frame.getData(), true), serviceId(frame.getIdentifier()));
        } catch (ParseException e) {
            throw new PlcRuntimeException("Could not parse CANopen payload", e);
        }
    }

    public CANOpenPayload getPayload() {
        return payload;
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

    private CANOpenService serviceId(int cobId) {
        // form 32 bit socketcan identifier
        CANOpenService service = CANOpenService.valueOf((byte) (cobId >> 7));
        if (service == null) {
            for (CANOpenService val : CANOpenService.values()) {
                if (val.getMin() > cobId && val.getMax() < cobId) {
                    return val;
                }
            }
        }
        return service;
    }
}
