package org.apache.plc4x.java.can.protocol;

import org.apache.plc4x.java.can.field.CANOpenPDOField;
import org.apache.plc4x.java.can.field.CANOpenSubscriptionField;
import org.apache.plc4x.java.canopen.readwrite.types.CANOpenService;
import org.apache.plc4x.java.spi.messages.PlcSubscriber;
import org.apache.plc4x.java.spi.model.DefaultPlcSubscriptionHandle;

public class CANOpenSubscriptionHandle extends DefaultPlcSubscriptionHandle {
    private final String name;
    private final CANOpenSubscriptionField field;

    public CANOpenSubscriptionHandle(PlcSubscriber subscriber, String name, CANOpenSubscriptionField field) {
        super(subscriber);
        this.name = name;
        this.field = field;
    }

    public boolean matches(CANOpenService service, int identifier) {
        if (field.getService() != service) {
            return false;
        }
        return field.isWildcard() || field.getNodeId() == identifier;
    }

    public String getName() {
        return name;
    }

    public CANOpenSubscriptionField getField() {
        return field;
    }

    public String toString() {
        return "CANopenSubscriptionHandle [service=" + field.getService() + ", node=" + intAndHex(field.getNodeId()) + ", cob=" + intAndHex(field.getService().getMin() + field.getNodeId()) + "]";
    }

    private static String intAndHex(int val) {
        return val + "(0x" + Integer.toHexString(val) + ")";
    }

}
