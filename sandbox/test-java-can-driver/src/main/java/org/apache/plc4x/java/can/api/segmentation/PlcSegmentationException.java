package org.apache.plc4x.java.can.api.segmentation;

import org.apache.plc4x.java.api.exceptions.PlcException;

public class PlcSegmentationException extends PlcException {
    public PlcSegmentationException(String message) {
        super(message);
    }

    public PlcSegmentationException(String message, Throwable cause) {
        super(message, cause);
    }

    public PlcSegmentationException(Throwable cause) {
        super(cause);
    }

    public PlcSegmentationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
