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

[type 'CANOpenFrame'
    [enum CANOpenService 'function']
    [simple int 11 'identifier']
    [reserved int 9 '0x0'] // filling gap used by extended frame, and extended marker which should always be 0
    [simple bit 'remote']
    [simple bit 'error']
    [reserved int 5 '0x0']  // filling gap used by extended frame
    [implicit uint 8 'size' 'COUNT(payload)']
    [reserved uint 8 '0x0'] // in case of fd frame these are flags
    [reserved uint 8 '0x0'] // padding 1
    [reserved uint 8 '0x0'] // padding 2
    [simple CANOpenPayload 'payload' ['function', 'size']]
]

[enum uint 4 'CANOpenService'
    ['0b1110' NMT]
]

[discriminatedType 'CANOpenPayload' [CANOpenService 'function', uint 8 'size']
    [typeSwitch 'function'
        ['CANOpenService.NMT' CANOpenNetworkPayload [uint 8 'size']
            [array int 8 'data' COUNT 'size']
        ]
    ]
]