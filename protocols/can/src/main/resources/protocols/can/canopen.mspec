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

[enum uint 4 'CANOpenService'
    ['0x00' BROADCAST]
    ['0x07' NMT]
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
            [reserved uint 1 '0x0']
            [simple uint 7 'node']
        ]
        ['CANOpenService.NMT' CANOpenNetworkPayload
            [enum NMTState 'state']
        ]
    ]
]