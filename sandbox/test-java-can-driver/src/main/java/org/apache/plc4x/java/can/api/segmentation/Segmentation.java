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
package org.apache.plc4x.java.can.api.segmentation;

import org.apache.plc4x.java.can.api.segmentation.accumulator.Storage;
import org.apache.plc4x.java.spi.ConversationContext;
import org.apache.plc4x.java.spi.transaction.RequestTransactionManager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Utility type for handling segmented request-response operations.
 *
 * Segmented operation is one which spans over multiple frames and contains of:
 * - initialization
 * - 0... n steps
 * - finalization
 *
 * Depending on use case there might be 0 intermediate frames, leading to situation where requester sends segment request
 * and answer comes in one shot.
 */
public abstract class Segmentation<C, T, R> {

    private final Duration timeout;
    private final Storage<T, R> storage;
    private final Class<C> frameType;

    private Supplier<T> request;
    private Predicate<T> responseValidator;

    private Predicate<T> finalStep;
    private Consumer<List<T>> callback;
    private Function<T, T> step;
    private Predicate<T> stepValidator;
    private List<T> answers = new ArrayList<>();

    public Segmentation(Class<C> frameType, Duration timeout, Storage<T, R> storage) {
        this.frameType = frameType;
        this.timeout = timeout;
        this.storage = storage;
    }

    public Segmentation<C, T, R> begin(Supplier<T> request, Predicate<T> requestAnswer) {
        this.request = request;
        this.responseValidator = requestAnswer;
        return this;
    }

    public Segmentation<C, T, R> step(Function<T, T> step, Predicate<T> stepAnswer) {
        this.step = step;
        this.stepValidator = stepAnswer;
        return this;
    }

    public Segmentation<C, T, R> end(Predicate<T> finalStep, Consumer<List<T>> callback) {
        this.finalStep = finalStep;
        this.callback = callback;
        return this;
    }

    public void execute(RequestTransactionManager tm, ConversationContext<C> context) throws PlcSegmentationException {
        Consumer<T> consumer = new Consumer<T>() {
            @Override
            public void accept(T payload) {
                storage.append(payload);

                if (finalStep.test(payload)) {
                    callback.accept(answers);
                } else {
                    try {
                        T apply = step.apply(payload);
                        send(tm, context, () -> apply, stepValidator, this);
                    } catch (PlcSegmentationException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
        try {
            send(tm, context, request, responseValidator, consumer);
        } catch (RuntimeException e) {
            if (e.getCause() != null) {
                throw new PlcSegmentationException(e.getCause());
            }
            throw new PlcSegmentationException(e);
        }
    }

    private void send(RequestTransactionManager tm, ConversationContext<C> context, Supplier<T> generator, Predicate<T> predicate, Consumer<T> callback) throws PlcSegmentationException {
        C request = wrap(generator.get());

        RequestTransactionManager.RequestTransaction transaction = tm.startRequest();
        transaction.submit(() -> context.sendRequest(request)
            .expectResponse(frameType, timeout)
            .unwrap(this::unwrap)
            .check(predicate)
            .onError((payload, error) -> transaction.endRequest())
            .handle(payload -> {
                callback.accept(payload);
                transaction.endRequest();
            })
        );
    }

    protected abstract T unwrap(C frame);

    protected abstract C wrap(T payload) throws PlcSegmentationException;

}
