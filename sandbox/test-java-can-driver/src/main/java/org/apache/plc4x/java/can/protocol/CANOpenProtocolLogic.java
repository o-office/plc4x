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
package org.apache.plc4x.java.can.protocol;

import org.apache.plc4x.java.can.configuration.CANConfiguration;
import org.apache.plc4x.java.canopen.readwrite.CANOpenNetworkPayload;
import org.apache.plc4x.java.canopen.readwrite.CANOpenPayload;
import org.apache.plc4x.java.canopen.readwrite.io.CANOpenNetworkPayloadIO;
import org.apache.plc4x.java.canopen.readwrite.io.CANOpenPayloadIO;
import org.apache.plc4x.java.canopen.readwrite.types.CANOpenService;
import org.apache.plc4x.java.canopen.readwrite.types.NMTState;
import org.apache.plc4x.java.socketcan.readwrite.SocketCANFrame;
import org.apache.plc4x.java.spi.ConversationContext;
import org.apache.plc4x.java.spi.Plc4xProtocolBase;
import org.apache.plc4x.java.spi.configuration.HasConfiguration;
import org.apache.plc4x.java.spi.generation.ParseException;
import org.apache.plc4x.java.spi.generation.ReadBuffer;
import org.apache.plc4x.java.spi.generation.WriteBuffer;
import org.apache.plc4x.java.spi.transaction.RequestTransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;

public class CANOpenProtocolLogic extends Plc4xProtocolBase<SocketCANFrame> implements HasConfiguration<CANConfiguration> {

    private Logger logger = LoggerFactory.getLogger(CANOpenProtocolLogic.class);

    private CANConfiguration configuration;
    private RequestTransactionManager tm;

    @Override
    public void setConfiguration(CANConfiguration configuration) {
        this.configuration = configuration;
        // Set the transaction manager to allow only one message at a time.
        this.tm = new RequestTransactionManager(1);
    }

    @Override
    public void onConnect(ConversationContext<SocketCANFrame> context) {
        CANOpenNetworkPayload state = new CANOpenNetworkPayload(NMTState.BOOTED_UP);
        WriteBuffer buffer = new WriteBuffer(1);
        try {
            CANOpenNetworkPayloadIO.staticSerialize(buffer, state);
            context.sendToWire(new SocketCANFrame(cobId(CANOpenService.NMT), buffer.getData()));
            context.fireConnected();

            Timer heartbeat = new Timer();
            heartbeat.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    CANOpenNetworkPayload state = new CANOpenNetworkPayload(NMTState.OPERATIONAL);
                    WriteBuffer buffer = new WriteBuffer(1);
                    context.sendToWire(new SocketCANFrame(cobId(CANOpenService.NMT), buffer.getData()));
                }
            }, 5000, 5000);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void decode(ConversationContext<SocketCANFrame> context, SocketCANFrame msg) throws Exception {
        logger.info("Decode CAN message {}", msg);

        int identifier = msg.getIdentifier();
        CANOpenService service = CANOpenService.valueOf((byte) (identifier >> 7));
        if (service != null) {
            ReadBuffer buffer = new ReadBuffer(msg.getData());
            CANOpenPayload payload = CANOpenPayloadIO.staticParse(buffer, service);


        }
    }

    @Override
    public void close(ConversationContext<SocketCANFrame> context) {

    }

    @Override
    public void onDisconnect(ConversationContext<SocketCANFrame> context) {

    }

    private int cobId(CANOpenService service) {
        return service.getValue() & configuration.getNodeId();
    }

}
