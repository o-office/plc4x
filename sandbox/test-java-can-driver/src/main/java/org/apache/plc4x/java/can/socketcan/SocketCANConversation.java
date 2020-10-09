package org.apache.plc4x.java.can.socketcan;

import org.apache.plc4x.java.api.exceptions.PlcRuntimeException;
import org.apache.plc4x.java.can.api.CANFrame;
import org.apache.plc4x.java.can.api.conversation.canopen.CANConversation;
import org.apache.plc4x.java.can.api.conversation.canopen.CANFrameBuilder;
import org.apache.plc4x.java.socketcan.readwrite.SocketCANFrame;
import org.apache.plc4x.java.spi.ConversationContext;
import org.apache.plc4x.java.spi.ConversationContext.SendRequestContext;
import org.apache.plc4x.java.spi.transaction.RequestTransactionManager.RequestTransaction;

import java.time.Duration;
import java.util.function.BiConsumer;

public class SocketCANConversation implements CANConversation<CANFrame> {

    private final int nodeId;
    private final ConversationContext<SocketCANFrame> context;

    public SocketCANConversation(int nodeId, ConversationContext<SocketCANFrame> context) {
        this.nodeId = nodeId;
        this.context = context;
    }

    @Override
    public int getNodeId() {
        return nodeId;
    }

    @Override
    public CANFrameBuilder<CANFrame> frameBuilder() {
        return new SocketCANFrameBuilder();
    }

    @Override
    public void send(RequestTransaction transaction, CANFrame frame, BiConsumer<RequestTransaction, SendRequestContext<CANFrame>> callback) {
        if (frame instanceof SocketCANDelegateFrame) {
            System.out.println("-----> Sending request frame " + transaction);
            transaction.submit(() -> {
                ConversationContext.SendRequestContext<CANFrame> ctx = context.sendRequest(((SocketCANDelegateFrame) frame).getFrame())
                    .expectResponse(SocketCANFrame.class, Duration.ofSeconds(10L))
                    .unwrap(SocketCANDelegateFrame::new);
                System.out.println("-----> Frame been sent " + transaction);
                callback.accept(transaction, ctx);
            });
            return;
        }
        throw new PlcRuntimeException("Unsupported frame type " + frame);
    }

}
