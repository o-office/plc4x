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
package org.apache.plc4x.java.ads.api.commands.types;

import io.netty.buffer.ByteBuf;
import org.apache.commons.lang3.StringUtils;
import org.apache.plc4x.java.ads.api.util.ByteValue;

import java.nio.charset.Charset;

import static java.util.Objects.requireNonNull;

public class Device extends ByteValue {

    private static final int NUM_BYTES = 16;

    private Device(byte... values) {
        super(values);
        assertLength(NUM_BYTES);
    }

    public static Device of(byte... values) {
        return new Device(values);
    }

    public static Device of(ByteBuf byteBuf) {
        byte[] values = new byte[NUM_BYTES];
        byteBuf.readBytes(values);
        return of(values);
    }

    public static Device of(String value) {
        requireNonNull(value);
        return new Device(StringUtils.leftPad(value,NUM_BYTES).getBytes());
    }

    public static Device of(String value, Charset charset) {
        requireNonNull(value);
        return new Device(StringUtils.leftPad(value,NUM_BYTES).getBytes(charset));
    }

    @Override
    public String toString() {
        return "Device{" + new String(value) + "} " + super.toString();
    }
}
