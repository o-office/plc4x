package org.apache.plc4x.java.can.field;

import org.apache.plc4x.java.api.exceptions.PlcInvalidFieldException;
import org.apache.plc4x.java.canopen.readwrite.types.CANOpenDataType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CANOpenSDOFieldTest {

    @Test
    public void testNodeSyntax() {
        final CANOpenSDOField canField = CANOpenSDOField.of("SDO:20:0x10/0xAA:RECORD");

        assertEquals(20, canField.getNodeId());
        assertEquals(0x10, canField.getIndex());
        assertEquals(0xAA, canField.getSubIndex());
        assertEquals(CANOpenDataType.RECORD, canField.getCanOpenDataType());
    }

    @Test
    public void testInvalidSyntax() {
        assertThrows(PlcInvalidFieldException.class, () -> CANOpenSDOField.of("SDO:"));
    }

}