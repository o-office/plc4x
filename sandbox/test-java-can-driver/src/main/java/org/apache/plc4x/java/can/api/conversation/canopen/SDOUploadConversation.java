package org.apache.plc4x.java.can.api.conversation.canopen;

import org.apache.plc4x.java.api.exceptions.PlcException;
import org.apache.plc4x.java.api.value.PlcValue;
import org.apache.plc4x.java.can.api.segmentation.accumulator.ByteStorage;
import org.apache.plc4x.java.canopen.readwrite.*;
import org.apache.plc4x.java.canopen.readwrite.types.CANOpenDataType;
import org.apache.plc4x.java.spi.generation.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class SDOUploadConversation extends CANOpenConversationBase {

    private final Logger logger = LoggerFactory.getLogger(SDOUploadConversation.class);
    private final SDOConversation delegate;
    private final IndexAddress address;
    private final CANOpenDataType type;

    public SDOUploadConversation(SDOConversation delegate, IndexAddress address, CANOpenDataType type) {
        this.delegate = delegate;
        this.address = address;
        this.type = type;
    }

    public void execute(CompletableFuture<PlcValue> receiver) {
        SDOInitiateUploadRequest rq = new SDOInitiateUploadRequest(address);

        delegate.send(rq, (ctx) ->
            ctx.onError((response, error) -> {
                if (error != null) {
                    receiver.completeExceptionally(error);
                    return;
                }
                if (response.getResponse() instanceof SDOAbortResponse) {
                    SDOAbortResponse abort = (SDOAbortResponse) response.getResponse();
                    SDOAbort sdoAbort = abort.getAbort();
                    receiver.completeExceptionally(new PlcException("Could not read value. Remote party reported code " + sdoAbort.getCode()));
                }
            })
            .unwrap(CANOpenSDOResponse::getResponse)
            .only(SDOInitiateUploadResponse.class)
            .check(response -> response.getAddress().equals(address))
            .handle(response -> {
                handle(receiver, response);
            })
        );
    }

    private void handle(CompletableFuture<PlcValue> receiver, SDOInitiateUploadResponse answer) {
        BiConsumer<Integer, byte[]> valueCallback = (length, bytes) -> {
            try {
                final PlcValue decodedValue = decodeFrom(bytes, type, length);
                receiver.complete(decodedValue);
            } catch (ArrayIndexOutOfBoundsException | ParseException e) {
                receiver.completeExceptionally(e);
            }
        };

        if (answer.getExpedited() && answer.getIndicated() && answer.getPayload() instanceof SDOInitiateExpeditedUploadResponse) {
            SDOInitiateExpeditedUploadResponse payload = (SDOInitiateExpeditedUploadResponse) answer.getPayload();
            valueCallback.accept(payload.getData().length, payload.getData());
        } else if (answer.getPayload() instanceof SDOInitiateSegmentedUploadResponse) {
            logger.debug("Beginning of segmented operation for address {}/{}", Integer.toHexString(address.getIndex()), Integer.toHexString(address.getSubindex()));
            ByteStorage.SDOUploadStorage storage = new ByteStorage.SDOUploadStorage();
            storage.append(answer);

            SDOInitiateSegmentedUploadResponse segment = (SDOInitiateSegmentedUploadResponse) answer.getPayload();
            fetch(storage, valueCallback, receiver, false, Long.valueOf(segment.getBytes()).intValue());
        } else {
            receiver.completeExceptionally(new PlcException("Unsupported SDO operation kind."));
        }
    }

    private void fetch(ByteStorage.SDOUploadStorage storage, BiConsumer<Integer, byte[]> valueCallback, CompletableFuture<PlcValue> receiver, boolean toggle, int size) {
        logger.info("Request next data block for address {}/{}", Integer.toHexString(address.getIndex()), Integer.toHexString(address.getSubindex()));
        delegate.send(new SDOSegmentUploadRequest(toggle), (ctx) -> {
            ctx.unwrap(CANOpenSDOResponse::getResponse)
                .onError((response, error) -> {
                    if (error != null) {
                        receiver.completeExceptionally(error);
                        return;
                    }

                    if (response instanceof SDOAbortResponse) {
                        SDOAbortResponse abort = (SDOAbortResponse) response;
                        SDOAbort sdoAbort = abort.getAbort();
                        receiver.completeExceptionally(new PlcException("Could not read value. Remote party reported code " + sdoAbort.getCode()));
                    }
                })
                .only(SDOSegmentUploadResponse.class)
                .check(r -> r.getToggle() == toggle)
                .handle(response -> {
                    storage.append(response);

                    if (response.getLast()) {
                        // validate size
                        logger.trace("Completed reading of data from {}/{}, collected {}, wanted {}", Integer.toHexString(address.getIndex()), Integer.toHexString(address.getSubindex()), storage.size(), size);
                        valueCallback.accept(Long.valueOf(size).intValue(), storage.get());
                    } else {
                        logger.trace("Continue reading of data from {}/{}, collected {}, wanted {}", Integer.toHexString(address.getIndex()), Integer.toHexString(address.getSubindex()), storage.size(), size);
                        fetch(storage, valueCallback, receiver, !toggle, size);
                    }
                });
        });
    }


}
