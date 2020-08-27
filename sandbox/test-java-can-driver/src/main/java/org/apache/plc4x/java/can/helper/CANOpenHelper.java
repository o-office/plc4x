package org.apache.plc4x.java.can.helper;

import org.apache.plc4x.java.canopen.readwrite.types.CANOpenService;
import org.apache.plc4x.java.socketcan.readwrite.SocketCANFrame;
import org.apache.plc4x.java.spi.generation.ParseException;
import org.apache.plc4x.java.spi.generation.WriteBuffer;

public class CANOpenHelper {

    public static CANOpenService readFunction(short identifier) {
        return CANOpenService.valueOf((byte) (identifier >> 7));
    }

    public static void writeFunction(WriteBuffer io, short identifier) {
        // NOOP - a placeholder to let mspec compile
    }

}
