package org.apache.plc4x.java.can.api.conversation.canopen;

import org.apache.plc4x.java.api.exceptions.PlcException;
import org.apache.plc4x.java.api.exceptions.PlcRuntimeException;
import org.apache.plc4x.java.api.types.PlcResponseCode;
import org.apache.plc4x.java.api.value.PlcValue;
import org.apache.plc4x.java.canopen.readwrite.*;
import org.apache.plc4x.java.canopen.readwrite.io.DataItemIO;
import org.apache.plc4x.java.canopen.readwrite.types.CANOpenDataType;
import org.apache.plc4x.java.canopen.readwrite.types.SDOResponseCommand;
import org.apache.plc4x.java.spi.generation.ParseException;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class SDODownloadConversation extends CANOpenConversationBase {

    private final SDOConversation delegate;
    private final IndexAddress indexAddress;
    private final byte[] data;

    public SDODownloadConversation(SDOConversation delegate, IndexAddress indexAddress, PlcValue value, CANOpenDataType type) {
        this.delegate = delegate;
        this.indexAddress = indexAddress;

        try {
            data = DataItemIO.staticSerialize(value, type,  null,true).getData();
        } catch (ParseException e) {
            throw new PlcRuntimeException("Could not serialize data", e);
        }
    }

    public void execute(CompletableFuture<PlcResponseCode> receiver) {
        if (data.length > 4) {
            // segmented

            SDOInitiateSegmentedUploadResponse size = new SDOInitiateSegmentedUploadResponse(data.length);
            delegate.send(new SDOInitiateDownloadRequest(false, true, indexAddress, size), (ctx) -> {
                ctx.unwrap(CANOpenSDOResponse::getResponse)
                    .check(p -> p.getCommand() == SDOResponseCommand.INITIATE_DOWNLOAD)
                    .only(SDOInitiateDownloadResponse.class)
                    .check(p -> indexAddress.equals(p.getAddress()))
                    .handle(x -> {
                        put(data, receiver, false, 0);
                    });
            });

            return;
        }

        // expedited
        SDOInitiateDownloadRequest rq = new SDOInitiateDownloadRequest(
            true, true,
            indexAddress,
            new SDOInitiateExpeditedUploadResponse(data)
        );

        delegate.send(rq, (ctx) ->
            ctx.onError((response, error) -> {
                System.out.println("Unexpected frame " + response + " " + error);
            })
            .unwrap(CANOpenSDOResponse::getResponse)
            .only(SDOInitiateDownloadResponse.class)
            .check(r -> r.getCommand() == SDOResponseCommand.INITIATE_DOWNLOAD)
            .handle(r -> {
                System.out.println(r);
            })
        );
    }

    private void put(byte[] data, CompletableFuture<PlcResponseCode> receiver, boolean toggle, int offset) {
        int remaining = data.length - offset;
        byte[] segment = new byte[Math.min(remaining, 7)];
        System.arraycopy(data, offset, segment, 0, segment.length);

        delegate.send(new SDOSegmentDownloadRequest(toggle, remaining <= 7, segment), (ctx) -> {
            ctx.unwrap(CANOpenSDOResponse::getResponse)
                .only(SDOSegmentDownloadResponse.class)
                .onError((response, error) -> {
                    if (error != null) {
                        receiver.completeExceptionally(error);
                    } else {
                        receiver.completeExceptionally(new PlcException("Transaction terminated"));
                    }
                })
                .check(response -> response.getToggle() == toggle)
                .handle(reply -> {
                    if (offset + segment.length == data.length) {
                        receiver.complete(PlcResponseCode.OK);
                    } else {
                        put(data, receiver, !toggle, offset + segment.length);
                    }
                });
        });
    }
}
