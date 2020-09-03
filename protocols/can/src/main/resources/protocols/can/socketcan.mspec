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

[type 'BrokenSocketCANFrame'
    [simple bit 'remote']
    [simple bit 'error']
    [discriminator bit 'extended']
    [typeSwitch 'extended'
        ['true' ExtendedSocketCANFrame
            [simple uint 29 'identifier']
        ]
        ['false' StandardSocketCANFrame
            [const  uint 18 'spacing' '0x0']
            [simple uint 11 'identifier']
        ]
    ]
    [implicit uint 8 'size' 'COUNT(data)']
    [reserved uint 8 '0x0'] // padding
    [reserved uint 8 '0x0'] // reserved / padding
    [reserved uint 8 '0x0'] // reserved / padding
    [array int 8 'data' COUNT 'size']
]

[type 'SocketCANFrame'
    [simple int 32 'rawId']
    [virtual int 32 'identifier'
        'STATIC_CALL("org.apache.plc4x.java.can.helper.HeaderParser.readIdentifier", rawId)'
    ]
    [virtual bit 'extended'
        'STATIC_CALL("org.apache.plc4x.java.can.helper.HeaderParser.isExtended", rawId)'
    ]
    [virtual bit 'remote'
        'STATIC_CALL("org.apache.plc4x.java.can.helper.HeaderParser.isRemote", rawId)'
    ]
    [virtual bit 'error'
        'STATIC_CALL("org.apache.plc4x.java.can.helper.HeaderParser.isError", rawId)'
    ]
    [implicit uint 8 'size' 'COUNT(data)']
    [reserved uint 8 '0x0'] //flags
    [reserved uint 8 '0x0'] // padding 1
    [reserved uint 8 '0x0'] // padding 2
    [array int 8 'data' COUNT 'size']
    [padding uint 8 'alignment' '0x00' '8 - (COUNT(data))']
]

[type 'SocketCAN20AFrame'
    [simple int 16 'identifier']
    [reserved int 8 '0x0'] // filling gap used by extended frame
    [simple bit 'extended']
    [simple bit 'remote']
    [simple bit 'error']
    [reserved int 5 '0x0']  // filling gap used by extended frame
    [implicit uint 8 'size' 'COUNT(data)']
    [reserved uint 8 '0x0'] // in case of fd frame these are flags
    [reserved uint 8 '0x0'] // padding 1
    [reserved uint 8 '0x0'] // padding 2
    [array int 8 'data' COUNT 'size']
]