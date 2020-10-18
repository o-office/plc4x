package org.apache.plc4x.java.can.protocol;

import org.apache.plc4x.java.can.field.CANOpenPDOField;
import org.apache.plc4x.java.canopen.readwrite.types.CANOpenService;
import org.apache.plc4x.java.spi.messages.PlcSubscriber;
import org.apache.plc4x.java.spi.model.DefaultPlcSubscriptionHandle;

public class CANOpenPDOSubscriptionHandle extends DefaultPlcSubscriptionHandle {
    private final String name;
    private final CANOpenPDOField field;

    public CANOpenPDOSubscriptionHandle(PlcSubscriber subscriber, String name, CANOpenPDOField field) {
        super(subscriber);
        this.name = name;
        this.field = field;
    }

    public boolean matches(CANOpenService service, int identifier) {
        if (field.getService() != service) {
            return false;
        }
        return field.getNodeId() == identifier;
    }

    public String getName() {
        return name;
    }

    public CANOpenPDOField getField() {
        return field;
    }

    public String toString() {
        return "CANOpenPDOSubscriptionHandle [service=" + field.getService() + ", node=" + intAndHex(field.getNodeId()) + ", cob=" + intAndHex(field.getService().getMin() + field.getNodeId()) + "]";
    }

    private static String intAndHex(int val) {
        return val + "(0x" + Integer.toHexString(val) + ")";
    }

}
