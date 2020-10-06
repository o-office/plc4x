package org.apache.plc4x.java.can.protocol.segmentation;

import org.apache.plc4x.java.can.api.segmentation.Segmentation;
import org.apache.plc4x.java.can.api.segmentation.accumulator.Storage;
import org.apache.plc4x.java.canopen.readwrite.CANOpenPayload;
import org.apache.plc4x.java.canopen.readwrite.io.CANOpenPayloadIO;
import org.apache.plc4x.java.canopen.readwrite.types.CANOpenService;
import org.apache.plc4x.java.socketcan.readwrite.SocketCANFrame;
import org.apache.plc4x.java.spi.generation.ParseException;
import org.apache.plc4x.java.spi.generation.ReadBuffer;
import org.apache.plc4x.java.spi.generation.WriteBuffer;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A basic utility to execute segmented operations.
 */
public class CANOpenSegmentation<R> extends Segmentation<SocketCANFrame, CANOpenPayload, R> {

    private final CANOpenService service;
    private final int node;

    public CANOpenSegmentation(CANOpenService service, int node, Storage<CANOpenPayload, R> storage) {
        super(SocketCANFrame.class, Duration.ofSeconds(10L), storage);

        this.service = service;
        this.node = node;
    }

    public CANOpenSegmentation<R> begin(Supplier<CANOpenPayload> request, Predicate<CANOpenPayload> requestAnswer) {
        super.begin(request, requestAnswer);
        return this;
    }

    public CANOpenSegmentation<R> step(Function<CANOpenPayload, CANOpenPayload> step, Predicate<CANOpenPayload> stepAnswer) {
        super.step(step, stepAnswer);
        return this;
    }

    public CANOpenSegmentation<R> end(Predicate<CANOpenPayload> finalStep, Consumer<List<CANOpenPayload>> callback) {
        super.end(finalStep, callback);
        return this;
    }

    protected CANOpenPayload unwrap(SocketCANFrame frame) {
        return unsecure(() -> CANOpenPayloadIO.staticParse(new ReadBuffer(frame.getData()), serviceId(frame.getIdentifier())));
    }

    private <T> T unsecure(Callable<T> statement) {
        try {
            return statement.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected SocketCANFrame wrap(CANOpenPayload payload) {
        try {
            WriteBuffer io = new WriteBuffer(payload.getLengthInBytes(), true);
            payload.getMessageIO().serialize(io, payload);
            return new SocketCANFrame(service.getMin() + node, io.getData());
        } catch (ParseException e) {
            throw new RuntimeException("Could not construct segmented frame", e);
        }
    }

    private CANOpenService serviceId(int cobId) {
        // form 32 bit socketcan identifier
        CANOpenService service = CANOpenService.valueOf((byte) (cobId >> 7));
        if (service == null) {
            for (CANOpenService val : CANOpenService.values()) {
                if (val.getMin() > cobId && val.getMax() < cobId) {
                    return val;
                }
            }
        }
        return service;
    }

}
