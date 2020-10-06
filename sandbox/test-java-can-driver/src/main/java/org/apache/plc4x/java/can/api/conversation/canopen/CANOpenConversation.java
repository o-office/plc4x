package org.apache.plc4x.java.can.api.conversation.canopen;

import org.apache.plc4x.java.api.exceptions.PlcRuntimeException;
import org.apache.plc4x.java.can.api.CANFrame;
import org.apache.plc4x.java.canopen.readwrite.CANOpenPayload;
import org.apache.plc4x.java.canopen.readwrite.io.CANOpenPayloadIO;
import org.apache.plc4x.java.canopen.readwrite.types.CANOpenService;
import org.apache.plc4x.java.spi.ConversationContext.SendRequestContext;
import org.apache.plc4x.java.spi.generation.ParseException;
import org.apache.plc4x.java.spi.generation.ReadBuffer;
import org.apache.plc4x.java.spi.generation.WriteBuffer;
import org.apache.plc4x.java.spi.transaction.RequestTransactionManager.RequestTransaction;

import java.util.function.BiConsumer;

public class CANOpenConversation<W extends CANFrame> {

    private final int node;
    private final CANConversation<W> delegate;

    public CANOpenConversation(int node, CANConversation<W> delegate) {
        this.node = node;
        this.delegate = delegate;
    }

    public SDOConversation<W> sdo() {
        return new SDOConversation<>(this);
    }

    public void send(CANOpenService service, CANOpenPayload payload, BiConsumer<RequestTransaction, SendRequestContext<CANOpenPayload>> callback) {
        CANFrameBuilder<W> builder = delegate.frameBuilder();
        W frame = builder.node(service.getMin() + node).data(serialize(payload)).build();
        delegate.send(frame, (tx, ctx) -> {
            SendRequestContext<CANOpenPayload> unwrap = ctx
//                .onError((response, error) -> {
//                    System.err.println("Unexpected frame " + response + " " + error);
//                })
            .unwrap(CANOpenConversation.this::deserialize);
            callback.accept(tx, unwrap);
        });
    }

    private CANOpenPayload deserialize(CANFrame frame) {
        try {
            CANOpenService service = serviceId(frame.getIdentifier());
            ReadBuffer buffer = new ReadBuffer(frame.getData(), true);
            return CANOpenPayloadIO.staticParse(buffer, service);
        } catch (ParseException e) {
            throw new PlcRuntimeException("Could not deserialize CAN open payload", e);
        }
    }

    private byte[] serialize(CANOpenPayload payload) {
        try {
            WriteBuffer buffer = new WriteBuffer(payload.getLengthInBytes(), true);
            CANOpenPayloadIO.staticSerialize(buffer, payload);
            return buffer.getData();
        } catch (ParseException e) {
            throw new PlcRuntimeException("Could not serialize CAN open payload", e);
        }
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
