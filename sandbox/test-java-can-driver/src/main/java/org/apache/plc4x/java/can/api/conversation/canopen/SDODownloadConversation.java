package org.apache.plc4x.java.can.api.conversation.canopen;

import org.apache.plc4x.java.api.exceptions.PlcException;
import org.apache.plc4x.java.api.exceptions.PlcRuntimeException;
import org.apache.plc4x.java.api.types.PlcResponseCode;
import org.apache.plc4x.java.api.value.PlcValue;
import org.apache.plc4x.java.can.api.CANFrame;
import org.apache.plc4x.java.can.api.segmentation.accumulator.ByteStorage;
import org.apache.plc4x.java.canopen.readwrite.*;
import org.apache.plc4x.java.canopen.readwrite.io.DataItemIO;
import org.apache.plc4x.java.canopen.readwrite.types.CANOpenDataType;
import org.apache.plc4x.java.canopen.readwrite.types.SDOResponseCommand;
import org.apache.plc4x.java.spi.generation.ParseException;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

public class SDODownloadConversation<W extends CANFrame> {

    private final SDOConversation<W> delegate;
    private final IndexAddress indexAddress;
    private final byte[] data;

    public SDODownloadConversation(SDOConversation<W> delegate, IndexAddress indexAddress, PlcValue value, CANOpenDataType type) {
        this.delegate = delegate;
        this.indexAddress = indexAddress;

        try {
            data = DataItemIO.staticSerialize(value, type,  null,true).getData();
        } catch (ParseException e) {
            throw new PlcRuntimeException("Could not serialize data", e);
        }
    }

    public void execute(BiConsumer<PlcResponseCode, Throwable> receiver) throws PlcException {
        if (data.length > 4) {
            // segmented

            SDOInitiateSegmentedUploadResponse size = new SDOInitiateSegmentedUploadResponse(data.length);
            delegate.send(new SDOInitiateDownloadRequest(false, true, indexAddress, size), (tx, ctx) -> {
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

        delegate.send(rq, (tx, ctx) ->
            ctx.onError((response, error) -> {
                System.out.println("Unexpected frame " + response + " " + error);
            })
            .unwrap(CANOpenSDOResponse::getResponse)
            .check(r -> r.getCommand() == SDOResponseCommand.INITIATE_DOWNLOAD)
            .handle(r -> {
                System.out.println(r);
            })
        );
    }

    private void put(byte[] data, BiConsumer<PlcResponseCode, Throwable> receiver, boolean toggle, int offset) {
        int remaining = data.length - offset;
        byte[] segment = new byte[Math.min(remaining, 7)];
        System.arraycopy(data, offset, segment, 0, segment.length);

        delegate.send(new SDOSegmentDownloadRequest(toggle, remaining <= 7, segment), (tx, ctx) -> {
            ctx.unwrap(CANOpenSDOResponse::getResponse)
                .only(SDOSegmentDownloadResponse.class)
                .onError((response, error) -> {
                    System.out.println("Unexpected frame " + response + " " + error);
                    receiver.accept(null, error);
                })
                .check(r -> r.getToggle() == toggle)
                .handle(reply -> {
                    if (offset + segment.length == data.length) {
                        // validate offset
                        receiver.accept(PlcResponseCode.OK, null);
                    } else {
                        put(data, receiver, !toggle, offset + segment.length);
                    }
                });
        });
    }
}
