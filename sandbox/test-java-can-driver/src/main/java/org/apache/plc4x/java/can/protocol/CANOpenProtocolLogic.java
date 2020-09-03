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
import org.apache.plc4x.java.canopen.readwrite.CANOpenHeartbeatPayload;
import org.apache.plc4x.java.canopen.readwrite.CANOpenNetworkPayload;
import org.apache.plc4x.java.canopen.readwrite.CANOpenPayload;
import org.apache.plc4x.java.canopen.readwrite.io.CANOpenHeartbeatPayloadIO;
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
    private Timer heartbeat;

    @Override
    public void setConfiguration(CANConfiguration configuration) {
        this.configuration = configuration;
        // Set the transaction manager to allow only one message at a time.
        this.tm = new RequestTransactionManager(1);
    }

    @Override
    public void onConnect(ConversationContext<SocketCANFrame> context) {
        try {
            if (configuration.isHeartbeat()) {
                context.sendToWire(createFrame(new CANOpenHeartbeatPayload(NMTState.BOOTED_UP)));
                context.fireConnected();

                this.heartbeat = new Timer();
                this.heartbeat.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            context.sendToWire(createFrame(new CANOpenHeartbeatPayload(NMTState.OPERATIONAL)));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }, 5000, 5000);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private SocketCANFrame createFrame(CANOpenHeartbeatPayload state) throws ParseException {
        WriteBuffer buffer = new WriteBuffer(state.getLengthInBytes());
        CANOpenHeartbeatPayloadIO.staticSerialize(buffer, state);
        return new SocketCANFrame(cobId(CANOpenService.HEARTBEAT), buffer.getData());
    }

    @Override
    protected void decode(ConversationContext<SocketCANFrame> context, SocketCANFrame msg) throws Exception {
        logger.info("Decode CAN message {}", msg);

//        int identifier = msg.getIdentifier();
//        CANOpenService service = CANOpenService.valueOf((byte) (identifier >> 7));
//        if (service != null) {
//            ReadBuffer buffer = new ReadBuffer(msg.getData());
//            CANOpenPayload payload = CANOpenPayloadIO.staticParse(buffer, service);
//
//
//        }
    }

    @Override
    public void close(ConversationContext<SocketCANFrame> context) {

    }

    @Override
    public void onDisconnect(ConversationContext<SocketCANFrame> context) {

    }

    private int cobId(CANOpenService service) {
        // form 32 bit socketcan identifier
        return (configuration.getNodeId() << 24) & 0xff000000 |
            (service.getValue() << 16 ) & 0x00ff0000;
    }

    private CANOpenService serviceId(int nodeId) {
        // form 32 bit socketcan identifier
        return CANOpenService.valueOf((byte) (nodeId >> 7));
    }
}
