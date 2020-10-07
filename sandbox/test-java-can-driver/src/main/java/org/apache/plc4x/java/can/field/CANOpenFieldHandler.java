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
package org.apache.plc4x.java.can.field;

import org.apache.plc4x.java.api.exceptions.PlcInvalidFieldException;
import org.apache.plc4x.java.api.exceptions.PlcRuntimeException;
import org.apache.plc4x.java.api.model.PlcField;
import org.apache.plc4x.java.api.value.*;
import org.apache.plc4x.java.spi.connection.DefaultPlcFieldHandler;
import org.apache.plc4x.java.spi.connection.PlcFieldHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CANOpenFieldHandler extends DefaultPlcFieldHandler implements PlcFieldHandler {

    @Override
    public PlcField createField(String fieldQuery) throws PlcInvalidFieldException {
        return CANOpenField.of(fieldQuery);
    }

    @Override
    public PlcValue encodeString(PlcField field, Object[] values) {
        CANOpenSDOField coField = (CANOpenSDOField) field;
        String[] strings = (String[]) values;

        switch (coField.getCanOpenDataType()) {
            case VISIBLE_STRING:
            case OCTET_STRING:
            case UNICODE_STRING:
                if (values.length == 1) {
                    return new PlcString(strings[0]);
                } else {
                    return new PlcList(Arrays.stream(strings).map(PlcString::new).collect(Collectors.toList()));
                }
        }

        throw new PlcRuntimeException("Invalid encoder for type " + coField.getCanOpenDataType().name());
    }

    @Override
    public PlcValue encodeByte(PlcField field, Object[] values) {
        List<PlcValue> resultSet = new ArrayList<>();
        for (Object item : values) {
            resultSet.add(PlcValues.of((Byte) item));
        }

        if (resultSet.size() == 1) {
            return resultSet.get(0);
        } else {
            return new PlcList(resultSet);
        }
    }
}
