package org.apache.plc4x.java.can.field;

import org.apache.plc4x.java.api.exceptions.PlcInvalidFieldException;
import org.apache.plc4x.java.canopen.readwrite.types.CANOpenDataType;
import org.apache.plc4x.java.canopen.readwrite.types.CANOpenService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CANOpenPDOFieldTest {

    @Test
    public void testNodeSyntax() {
        final CANOpenPDOField canField = CANOpenPDOField.of("RECEIVE_PDO_2:20:RECORD");

        assertEquals(20, canField.getNodeId());
        assertEquals(CANOpenService.RECEIVE_PDO_2, canField.getService());
        assertEquals(CANOpenDataType.RECORD, canField.getCanOpenDataType());
    }

    @Test
    public void testInvalidSyntax() {
        assertThrows(PlcInvalidFieldException.class, () -> CANOpenPDOField.of("PDO:"));
    }
}