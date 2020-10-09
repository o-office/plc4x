package org.apache.plc4x.java.can.api.conversation.canopen;

import org.apache.plc4x.java.api.exceptions.PlcException;
import org.apache.plc4x.java.api.value.PlcValue;
import org.apache.plc4x.java.can.api.CANFrame;
import org.apache.plc4x.java.can.api.segmentation.accumulator.ByteStorage;
import org.apache.plc4x.java.canopen.readwrite.*;
import org.apache.plc4x.java.canopen.readwrite.types.CANOpenDataType;
import org.apache.plc4x.java.spi.generation.ParseException;
import org.apache.plc4x.java.spi.transaction.RequestTransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class SDOUploadConversation<W extends CANFrame> extends CANOpenConversationBase {

    private final Logger logger = LoggerFactory.getLogger(SDOUploadConversation.class);
    private final SDOConversation<W> delegate;
    private final IndexAddress address;
    private final CANOpenDataType type;

    public SDOUploadConversation(SDOConversation<W> delegate, IndexAddress address, CANOpenDataType type) {
        this.delegate = delegate;
        this.address = address;
        this.type = type;
    }

    public void execute(CompletableFuture<PlcValue> receiver) throws PlcException {
        SDOInitiateUploadRequest rq = new SDOInitiateUploadRequest(address);

        delegate.send(rq, (tx, ctx) ->
            ctx.onError((response, error) -> {
                System.err.println("Unexpected frame " + response + " " + error);
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
            .check(reply -> {
                logger.warn("Received answer {}", reply);
                return true;
            })
            .unwrap(CANOpenSDOResponse::getResponse).check(reply -> {
                logger.warn("Received answer {}", reply);
                return true;
            })
            .check(reply -> {
                logger.warn("Received answer {}", reply);
                return true;
            })
            .only(SDOInitiateUploadResponse.class)
            .check(resp -> {
                logger.warn("Checking if reply address {}/{} matches {}/{}: {}",
                    Integer.toHexString(resp.getAddress().getIndex()), Integer.toHexString(resp.getAddress().getSubindex()),
                    Integer.toHexString(address.getIndex()), Integer.toHexString(address.getSubindex()),
                    resp.getAddress().equals(address)
                );
                return resp.getAddress().equals(address);
            })
            .handle(response -> {
                handle(tx, receiver, response);
            })
        );
    }

    private void handle(RequestTransactionManager.RequestTransaction tx, CompletableFuture<PlcValue> receiver, SDOInitiateUploadResponse answer) {
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
            logger.info("Beginning of segmented operation for address {}/{}", Integer.toHexString(address.getIndex()), Integer.toHexString(address.getSubindex()));
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
        delegate.send(new SDOSegmentUploadRequest(toggle), (tx, ctx) -> {
            ctx.unwrap(CANOpenSDOResponse::getResponse)
                .onError((response, error) -> {
                    System.out.println("Unexpected frame " + response + " " + error);
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
//                    if (!reply.getToggle() == toggle) { // toggle flag is wrong, abort transaction
//                        logger.info("Received invalid answer from party for {}", address);
//                        delegate.send(new SDOAbortRequest(new SDOAbort(address, 0x100)), (tx2, ctx2) -> {});
//                        return;
//                    }

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
