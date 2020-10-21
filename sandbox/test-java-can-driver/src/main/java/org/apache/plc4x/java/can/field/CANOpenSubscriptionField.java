package org.apache.plc4x.java.can.field;

import org.apache.plc4x.java.canopen.readwrite.types.CANOpenService;

public interface CANOpenSubscriptionField {
    CANOpenService getService();

    boolean isWildcard();

    int getNodeId();
}
