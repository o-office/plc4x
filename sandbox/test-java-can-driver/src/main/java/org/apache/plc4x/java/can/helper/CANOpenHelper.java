package org.apache.plc4x.java.can.helper;

import org.apache.plc4x.java.canopen.readwrite.SDOInitiateExpeditedUploadResponse;
import org.apache.plc4x.java.canopen.readwrite.SDOInitiateUploadResponsePayload;
import org.apache.plc4x.java.canopen.readwrite.SDOSegmentUploadResponse;
import org.apache.plc4x.java.canopen.readwrite.types.CANOpenService;
import org.apache.plc4x.java.spi.generation.WriteBuffer;

import static org.apache.plc4x.java.spi.generation.StaticHelper.COUNT;

public class CANOpenHelper {

    public static CANOpenService readFunction(short identifier) {
        return CANOpenService.valueOf((byte) (identifier >> 7));
    }

    public static int uploadPadding(SDOSegmentUploadResponse payload) {
        return 7 - payload.getData().length;
    }

    public static int count(boolean expedited, boolean indicated, SDOInitiateUploadResponsePayload payload) {
        return expedited && indicated && payload instanceof SDOInitiateExpeditedUploadResponse ? 4 - COUNT(((SDOInitiateExpeditedUploadResponse) payload).getData()) : 0;
    }

    public static void writeFunction(WriteBuffer io, short identifier) {
        // NOOP - a placeholder to let mspec compile
    }

}
