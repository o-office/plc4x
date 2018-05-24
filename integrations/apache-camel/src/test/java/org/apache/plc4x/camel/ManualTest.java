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
package org.apache.plc4x.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.main.Main;
import org.apache.camel.main.MainListenerSupport;
import org.apache.camel.main.MainSupport;

import java.util.Date;

public class ManualTest {

    private Main main;

    public static void main(String[] args) throws Exception {
        ManualTest example = new ManualTest();
        example.boot();
    }

    public void boot() throws Exception {
        // create a Main instance
        main = new Main();
        // bind MyBean into the registry
        main.bind("foo", new MyBean());
        // add routes
        main.addRouteBuilder(new MyRouteBuilder());
        // add event listener
        main.addMainListener(new Events());
        // set the properties from a file
        main.setPropertyPlaceholderLocations("example.properties");
        // run until you terminate the JVM
        System.out.println("Starting Camel. Use ctrl + c to terminate the JVM.\n");
        main.run();
    }

    private static class MyRouteBuilder extends RouteBuilder {
        @Override
        public void configure() {
            from("plc4x:ads:tcp://10.10.64.40/10.10.64.40.1.1:851/10.10.56.23.1.1:30000?dataType=java.lang.Integer&address=Allgemein_S2.Station")
                .process(new Processor() {
                    public void process(Exchange exchange) {
                        System.out.println("Invoked timer at " + new Date());
                    }
                })
                .bean("foo")
                .log("Received ${body}");
        }
    }

    public static class MyBean {
        public void callMe() {
            System.out.println("MyBean.callMe method has been called");
        }
    }

    public static class Events extends MainListenerSupport {

        @Override
        public void afterStart(MainSupport main) {
            System.out.println("MainExample with Camel is now started!");
        }

        @Override
        public void beforeStop(MainSupport main) {
            System.out.println("MainExample with Camel is now being stopped!");
        }
    }
}