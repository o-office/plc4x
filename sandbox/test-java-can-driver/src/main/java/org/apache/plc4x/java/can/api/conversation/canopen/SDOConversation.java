package org.apache.plc4x.java.can.api.conversation.canopen;

import org.apache.plc4x.java.api.value.PlcValue;
import org.apache.plc4x.java.can.canopen.CANOpenFrame;
import org.apache.plc4x.java.canopen.readwrite.CANOpenSDORequest;
import org.apache.plc4x.java.canopen.readwrite.CANOpenSDOResponse;
import org.apache.plc4x.java.canopen.readwrite.IndexAddress;
import org.apache.plc4x.java.canopen.readwrite.SDORequest;
import org.apache.plc4x.java.canopen.readwrite.types.CANOpenDataType;
import org.apache.plc4x.java.canopen.readwrite.types.CANOpenService;
import org.apache.plc4x.java.spi.ConversationContext.SendRequestContext;
import org.apache.plc4x.java.spi.transaction.RequestTransactionManager.RequestTransaction;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SDOConversation {

    private final CANOpenConversation delegate;

    public SDOConversation(CANOpenConversation delegate) {
        this.delegate = delegate;
    }

    public SDODownloadConversation download(IndexAddress indexAddress, PlcValue value, CANOpenDataType type) {
        return new SDODownloadConversation(this, indexAddress, value, type);
    }

    public SDOUploadConversation upload(IndexAddress indexAddress, CANOpenDataType type) {
        return new SDOUploadConversation(this, indexAddress, type);
    }

    public void send(SDORequest request, Consumer<SendRequestContext<CANOpenSDOResponse>> callback) {
        delegate.send(CANOpenService.RECEIVE_SDO, new CANOpenSDORequest(request.getCommand(), request), (ctx) -> {
            SendRequestContext<CANOpenSDOResponse> context = ctx
//            .onError((response, error) -> {
//                System.out.println("Unexpected frame " + response + " " + error);
//            })
            .only(CANOpenSDOResponse.class);
            callback.accept(context);
        });
    }

}
