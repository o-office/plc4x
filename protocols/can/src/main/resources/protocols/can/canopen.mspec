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

[enum uint 4 'CANOpenService' [uint 8 'min', uint 8 'max', bit 'pdo']
    ['0b0000' NMT             ['0',     '0'    , 'false' ] ]
    ['0b0001' SYNC            ['0x80',  '0x80' , 'false' ] ]
    ['0b0001' EMCY            ['0x81',  '0xFF' , 'false' ] ]
    ['0b0010' TIME            ['0x100', '0x100', 'false' ] ]
    ['0b0011' TRANSMIT_PDO_1  ['0x181', '0x1FF', 'true'  ] ]
    ['0b0100' RECEIVE_PDO_1   ['0x201', '0x27F', 'true'  ] ]
    ['0b0101' TRANSMIT_PDO_2  ['0x281', '0x2FF', 'true'  ] ]
    ['0b0110' RECEIVE_PDO_2   ['0x301', '0x37F', 'true'  ] ]
    ['0b0111' TRANSMIT_PDO_3  ['0x381', '0x3FF', 'true'  ] ]
    ['0b1000' RECEIVE_PDO_3   ['0x401', '0x47F', 'true'  ] ]
    ['0b1001' TRANSMIT_PDO_4  ['0x481', '0x4FF', 'true'  ] ]
    ['0b1010' RECEIVE_PDO_4   ['0x501', '0x57F', 'true'  ] ]
    ['0b1011' TRANSMIT_SDO    ['0x581', '0x5FF', 'false' ] ]
    ['0b1100' RECEIVE_SDO     ['0x601', '0x67F', 'false' ] ]
    ['0b1110' HEARTBEAT       ['0x701', '0x77F', 'false' ] ]
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
        ['CANOpenService.NMT' CANOpenNetworkPayload
            [enum NMTStateRequest 'request']
            [reserved uint 1 '0x00']
            [simple uint 7 'node']
        ]
        ['CANOpenService.TIME' CANOpenTimeSynchronization
            [simple TimeOfDay 'timeOfDay']
        ]
        ['CANOpenService.RECEIVE_PDO_1' CANOpenPDOPayload
            [simple CANOpenPDO 'pdo' ['1', 'true']]
        ]
        ['CANOpenService.TRANSMIT_PDO_1' CANOpenPDOPayload
            [simple CANOpenPDO 'pdo' ['1', 'false']]
        ]
        ['CANOpenService.RECEIVE_PDO_2' CANOpenPDOPayload
            [simple CANOpenPDO 'pdo' ['2', 'true']]
        ]
        ['CANOpenService.TRANSMIT_PDO_2' CANOpenPDOPayload
            [simple CANOpenPDO 'pdo' ['1', 'false']]
        ]
        ['CANOpenService.RECEIVE_PDO_3' CANOpenPDOPayload
            [simple CANOpenPDO 'pdo' ['3', 'true']]
        ]
        ['CANOpenService.TRANSMIT_PDO_3' CANOpenPDOPayload
            [simple CANOpenPDO 'pdo' ['1', 'false']]
        ]
        ['CANOpenService.RECEIVE_PDO_4' CANOpenPDOPayload
            [simple CANOpenPDO 'pdo' ['4', 'true']]
        ]
        ['CANOpenService.TRANSMIT_PDO_4' CANOpenPDOPayload
            [simple CANOpenPDO 'pdo' ['1', 'false']]
        ]
        ['CANOpenService.TRANSMIT_SDO' CANOpenSDORequest
            [enum SDOCommand 'command']
            [simple SDORequest 'request' ['command']]
        ]
        ['CANOpenService.RECEIVE_SDO' CANOpenSDOResponse
            [enum SDOCommand 'command']
            [simple SDOResponse 'response' ['command']]
        ]
        ['CANOpenService.HEARTBEAT' CANOpenHeartbeatPayload
            [enum NMTState 'state']
        ]
    ]
]

[type 'SDORequest' [SDOCommand 'command']
    [typeSwitch 'command'
        ['SDOCommand.INITIALIZE_DOWNLOAD' SDOInitializeDownloadRequest
            [reserved uint 1 '0x00']
            [implicit uint 2 'size' 'expedited && indicated ? 4 - COUNT(data) : 0']
            [simple bit 'expedited']
            [simple bit 'indicated']
            [simple Multiplexer 'address']
            [array int 8 'data' COUNT '(expedited && indicated) ? 4 - size : 0']
            [padding uint 8 'alignment' '0x00' '4 - (COUNT(data))']
        ]
        ['SDOCommand.SEGMENT_DOWNLOAD' SDOSegmentDownloadRequest
            [simple bit 'toggle']
            [implicit uint 3 'size' '7 - COUNT(data)']
            [simple bit 'last']
            [array int 8 'data' COUNT '7 - data']
            [padding uint 8 'alignment' '0x00' '7 - (COUNT(data))']
        ]
        ['SDOCommand.INITIALIZE_UPLOAD' SDOInitializeUploadRequest
            [reserved uint 5 '0x00']
            [simple Multiplexer 'address']
            [reserved int 32 '0x00'] // padding
        ]
    ]
]

[type 'SDOResponse' [SDOCommand 'command']
    [typeSwitch 'command'
        ['SDOCommand.SEGMENT_UPLOAD' SDOSegmentUploadResponse
            [reserved uint 5 '0x00']
            [simple Multiplexer 'address']
            [reserved int 32 '0x00'] // padding
        ]
        ['SDOCommand.INITIALIZE_DOWNLOAD' SDOInitializeDownloadResponse
            [simple bit 'toggle']
            [reserved uint 4 '0x00']
            [reserved int 32 '0x00'] // padding
        ]
        ['SDOCommand.INITIALIZE_UPLOAD' SDOInitializeUploadResponse
            [simple SDOSegment 'segment']
        ]
    ]
]

[type 'SDOSegment'
    [reserved uint 1 '0x00']
    [implicit uint 2 'size' 'expedited && indicated ? 4 - COUNT(data) : 0']
    [simple bit 'expedited']
    [simple bit 'indicated']
    [simple Multiplexer 'address']
    [array int 8 'data' COUNT '(expedited && indicated) ? 4 - size : 0']
    [padding uint 8 'alignment' '0x00' '4 - (COUNT(data))']
]

[type 'Multiplexer'
    [simple uint 16 'index']
    [simple uint 8 'subindex']
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
    [array int 8 'data' COUNT '8']
]

[type 'TimeOfDay'
    // CiA 301 - section 7.1.6.5
    [simple uint 28 'millis']
    [reserved int 4 '0x00']
    [simple uint 16 'days']
]