package org.apache.plc4x.java.can.api.conversation.canopen;

import org.apache.plc4x.java.can.api.CANFrame;

import java.util.function.BiConsumer;

import org.apache.plc4x.java.spi.ConversationContext.SendRequestContext;
import org.apache.plc4x.java.spi.transaction.RequestTransactionManager.RequestTransaction;

public interface CANConversation<W extends CANFrame> {

    int getNodeId();

    CANFrameBuilder<W> frameBuilder();

    void send(RequestTransaction transaction, W frame, BiConsumer<RequestTransaction, SendRequestContext<W>> callback);


}

