package org.apache.plc4x.java.can.api.conversation.canopen;

import org.apache.plc4x.java.api.value.PlcValue;
import org.apache.plc4x.java.can.api.CANFrame;
import org.apache.plc4x.java.canopen.readwrite.CANOpenSDORequest;
import org.apache.plc4x.java.canopen.readwrite.CANOpenSDOResponse;
import org.apache.plc4x.java.canopen.readwrite.IndexAddress;
import org.apache.plc4x.java.canopen.readwrite.SDORequest;
import org.apache.plc4x.java.canopen.readwrite.types.CANOpenDataType;
import org.apache.plc4x.java.canopen.readwrite.types.CANOpenService;
import org.apache.plc4x.java.spi.ConversationContext.SendRequestContext;
import org.apache.plc4x.java.spi.transaction.RequestTransactionManager.RequestTransaction;

import java.util.function.BiConsumer;

public class SDOConversation<W extends CANFrame> {

    private final CANOpenConversation<W> delegate;

    public SDOConversation(CANOpenConversation<W> delegate) {
        this.delegate = delegate;
    }

    public SDODownloadConversation<W> download(IndexAddress indexAddress, PlcValue value, CANOpenDataType type) {
        return new SDODownloadConversation<>(this, indexAddress, value, type);
    }

    public SDOUploadConversation<W> upload(IndexAddress indexAddress, CANOpenDataType type) {
        return new SDOUploadConversation<>(this, indexAddress, type);
    }

    public void send(SDORequest request, BiConsumer<RequestTransaction, SendRequestContext<CANOpenSDOResponse>> callback) {
        delegate.send(CANOpenService.RECEIVE_SDO, new CANOpenSDORequest(request.getCommand(), request), (tx, ctx) -> {
            SendRequestContext<CANOpenSDOResponse> context = ctx
//            .onError((response, error) -> {
//                System.out.println("Unexpected frame " + response + " " + error);
//            })
            .only(CANOpenSDOResponse.class);
            callback.accept(tx, context);
        });
    }

}
