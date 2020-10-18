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

import org.apache.commons.codec.binary.Hex;
import org.apache.plc4x.java.api.messages.*;
import org.apache.plc4x.java.api.model.PlcConsumerRegistration;
import org.apache.plc4x.java.api.model.PlcField;
import org.apache.plc4x.java.api.model.PlcSubscriptionHandle;
import org.apache.plc4x.java.api.types.PlcResponseCode;
import org.apache.plc4x.java.api.types.PlcSubscriptionType;
import org.apache.plc4x.java.api.value.PlcNull;
import org.apache.plc4x.java.api.value.PlcStruct;
import org.apache.plc4x.java.api.value.PlcUSINT;
import org.apache.plc4x.java.api.value.PlcValue;
import org.apache.plc4x.java.can.canopen.CANOpenFrame;
import org.apache.plc4x.java.can.api.conversation.canopen.CANConversation;
import org.apache.plc4x.java.can.api.conversation.canopen.CANOpenConversation;
import org.apache.plc4x.java.can.api.conversation.canopen.SDODownloadConversation;
import org.apache.plc4x.java.can.api.conversation.canopen.SDOUploadConversation;
import org.apache.plc4x.java.can.canopen.CANOpenFrameBuilderFactory;
import org.apache.plc4x.java.can.canopen.socketcan.CANOpenSocketCANFrameBuilder;
import org.apache.plc4x.java.can.configuration.CANConfiguration;
import org.apache.plc4x.java.can.context.CANOpenDriverContext;
import org.apache.plc4x.java.can.field.CANOpenField;
import org.apache.plc4x.java.can.field.CANOpenNMTField;
import org.apache.plc4x.java.can.field.CANOpenPDOField;
import org.apache.plc4x.java.can.field.CANOpenSDOField;
import org.apache.plc4x.java.can.socketcan.SocketCANConversation;
import org.apache.plc4x.java.canopen.readwrite.*;
import org.apache.plc4x.java.canopen.readwrite.io.CANOpenPayloadIO;
import org.apache.plc4x.java.canopen.readwrite.io.DataItemIO;
import org.apache.plc4x.java.canopen.readwrite.types.CANOpenService;
import org.apache.plc4x.java.canopen.readwrite.types.NMTState;
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
import org.apache.plc4x.java.spi.model.DefaultPlcSubscriptionHandle;
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
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class CANOpenProtocolLogic extends Plc4xProtocolBase<CANOpenFrame> implements HasConfiguration<CANConfiguration>, PlcSubscriber {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10L);
    private Logger logger = LoggerFactory.getLogger(CANOpenProtocolLogic.class);

    private CANOpenFrameBuilderFactory factory = CANOpenSocketCANFrameBuilder::new;

    private CANConfiguration configuration;
    private RequestTransactionManager tm;
    private Timer heartbeat;
    private CANOpenDriverContext canContext;
    private CANConversation<CANOpenFrame> conversation;

    private Map<DefaultPlcConsumerRegistration, Consumer<PlcSubscriptionEvent>> consumers = new ConcurrentHashMap<>();

    @Override
    public void setConfiguration(CANConfiguration configuration) {
        this.configuration = configuration;
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
    public void onConnect(ConversationContext<CANOpenFrame> context) {
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
    public void setContext(ConversationContext<CANOpenFrame> context) {
        super.setContext(context);
        this.conversation = new SocketCANConversation(configuration.getNodeId(), context, factory);
    }

    private CANOpenFrame createFrame(CANOpenHeartbeatPayload state) throws ParseException {
        return factory.createBuilder().withNodeId(configuration.getNodeId()).withService(CANOpenService.HEARTBEAT).withPayload(state).build();
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
        final RequestTransactionManager.RequestTransaction transaction = tm.startRequest();

        String fieldName = writeRequest.getFieldNames().iterator().next();

        CompletableFuture<PlcResponseCode> callback = new CompletableFuture<>();
        callback.whenComplete((code, error) -> {
            if (error != null) {
                response.completeExceptionally(error);
                transaction.endRequest();
                return;
            }
            response.complete(new DefaultPlcWriteResponse(writeRequest, Collections.singletonMap(fieldName, code)));
            transaction.endRequest();
        });

        PlcValue writeValue = writeRequest.getPlcValues().get(0);
        CANOpenConversation canopen = new CANOpenConversation(field.getNodeId(), conversation);
        SDODownloadConversation download = canopen.sdo().download(new IndexAddress(field.getIndex(), field.getSubIndex()), writeValue, field.getCanOpenDataType());
        transaction.submit(() -> download.execute(callback));
    }

    private void writeInternally(InternalPlcWriteRequest writeRequest, CANOpenPDOField field, CompletableFuture<PlcWriteResponse> response) {
        PlcValue writeValue = writeRequest.getPlcValues().get(0);

        try {
            String fieldName = writeRequest.getFieldNames().iterator().next();
            WriteBuffer buffer = DataItemIO.staticSerialize(writeValue, field.getCanOpenDataType(), writeValue.getLength(), true);
            if (buffer != null) {
                final CANOpenPDOPayload payload = new CANOpenPDOPayload(new CANOpenPDO(buffer.getData()));
                context.sendToWire(factory.createBuilder()
                    .withNodeId(field.getNodeId())
                    .withService(field.getService())
                    .withPayload(payload)
                    .build()
                );
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

        Map<String, ResponseItem<PlcSubscriptionHandle>> answers = new LinkedHashMap<>();
        DefaultPlcSubscriptionResponse response = new DefaultPlcSubscriptionResponse(rq, answers);

        for (Map.Entry<String, SubscriptionPlcField> entry : rq.getSubscriptionPlcFieldMap().entrySet()) {
            SubscriptionPlcField subscription = entry.getValue();
            if (subscription.getPlcSubscriptionType() != PlcSubscriptionType.EVENT) {
                answers.put(entry.getKey(), new ResponseItem<>(PlcResponseCode.UNSUPPORTED, null));
            } else if ((subscription.getPlcField() instanceof CANOpenPDOField)) {
                answers.put(entry.getKey(), new ResponseItem<>(PlcResponseCode.OK,
                    new CANOpenPDOSubscriptionHandle(this, entry.getKey(), (CANOpenPDOField) subscription.getPlcField())
                ));
            } else if ((subscription.getPlcField() instanceof CANOpenNMTField)) {
                answers.put(entry.getKey(), new ResponseItem<>(PlcResponseCode.OK,
                    new CANOpenNMTSubscriptionHandle(this, entry.getKey(), (CANOpenNMTField) subscription.getPlcField())
                ));
            } else {
                answers.put(entry.getKey(), new ResponseItem<>(PlcResponseCode.INVALID_ADDRESS, null));
            }
        }

        return CompletableFuture.completedFuture(response);
    }

    private void readInternally(InternalPlcReadRequest readRequest, CANOpenSDOField field, CompletableFuture<PlcReadResponse> response) {
        String fieldName = readRequest.getFieldNames().iterator().next();

        final RequestTransactionManager.RequestTransaction transaction = tm.startRequest();
        CompletableFuture<PlcValue> callback = new CompletableFuture<>();
        callback.whenComplete((value, error) -> {
            if (error != null) {
                response.completeExceptionally(error);
                transaction.endRequest();
                return;
            }

            Map<String, ResponseItem<PlcValue>> fields = new HashMap<>();
            fields.put(fieldName, new ResponseItem<>(PlcResponseCode.OK, value));
            response.complete(new DefaultPlcReadResponse(readRequest, fields));
            transaction.endRequest();
        });

        CANOpenConversation canopen = new CANOpenConversation(field.getNodeId(), conversation);
        SDOUploadConversation upload = canopen.sdo().upload(new IndexAddress(field.getIndex(), field.getSubIndex()), field.getCanOpenDataType());
        transaction.submit(() -> upload.execute(callback));
    }

    @Override
    protected void decode(ConversationContext<CANOpenFrame> context, CANOpenFrame msg) throws Exception {
        int nodeId = msg.getNodeId();
        CANOpenService service = msg.getService();
        CANOpenPayload payload = msg.getPayload();

        if (service != null) {
            if (service.getPdo() && payload instanceof CANOpenPDOPayload) {
                publishEvent(service, nodeId, (CANOpenPDOPayload) payload);
            } else if (service == CANOpenService.NMT && payload instanceof CANOpenNetworkPayload) {
                publishEvent(service, nodeId, (CANOpenNetworkPayload) payload);
            } else {
                String hex = "";
                if (logger.isInfoEnabled()) {
                    try {
                        final WriteBuffer buffer = new WriteBuffer(payload.getLengthInBytes(), true);
                        CANOpenPayloadIO.staticSerialize(buffer, payload);
                        hex = Hex.encodeHexString(buffer.getData());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                logger.info("Decoded CANOpen {} from {}, message {}, {}", service, nodeId, payload, hex);
            }
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

    private void publishEvent(CANOpenService service, int nodeId, CANOpenPayload payload) {
        DefaultPlcSubscriptionHandle dispatchedHandle = null;
        for (Map.Entry<DefaultPlcConsumerRegistration, Consumer<PlcSubscriptionEvent>> entry : consumers.entrySet()) {
            DefaultPlcConsumerRegistration registration = entry.getKey();
            Consumer<PlcSubscriptionEvent> consumer = entry.getValue();

            for (InternalPlcSubscriptionHandle handler : registration.getAssociatedHandles()) {
                if (handler instanceof CANOpenPDOSubscriptionHandle && payload instanceof CANOpenPDOPayload) {
                    CANOpenPDOSubscriptionHandle handle = (CANOpenPDOSubscriptionHandle) handler;

                    if (handle.matches(service, nodeId)) {
                        logger.trace("Dispatching notification {} for node {} to {}", service, nodeId, handle);
                        dispatchedHandle = handle;

                        CANOpenPDOField field = handle.getField();
                        byte[] data = ((CANOpenPDOPayload) payload).getPdo().getData();
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
                } else if (handler instanceof CANOpenPDOSubscriptionHandle && payload instanceof CANOpenHeartbeatPayload) {
                    CANOpenNMTSubscriptionHandle handle = (CANOpenNMTSubscriptionHandle) handler;

                    if (handle.matches(service, nodeId)) {
                        logger.trace("Dispatching notification {} for node {} to {}", service, nodeId, handle);
                        dispatchedHandle = handle;

                        final NMTState state = ((CANOpenHeartbeatPayload) payload).getState();
                        Map<String, PlcValue> fields = new HashMap<>();
                        fields.put("state", new PlcUSINT(state.getValue()));
                        fields.put("node", new PlcUSINT(nodeId));
                        PlcStruct struct = new PlcStruct(fields);
                        DefaultPlcSubscriptionEvent event = new DefaultPlcSubscriptionEvent(
                            Instant.now(),
                            Collections.singletonMap(
                                handle.getName(),
                                new ResponseItem<>(PlcResponseCode.OK, struct)
                            )
                        );
                        consumer.accept(event);
                    }
                }
            }
        }

        if (dispatchedHandle == null) {
            logger.trace("Could not find subscription matching {} and node {}", service, nodeId);
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
    public void close(ConversationContext<CANOpenFrame> context) {

    }

    @Override
    public void onDisconnect(ConversationContext<CANOpenFrame> context) {
        if (this.heartbeat != null) {
            this.heartbeat.cancel();
            this.heartbeat = null;
        }
    }

}
