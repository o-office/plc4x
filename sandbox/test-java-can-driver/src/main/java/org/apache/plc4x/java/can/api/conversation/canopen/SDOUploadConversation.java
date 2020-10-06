package org.apache.plc4x.java.can.api.conversation.canopen;

import org.apache.plc4x.java.api.exceptions.PlcException;
import org.apache.plc4x.java.api.value.PlcValue;
import org.apache.plc4x.java.can.api.CANFrame;
import org.apache.plc4x.java.can.api.segmentation.accumulator.ByteStorage;
import org.apache.plc4x.java.canopen.readwrite.*;
import org.apache.plc4x.java.canopen.readwrite.types.CANOpenDataType;
import org.apache.plc4x.java.spi.generation.ParseException;

import java.util.function.BiConsumer;

public class SDOUploadConversation<W extends CANFrame> extends CANOpenConversationBase {

    private final SDOConversation<W> delegate;
    private final IndexAddress address;
    private final CANOpenDataType type;

    public SDOUploadConversation(SDOConversation<W> delegate, IndexAddress address, CANOpenDataType type) {
        this.delegate = delegate;
        this.address = address;
        this.type = type;
    }

    public void execute(BiConsumer<PlcValue, Throwable> receiver) throws PlcException {
        SDOInitiateUploadRequest rq = new SDOInitiateUploadRequest(address);

        delegate.send(rq, (tx, ctx) ->
            ctx
//            .onError((response, error) -> {
//                System.err.println("Unexpected frame " + response + " " + error);
//                receiver.accept(null, error);
//            })
            .unwrap(CANOpenSDOResponse::getResponse)
            .onError(((response, error) -> {
                if (response instanceof SDOAbortResponse) {
                    SDOAbortResponse abort = (SDOAbortResponse) response;
                    SDOAbort sdoAbort = abort.getAbort();
                    receiver.accept(null, new PlcException("Could not read value. Remote party reported code " + sdoAbort.getCode()));
                } else {
                    receiver.accept(null, error);
                }
            }))
            .only(SDOInitiateUploadResponse.class)
            .check(r -> r.getAddress().equals(address))
            .handle(response -> {
                handle(receiver, response);
            })
        );
    }

    private void handle(BiConsumer<PlcValue, Throwable> receiver, SDOInitiateUploadResponse answer) {
        BiConsumer<Integer, byte[]> valueCallback = (length, bytes) -> {
            try {
                receiver.accept(decodeFrom(bytes, type, length), null);
            } catch (ParseException e) {
                receiver.accept(null, e);
            }
        };

        if (answer.getExpedited() && answer.getIndicated() && answer.getPayload() instanceof SDOInitiateExpeditedUploadResponse) {
            SDOInitiateExpeditedUploadResponse payload = (SDOInitiateExpeditedUploadResponse) answer.getPayload();
            valueCallback.accept(payload.getData().length, payload.getData());
        } else if (answer.getPayload() instanceof SDOInitiateSegmentedUploadResponse) {
            ByteStorage.SDOUploadStorage storage = new ByteStorage.SDOUploadStorage();
            storage.append(answer);

            SDOInitiateSegmentedUploadResponse segment = (SDOInitiateSegmentedUploadResponse) answer.getPayload();
            fetch(storage, valueCallback, receiver, false, Long.valueOf(segment.getBytes()).intValue());
        } else {
            receiver.accept(null, new PlcException("Unsupported SDO operation kind."));
        }
    }

    private void fetch(ByteStorage.SDOUploadStorage storage, BiConsumer<Integer, byte[]> valueCallback, BiConsumer<PlcValue, Throwable> receiver, boolean toggle, int size) {
        delegate.send(new SDOSegmentUploadRequest(toggle), (tx, ctx) -> {
            ctx.unwrap(CANOpenSDOResponse::getResponse)
                .only(SDOSegmentUploadResponse.class)
                .onError((response, error) -> {
                    System.out.println("Unexpected frame " + response + " " + error);
                    receiver.accept(null, error);
                })
                .check(r -> r.getToggle() == toggle)
                .handle(reply -> {
                    storage.append(reply);

                    if (reply.getLast()) {
                        // validate size
                        valueCallback.accept(Long.valueOf(size).intValue(), storage.get());
                    } else {
                        fetch(storage, valueCallback, receiver, !toggle, size);
                    }
                });
        });
    }


}
