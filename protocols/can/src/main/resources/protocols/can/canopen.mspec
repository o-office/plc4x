/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

[enum uint 4 'CANOpenService' [bit 'sdo', bit 'pdo', bit 'transmit', bit 'receive']
    ['0b0000' BROADCAST    ['false', 'false', 'false', 'false'] ]
    ['0b0001' SYNC         ['false', 'false', 'false', 'false'] ]
    ['0b0010' TIME         ['false', 'false', 'false', 'false'] ]
    ['0b1110' NMT          ['false', 'false', 'false', 'false'] ]
    ['0b1100' SDO_REQUEST  ['true',  'false', 'false', 'true' ] ]
    ['0b1011' SDO_RESPONSE ['true',  'false', 'true',  'false'] ]
    ['0b0011' TPDO_1       ['false', 'true',  'true',  'false'] ]
    ['0b0100' RPDO_1       ['false', 'true',  'false', 'true' ] ]
    ['0b0101' TPDO_2       ['false', 'true',  'true',  'false'] ]
    ['0b0110' RPDO_2       ['false', 'true',  'false', 'true' ] ]
    ['0b0111' TPDO_3       ['false', 'true',  'true',  'false'] ]
    ['0b1000' RPDO_3       ['false', 'true',  'false', 'true' ] ]
    ['0b1001' TPDO_4       ['false', 'true',  'true',  'false'] ]
    ['0b1010' RPDO_4       ['false', 'true',  'false', 'true' ] ]
]

[enum uint 8 'NMTStateRequest'
    ['0x01' OPERATIONAL]
    ['0x02' STOP]
    ['0x80' PRE_OPERATIONAL]
    ['0x81' RESET_NODE]
    ['0x82' RESET_COMMUNICATION]
]

[enum uint 8 'NMTState'
    ['0x00' BOOTED_UP]
    ['0x04' STOPPED]
    ['0x05' OPERATIONAL]
    ['0x7f' PRE_OPERATIONAL]
]

[discriminatedType 'CANOpenPayload' [CANOpenService 'function']
    [typeSwitch 'function'
        ['CANOpenService.BROADCAST' CANOpenBroadcastPayload
            [enum NMTStateRequest 'request']
            [reserved uint 1 '0x00']
            [simple uint 7 'node']
        ]
        ['CANOpenService.TIME' CANOpenTimeSynchronization
            [simple TimeOfDay 'timeOfDay']
        ]
        ['CANOpenService.NMT' CANOpenNetworkPayload
            [enum NMTState 'state']
        ]
        ['CANOpenService.SDO_REQUEST' CANOpenSDORequest
            [enum SDOCommand 'command']
            [reserved uint 1 '0x00']
            [implicit uint 2 'size' 'COUNT(data)']
            [simple bit 'expedited'] // segmented
            [simple bit 'placement']
            [simple uint 16 'index']
            [simple uint 8 'subindex']
            [array uint 8 'data' COUNT 'size']
        ]
        ['CANOpenService.SDO_RESPONSE' CANOpenSDOResponse
            [enum SDOCommand 'command']
            [reserved uint 1 '0x00']
            [implicit uint 2 'size' 'COUNT(data)']
            [simple bit 'expedited'] // segmented
            [simple bit 'placement']
            [simple uint 16 'index']
            [simple uint 8 'subindex']
            [array uint 8 'data' COUNT 'size']
        ]
        ['CANOpenService.RPDO_1' CANOpenRPDO
            [simple CANOpenPDO 'pdo' ['1', 'true']]
        ]
        ['CANOpenService.TPDO_1' CANOpenTPDO
            [simple CANOpenPDO 'pdo' ['1', 'false']]
        ]
        ['CANOpenService.RPDO_2' CANOpenRPDO
            [simple CANOpenPDO 'pdo' ['2', 'true']]
        ]
        ['CANOpenService.TPDO_2' CANOpenTPDO
            [simple CANOpenPDO 'pdo' ['1', 'false']]
        ]
        ['CANOpenService.RPDO_3' CANOpenRPDO
            [simple CANOpenPDO 'pdo' ['3', 'true']]
        ]
        ['CANOpenService.TPDO_3' CANOpenTPDO
            [simple CANOpenPDO 'pdo' ['1', 'false']]
        ]
        ['CANOpenService.RPDO_4' CANOpenRPDO
            [simple CANOpenPDO 'pdo' ['4', 'true']]
        ]
        ['CANOpenService.TPDO_4' CANOpenTPDO
            [simple CANOpenPDO 'pdo' ['1', 'false']]
        ]
    ]
]

[enum uint 3 'SDOCommand'
    ['0x00' SEGMENT_DOWNLOAD]
    ['0x01' INITIALIZE_DOWNLOAD]
    ['0x02' INITIALIZE_UPLOAD]
    ['0x03' SEGMENT_UPLOAD]
    ['0x04' ABORT]
    ['0x05' BLOCK_UPLOAD]
    ['0x06' BLOCK_DOWNLOAD]
]

[type 'CANOpenPDO' [uint 2 'index', bit 'receive']

]

[type 'TimeOfDay'
    // CiA 301 - section 7.1.6.5
    [simple uint 28 'millis']
    [reserved int 4 '0x00']
    [simple uint 16 'days']
]