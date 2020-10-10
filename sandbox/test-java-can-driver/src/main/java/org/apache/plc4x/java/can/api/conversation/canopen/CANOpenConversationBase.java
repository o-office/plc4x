package org.apache.plc4x.java.can.api.conversation.canopen;

import org.apache.plc4x.java.api.value.PlcValue;
import org.apache.plc4x.java.can.canopen.CANOpenFrame;
import org.apache.plc4x.java.canopen.readwrite.io.DataItemIO;
import org.apache.plc4x.java.canopen.readwrite.types.CANOpenDataType;
import org.apache.plc4x.java.spi.generation.ParseException;
import org.apache.plc4x.java.spi.generation.ReadBuffer;

public abstract class CANOpenConversationBase {

    protected PlcValue decodeFrom(byte[] data, CANOpenDataType type, int length) throws ParseException {
        return DataItemIO.staticParse(new ReadBuffer(data, true), type, length);
    }

}
