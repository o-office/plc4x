package org.apache.plc4x.java.can.socketcan;

import org.apache.plc4x.java.can.api.conversation.canopen.CANConversation;
import org.apache.plc4x.java.can.canopen.CANOpenFrame;
import org.apache.plc4x.java.can.canopen.CANOpenFrameBuilder;
import org.apache.plc4x.java.can.canopen.CANOpenFrameBuilderFactory;
import org.apache.plc4x.java.spi.ConversationContext;
import org.apache.plc4x.java.spi.ConversationContext.SendRequestContext;

import java.time.Duration;
import java.util.function.Consumer;

public class SocketCANConversation implements CANConversation<CANOpenFrame> {

    private final int nodeId;
    private final ConversationContext<CANOpenFrame> context;
    private final CANOpenFrameBuilderFactory factory;

    public SocketCANConversation(int nodeId, ConversationContext<CANOpenFrame> context, CANOpenFrameBuilderFactory factory) {
        this.nodeId = nodeId;
        this.context = context;
        this.factory = factory;
    }

    @Override
    public int getNodeId() {
        return nodeId;
    }

    @Override
    public CANOpenFrameBuilder createBuilder() {
        return factory.createBuilder();
    }

    @Override
    public void send(CANOpenFrame frame, Consumer<SendRequestContext<CANOpenFrame>> callback) {
        SendRequestContext<CANOpenFrame> ctx = context.sendRequest(frame)
            .expectResponse(CANOpenFrame.class, Duration.ofSeconds(10L));
        callback.accept(ctx);
    }

}
