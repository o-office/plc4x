package org.apache.plc4x.java.can.protocol;

import org.apache.plc4x.java.can.field.CANOpenPDOField;
import org.apache.plc4x.java.spi.messages.PlcSubscriber;
import org.apache.plc4x.java.spi.model.DefaultPlcSubscriptionHandle;

public class CANOpenSubscriptionHandle extends DefaultPlcSubscriptionHandle {
    private final String name;
    private final CANOpenPDOField field;

    public CANOpenSubscriptionHandle(PlcSubscriber subscriber, String name, CANOpenPDOField field) {
        super(subscriber);
        this.name = name;
        this.field = field;
    }

    public boolean matches(int identifier) {
        return field.getNodeId() == 0 || field.getNodeId() == identifier;
    }

    public String getName() {
        return name;
    }

    public CANOpenPDOField getField() {
        return field;
    }
}
