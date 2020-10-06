package org.apache.plc4x.java.can.helper;

import org.apache.plc4x.java.socketcan.readwrite.SocketCANFrame;
import org.apache.plc4x.java.spi.generation.ParseException;
import org.apache.plc4x.java.spi.generation.WriteBuffer;

public class HeaderParser {

    public static final int EFF_FLAG = 0b10000000_00000000_00000000_00000000;
    public static final int RTR_FLAG = 0b01000000_00000000_00000000_00000000;
    public static final int ERR_FLAG = 0b00100000_00000000_00000000_00000000;
    public static final int SFF_MASK = 0b00000000_00000000_00000111_11111111;
    public static final int EFF_MASK = 0b00011111_11111111_11111111_11111111;
    public static final int ERR_MASK = EFF_MASK;

    public static final int EXTENDED_FRAME_FORMAT_FLAG = 0x80000000;

    public static final int REMOTE_TRANSMISSION_FLAG = 0x40000000;

    public static final int ERROR_FRAME_FLAG = 0x20000000;

    public static final int STANDARD_FORMAT_IDENTIFIER_MASK = 0x7ff;

    public static final int EXTENDED_FORMAT_IDENTIFIER_MASK = 0x1fffffff;

    public static int readIdentifier(int identifier) {
        if ((isExtended(identifier))) {
            return identifier & EXTENDED_FORMAT_IDENTIFIER_MASK;
        }
        return identifier & STANDARD_FORMAT_IDENTIFIER_MASK;
    }

    public static void writeIdentifier(WriteBuffer buffer, SocketCANFrame frame) throws ParseException {

    }

    public static boolean isExtended(int identifier) {
        return (identifier & EXTENDED_FRAME_FORMAT_FLAG) != 0;
    }

    public static boolean isRemote(int identifier) {
        return (identifier & REMOTE_TRANSMISSION_FLAG) != 0;
    }

    public static boolean isError(int identifier) {
        return (identifier & ERROR_FRAME_FLAG) != 0;
    }

}
