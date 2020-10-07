/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */
package org.apache.plc4x.java.can;

import io.netty.buffer.ByteBuf;
import org.apache.plc4x.java.can.configuration.CANConfiguration;
import org.apache.plc4x.java.can.context.CANOpenDriverContext;
import org.apache.plc4x.java.can.field.CANFieldHandler;
import org.apache.plc4x.java.can.field.CANOpenFieldHandler;
import org.apache.plc4x.java.can.protocol.CANOpenProtocolLogic;
import org.apache.plc4x.java.socketcan.readwrite.SocketCANFrame;
import org.apache.plc4x.java.socketcan.readwrite.io.SocketCANFrameIO;
import org.apache.plc4x.java.spi.configuration.Configuration;
import org.apache.plc4x.java.spi.connection.GeneratedDriverBase;
import org.apache.plc4x.java.spi.connection.ProtocolStackConfigurer;
import org.apache.plc4x.java.spi.connection.SingleProtocolStackConfigurer;

import java.util.function.ToIntFunction;

/**
 */
public class CANOpenPlcDriver extends GeneratedDriverBase<SocketCANFrame> {

    @Override
    public String getProtocolCode() {
        return "canopen";
    }

    @Override
    public String getProtocolName() {
        return "CANopen";
    }

    @Override
    protected Class<? extends Configuration> getConfigurationType() {
        return CANConfiguration.class;
    }

    @Override
    protected boolean canRead() {
        return true;
    }

    @Override
    protected boolean canSubscribe() {
        return true;
    }

    @Override
    protected boolean canWrite() {
        return true;
    }

    @Override
    protected String getDefaultTransport() {
        return "javacan";
    }

    @Override
    protected CANOpenFieldHandler getFieldHandler() {
        return new CANOpenFieldHandler();
    }

    @Override
    protected ProtocolStackConfigurer<SocketCANFrame> getStackConfigurer() {
        return SingleProtocolStackConfigurer.builder(SocketCANFrame.class, SocketCANFrameIO.class)
            .withProtocol(CANOpenProtocolLogic.class)
            .withDriverContext(CANOpenDriverContext.class)
            .withPacketSizeEstimator(CANEstimator.class)
            .littleEndian()
            .build();
    }

    public static class CANEstimator implements ToIntFunction<ByteBuf> {
        @Override
        public int applyAsInt(ByteBuf byteBuf) {
            if (byteBuf.readableBytes() >= 5) {
                return 16; // socketcan transport always returns 16 bytes padded with zeros;
            }
            return -1; //discard
        }
    }

}
