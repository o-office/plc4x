package org.apache.plc4x.java.can.api.conversation.canopen;

import org.apache.plc4x.java.can.canopen.CANOpenFrame;
import org.apache.plc4x.java.canopen.readwrite.CANOpenPayload;
import org.apache.plc4x.java.canopen.readwrite.types.CANOpenService;
import org.apache.plc4x.java.spi.ConversationContext.SendRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class CANOpenConversation {

    private final Logger logger = LoggerFactory.getLogger(CANOpenConversation.class);
    private final int node;
    private final CANConversation<CANOpenFrame> delegate;

    public CANOpenConversation(int node, CANConversation<CANOpenFrame> delegate) {
        this.node = node;
        this.delegate = delegate;
    }

    public SDOConversation sdo() {
        return new SDOConversation(this);
    }

    public void send(CANOpenService service, CANOpenPayload payload, Consumer<SendRequestContext<CANOpenPayload>> callback) {
        CANOpenFrame frame = delegate.createBuilder().withNodeId(node).withService(service).withPayload(payload).build();
        delegate.send(frame, (ctx) -> {
            SendRequestContext<CANOpenPayload> unwrap = ctx
//                .onError((response, error) -> {
//                    System.err.println("Unexpected frame " + response + " " + error);
//                })
            .unwrap(CANOpenFrame::getPayload);
            callback.accept(unwrap);
        });


    }

}
