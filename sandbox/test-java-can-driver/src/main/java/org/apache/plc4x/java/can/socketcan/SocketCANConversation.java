package org.apache.plc4x.java.can.socketcan;

import org.apache.plc4x.java.api.exceptions.PlcRuntimeException;
import org.apache.plc4x.java.can.api.CANFrame;
import org.apache.plc4x.java.can.api.conversation.canopen.CANConversation;
import org.apache.plc4x.java.can.api.conversation.canopen.CANFrameBuilder;
import org.apache.plc4x.java.socketcan.readwrite.SocketCANFrame;
import org.apache.plc4x.java.spi.ConversationContext;
import org.apache.plc4x.java.spi.ConversationContext.SendRequestContext;
import org.apache.plc4x.java.spi.transaction.RequestTransactionManager;
import org.apache.plc4x.java.spi.transaction.RequestTransactionManager.RequestTransaction;

import java.time.Duration;
import java.util.function.BiConsumer;

public class SocketCANConversation implements CANConversation<CANFrame> {

    private final RequestTransactionManager tm;
    private final ConversationContext<SocketCANFrame> context;

    public SocketCANConversation(RequestTransactionManager tm, ConversationContext<SocketCANFrame> context) {
        this.tm = tm;
        this.context = context;
    }

    @Override
    public CANFrameBuilder<CANFrame> frameBuilder() {
        return new SocketCANFrameBuilder();
    }

    @Override
    public void send(CANFrame frame, BiConsumer<RequestTransaction, SendRequestContext<CANFrame>> callback) {
        if (frame instanceof SocketCANDelegateFrame) {
            RequestTransactionManager.RequestTransaction transaction = tm.startRequest();

            ConversationContext.SendRequestContext<CANFrame> ctx = context.sendRequest(((SocketCANDelegateFrame) frame).getFrame())
                .expectResponse(SocketCANFrame.class, Duration.ofSeconds(10L))
//                .onError((response, error) -> {
//                    System.err.println("Unexpected frame " + response + " " + error);
//                })
                .unwrap(SocketCANDelegateFrame::new);
            //return CompletableFuture.completedFuture(new SocketCANTransactionContext<>(transaction, ctx));
            callback.accept(transaction, ctx);
            return;
        }
        throw new PlcRuntimeException("Unsupported frame type " + frame);
    }

}
