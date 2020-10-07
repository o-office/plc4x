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

import org.apache.plc4x.java.api.messages.*;
import org.apache.plc4x.java.api.model.PlcConsumerRegistration;
import org.apache.plc4x.java.api.model.PlcField;
import org.apache.plc4x.java.api.model.PlcSubscriptionHandle;
import org.apache.plc4x.java.api.types.PlcResponseCode;
import org.apache.plc4x.java.api.types.PlcSubscriptionType;
import org.apache.plc4x.java.api.value.PlcList;
import org.apache.plc4x.java.api.value.PlcNull;
import org.apache.plc4x.java.api.value.PlcValue;
import org.apache.plc4x.java.can.api.CANFrame;
import org.apache.plc4x.java.can.api.conversation.canopen.CANConversation;
import org.apache.plc4x.java.can.api.conversation.canopen.CANOpenConversation;
import org.apache.plc4x.java.can.api.conversation.canopen.SDODownloadConversation;
import org.apache.plc4x.java.can.api.conversation.canopen.SDOUploadConversation;
import org.apache.plc4x.java.can.configuration.CANConfiguration;
import org.apache.plc4x.java.can.context.CANOpenDriverContext;
import org.apache.plc4x.java.can.field.CANOpenField;
import org.apache.plc4x.java.can.field.CANOpenPDOField;
import org.apache.plc4x.java.can.field.CANOpenSDOField;
import org.apache.plc4x.java.can.socketcan.SocketCANConversation;
import org.apache.plc4x.java.canopen.readwrite.*;
import org.apache.plc4x.java.canopen.readwrite.io.CANOpenHeartbeatPayloadIO;
import org.apache.plc4x.java.canopen.readwrite.io.CANOpenPayloadIO;
import org.apache.plc4x.java.canopen.readwrite.io.DataItemIO;
import org.apache.plc4x.java.canopen.readwrite.types.CANOpenService;
import org.apache.plc4x.java.canopen.readwrite.types.NMTState;
import org.apache.plc4x.java.socketcan.readwrite.SocketCANFrame;
import org.apache.plc4x.java.spi.ConversationContext;
import org.apache.plc4x.java.spi.Plc4xProtocolBase;
import org.apache.plc4x.java.spi.configuration.HasConfiguration;
import org.apache.plc4x.java.spi.context.DriverContext;
import org.apache.plc4x.java.spi.generation.ParseException;
import org.apache.plc4x.java.spi.generation.ReadBuffer;
import org.apache.plc4x.java.spi.generation.WriteBuffer;
import org.apache.plc4x.java.spi.messages.*;
import org.apache.plc4x.java.spi.messages.utils.ResponseItem;
import org.apache.plc4x.java.spi.model.DefaultPlcConsumerRegistration;
import org.apache.plc4x.java.spi.model.InternalPlcSubscriptionHandle;
import org.apache.plc4x.java.spi.model.SubscriptionPlcField;
import org.apache.plc4x.java.spi.transaction.RequestTransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class CANOpenProtocolLogic extends Plc4xProtocolBase<SocketCANFrame> implements HasConfiguration<CANConfiguration>, PlcSubscriber {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10L);
    private Logger logger = LoggerFactory.getLogger(CANOpenProtocolLogic.class);

    private CANConfiguration configuration;
    private RequestTransactionManager tm;
    private Timer heartbeat;
    private CANOpenDriverContext canContext;
    private CANConversation<CANFrame> conversation;

    private Map<DefaultPlcConsumerRegistration, Consumer<PlcSubscriptionEvent>> consumers = new ConcurrentHashMap<>();

    @Override
    public void setConfiguration(CANConfiguration configuration) {
        this.configuration = configuration;
        // Set the transaction manager to allow only one message at a time.
        this.tm = new RequestTransactionManager(1);
    }

    @Override
    public void setDriverContext(DriverContext driverContext) {
        super.setDriverContext(driverContext);
        this.canContext = (CANOpenDriverContext) driverContext;

        // Initialize Transaction Manager.
        // Until the number of concurrent requests is successfully negotiated we set it to a
        // maximum of only one request being able to be sent at a time. During the login process
        // No concurrent requests can be sent anyway. It will be updated when receiving the
        // S7ParameterSetupCommunication response.
        this.tm = new RequestTransactionManager(1);
    }

    @Override
    public void onConnect(ConversationContext<SocketCANFrame> context) {
        try {
            if (configuration.isHeartbeat()) {
                context.sendToWire(createFrame(new CANOpenHeartbeatPayload(NMTState.BOOTED_UP)));

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
            context.fireConnected();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setContext(ConversationContext<SocketCANFrame> context) {
        super.setContext(context);
        this.conversation = new SocketCANConversation(tm, context);
    }

    private SocketCANFrame createFrame(CANOpenHeartbeatPayload state) throws ParseException {
        WriteBuffer buffer = new WriteBuffer(state.getLengthInBytes(), true);
        CANOpenHeartbeatPayloadIO.staticSerialize(buffer, state);
        return new SocketCANFrame(cobId(configuration.getNodeId(), CANOpenService.HEARTBEAT), buffer.getData());
    }

    public CompletableFuture<PlcWriteResponse> write(PlcWriteRequest writeRequest) {
        CompletableFuture<PlcWriteResponse> response = new CompletableFuture<>();
        if (writeRequest.getFieldNames().size() != 1) {
            response.completeExceptionally(new IllegalArgumentException("You can write only one field at the time"));
            return response;
        }

        PlcField field = writeRequest.getFields().get(0);
        if (!(field instanceof CANOpenField)) {
            response.completeExceptionally(new IllegalArgumentException("Only CANOpenField instances are supported"));
            return response;
        }

        if (field instanceof CANOpenSDOField) {
            writeInternally((InternalPlcWriteRequest) writeRequest, (CANOpenSDOField) field, response);
            return response;
        }
        if (field instanceof CANOpenPDOField) {
            writeInternally((InternalPlcWriteRequest) writeRequest, (CANOpenPDOField) field, response);
            return response;
        }

        response.completeExceptionally(new IllegalArgumentException("Only CANOpenSDOField instances are supported"));
        return response;
    }

    private void writeInternally(InternalPlcWriteRequest writeRequest, CANOpenSDOField field, CompletableFuture<PlcWriteResponse> response) {
        CANOpenConversation<CANFrame> canopen = new CANOpenConversation<>(field.getNodeId(), conversation);

        PlcValue writeValue = writeRequest.getPlcValues().get(0);

        SDODownloadConversation<CANFrame> download = canopen.sdo().download(new IndexAddress(field.getIndex(), field.getSubIndex()), writeValue, field.getCanOpenDataType());
        try {
            download.execute((value, error) -> {
                String fieldName = writeRequest.getFieldNames().iterator().next();
                response.complete(new DefaultPlcWriteResponse(writeRequest, Collections.singletonMap(fieldName, PlcResponseCode.OK)));
            });
        } catch (Exception e) {
            response.completeExceptionally(e);
        }
    }

    private void writeInternally(InternalPlcWriteRequest writeRequest, CANOpenPDOField field, CompletableFuture<PlcWriteResponse> response) {
        PlcValue writeValue = writeRequest.getPlcValues().get(0);

        try {
            String fieldName = writeRequest.getFieldNames().iterator().next();
            //
            WriteBuffer buffer = DataItemIO.staticSerialize(writeValue, field.getCanOpenDataType(), writeValue.getLength() / 8, true);
            if (buffer != null) {
                context.sendToWire(new SocketCANFrame(field.getNodeId(), buffer.getData()));
                response.complete(new DefaultPlcWriteResponse(writeRequest, Collections.singletonMap(fieldName, PlcResponseCode.OK)));
            } else {
                response.complete(new DefaultPlcWriteResponse(writeRequest, Collections.singletonMap(fieldName, PlcResponseCode.INVALID_DATA)));
            }
        } catch (Exception e) {
            response.completeExceptionally(e);
        }
    }

    public CompletableFuture<PlcReadResponse> read(PlcReadRequest readRequest) {
        CompletableFuture<PlcReadResponse> response = new CompletableFuture<>();
        if (readRequest.getFieldNames().size() != 1) {
            response.completeExceptionally(new IllegalArgumentException("SDO requires single field to be read"));
            return response;
        }

        PlcField field = readRequest.getFields().get(0);
        if (!(field instanceof CANOpenField)) {
            response.completeExceptionally(new IllegalArgumentException("Only CANOpenField instances are supported"));
            return response;
        }

        if (!(field instanceof CANOpenSDOField)) {
            response.completeExceptionally(new IllegalArgumentException("Only CANOpenSDOField instances are supported"));
            return response;
        };

        readInternally((InternalPlcReadRequest) readRequest, (CANOpenSDOField) field, response);
        return response;
    }

    @Override
    public CompletableFuture<PlcSubscriptionResponse> subscribe(PlcSubscriptionRequest request) {
        InternalPlcSubscriptionRequest rq = (InternalPlcSubscriptionRequest) request;

        List<SubscriptionPlcField> fields = rq.getSubscriptionFields();

        Map<String, ResponseItem<PlcSubscriptionHandle>> answers = new LinkedHashMap<>();
        DefaultPlcSubscriptionResponse response = new DefaultPlcSubscriptionResponse(rq, answers);

        for (Map.Entry<String, SubscriptionPlcField> entry : rq.getSubscriptionPlcFieldMap().entrySet()) {
            SubscriptionPlcField subscription = entry.getValue();
            if (subscription.getPlcSubscriptionType() != PlcSubscriptionType.EVENT) {
                answers.put(entry.getKey(), new ResponseItem<>(PlcResponseCode.UNSUPPORTED, null));
            } else if (!(subscription.getPlcField() instanceof CANOpenPDOField)) {
                answers.put(entry.getKey(), new ResponseItem<>(PlcResponseCode.INVALID_ADDRESS, null));
            } else {
                answers.put(entry.getKey(), new ResponseItem<>(PlcResponseCode.OK,
                    new CANOpenSubscriptionHandle(this, entry.getKey(), (CANOpenPDOField) subscription.getPlcField())
                ));
            }
        }

        return CompletableFuture.completedFuture(response);
    }

    private void readInternally(InternalPlcReadRequest readRequest, CANOpenSDOField field, CompletableFuture<PlcReadResponse> response) {
        CANOpenConversation<CANFrame> canopen = new CANOpenConversation<>(field.getNodeId(), conversation);

        SDOUploadConversation<CANFrame> upload = canopen.sdo().upload(new IndexAddress(field.getIndex(), field.getSubIndex()), field.getCanOpenDataType());
        try {
            upload.execute((value, error) -> {
                String fieldName = readRequest.getFieldNames().iterator().next();
                Map<String, ResponseItem<PlcValue>> fields = new HashMap<>();
                fields.put(fieldName, new ResponseItem<>(PlcResponseCode.OK, value));
                response.complete(new DefaultPlcReadResponse(readRequest, fields));
            });
        } catch (Exception e) {
            response.completeExceptionally(e);
        }
    }

    @Override
    protected void decode(ConversationContext<SocketCANFrame> context, SocketCANFrame msg) throws Exception {
        CANOpenService service = serviceId(msg.getIdentifier());
        CANOpenPayload payload = CANOpenPayloadIO.staticParse(new ReadBuffer(msg.getData()), service);

        CANOpenDriverContext.CALLBACK.receive(msg);

        if (service != null) {
            logger.info("Decoded CANOpen {} from {}, message {}", service, Math.abs(service.getMin() - msg.getIdentifier()), payload);

            if (service.getPdo() && payload instanceof CANOpenPDOPayload) {
                logger.info("Broadcasting PDO to subscribers");
                publishEvent(msg.getIdentifier(), (CANOpenPDOPayload) payload);
            }

        } else {
            logger.info("CAN message {}, {}", msg.getIdentifier(), msg);
        }

//        int identifier = msg.getIdentifier();
//        CANOpenService service = CANOpenService.valueOf((byte) (identifier >> 7));
//        if (service != null) {
//            ReadBuffer buffer = new ReadBuffer(msg.getData());
//            CANOpenPayload payload = CANOpenPayloadIO.staticParse(buffer, service);
//
//
//        }
    }

    private void publishEvent(int nodeId, CANOpenPDOPayload payload) {
        for (Map.Entry<DefaultPlcConsumerRegistration, Consumer<PlcSubscriptionEvent>> entry : consumers.entrySet()) {
            DefaultPlcConsumerRegistration registration = entry.getKey();
            Consumer<PlcSubscriptionEvent> consumer = entry.getValue();

            for (InternalPlcSubscriptionHandle handler : registration.getAssociatedHandles()) {
                if (handler instanceof CANOpenSubscriptionHandle) {
                    CANOpenSubscriptionHandle handle = (CANOpenSubscriptionHandle) handler;

                    if (handle.matches(nodeId)) {
                        CANOpenPDOField field = handle.getField();
                        byte[] data = payload.getPdo().getData();
                        try {
                            PlcValue value = DataItemIO.staticParse(new ReadBuffer(data, true), field.getCanOpenDataType(), data.length);
                            DefaultPlcSubscriptionEvent event = new DefaultPlcSubscriptionEvent(
                                Instant.now(),
                                Collections.singletonMap(
                                    handle.getName(),
                                    new ResponseItem<>(PlcResponseCode.OK, value)
                                )
                            );
                            consumer.accept(event);
                        } catch (ParseException e) {
                            logger.warn("Could not parse data to desired type: {}", field.getCanOpenDataType(), e);
                            DefaultPlcSubscriptionEvent event = new DefaultPlcSubscriptionEvent(
                                Instant.now(),
                                Collections.singletonMap(
                                    handle.getName(),
                                    new ResponseItem<>(PlcResponseCode.INVALID_DATA, new PlcNull())
                                )
                            );
                            consumer.accept(event);
                        }
                    }
                }
            }
        }
    }

    @Override
    public PlcConsumerRegistration register(Consumer<PlcSubscriptionEvent> consumer, Collection<PlcSubscriptionHandle> handles) {
        final DefaultPlcConsumerRegistration consumerRegistration =new DefaultPlcConsumerRegistration(this, consumer, handles.toArray(new InternalPlcSubscriptionHandle[0]));
        consumers.put(consumerRegistration, consumer);
        return consumerRegistration;
    }

    @Override
    public void unregister(PlcConsumerRegistration registration) {
        consumers.remove(registration);
    }

    @Override
    public void close(ConversationContext<SocketCANFrame> context) {

    }

    @Override
    public void onDisconnect(ConversationContext<SocketCANFrame> context) {
        if (this.heartbeat != null) {
            this.heartbeat.cancel();
            this.heartbeat = null;
        }
    }

    private int cobId(int nodeId, CANOpenService service) {
        // form 32 bit socketcan identifier
        return (nodeId << 24) & 0xff000000 |
            (service.getValue() << 16 ) & 0x00ff0000;
    }

    private CANOpenService serviceId(int cobId) {
        // form 32 bit socketcan identifier
        CANOpenService service = CANOpenService.valueOf((byte) (cobId >> 7));
        if (service == null) {
            for (CANOpenService val : CANOpenService.values()) {
                if (val.getMin() > cobId && val.getMax() < cobId) {
                    return val;
                }
            }
        }
        return service;
    }
}
