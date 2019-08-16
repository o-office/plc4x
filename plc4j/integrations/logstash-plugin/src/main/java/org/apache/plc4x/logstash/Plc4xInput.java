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
package org.apache.plc4x.logstash;

import co.elastic.logstash.api.*;
import org.apache.plc4x.java.PlcDriverManager;
import org.apache.plc4x.java.scraper.ResultHandler;
import org.apache.plc4x.java.scraper.config.triggeredscraper.JobConfigurationTriggeredImplBuilder;
import org.apache.plc4x.java.scraper.config.triggeredscraper.ScraperConfigurationTriggeredImpl;
import org.apache.plc4x.java.scraper.config.triggeredscraper.ScraperConfigurationTriggeredImplBuilder;
import org.apache.plc4x.java.scraper.exception.ScraperException;
import org.apache.plc4x.java.scraper.triggeredscraper.TriggeredScraperImpl;
import org.apache.plc4x.java.scraper.triggeredscraper.triggerhandler.collector.TriggerCollector;
import org.apache.plc4x.java.scraper.triggeredscraper.triggerhandler.collector.TriggerCollectorImpl;
import org.apache.plc4x.java.utils.connectionpool.PooledPlcDriverManager;
import org.apache.plc4x.logstash.configuration.Job;
import org.apache.plc4x.logstash.configuration.Source;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

// class name must match plugin name
@LogstashPlugin(name="plc4x_input")
public class Plc4xInput implements Input {

    public static final PluginConfigSpec<Map<String, Object>> JOB_CONFIG =
            PluginConfigSpec.hashSetting("jobs");

    public static final PluginConfigSpec<Map<String, Object>> SOURCE_CONFIG =
            PluginConfigSpec.hashSetting("sources");
    private final Map<String, Object> sources;
    private final Map<String, Object> jobs;

    private String id;
    private PlcDriverManager plcDriverManager;
    private TriggerCollector triggerCollector;
    private TriggeredScraperImpl scraper;

    private final CountDownLatch done = new CountDownLatch(1);

    // all plugins must provide a constructor that accepts id, Configuration, and Context
    public Plc4xInput(String id, Configuration config, Context context) {
        // constructors should validate configuration options
        this.id = id;
        jobs = config.get(JOB_CONFIG);
        sources = config.get(SOURCE_CONFIG);
    }

    @Override
    public void start(Consumer<Map<String, Object>> consumer) {

        // The start method should push Map<String, Object> instances to the supplied QueueWriter
        // instance. Those will be converted to Event instances later in the Logstash event
        // processing pipeline.
        //
        // Inputs that operate on unbounded streams of data or that poll indefinitely for new
        // events should loop indefinitely until they receive a stop request. Inputs that produce
        // a finite sequence of events should loop until that sequence is exhausted or until they
        // receive a stop request, whichever comes first.
        // Establish a connection to the plc using the url provided as first argument
        ScraperConfigurationTriggeredImplBuilder builder = new ScraperConfigurationTriggeredImplBuilder();
        //TODO: use multiple sources:

        for (String sourceName : sources.keySet()) {
            //TODO: check !
            builder.addSource(sourceName, ((Source) sources.get(sourceName)).getConnectionUri());
        }

        for (String jobName : jobs.keySet()) {
            Job job = ((Job) jobs.get(jobName));
            JobConfigurationTriggeredImplBuilder jobBuilder = builder.job(
                jobName, String.format("(SCHEDULED,%s)", job.getRate()));
            for (String source : job.getSources()) {
                jobBuilder.source(source);
            }
            for (String query : job.getQueries()) {
                String[] fieldSegments = query.split("=");
                if(fieldSegments.length != 2) {
                    System.err.println(String.format("Error in job configuration '%s'. " +
                            "The field segment expects a format {field-alias}#{field-address}, but got '%s'",
                        jobName, query));
                    continue;
                }
                String fieldAlias = fieldSegments[0];
                String fieldAddress = fieldSegments[1];
                jobBuilder.field(fieldAlias, fieldAddress);
            }
            jobBuilder.build();
        }

        ScraperConfigurationTriggeredImpl scraperConfig = builder.build();
        try {
            plcDriverManager = new PooledPlcDriverManager();
            triggerCollector = new TriggerCollectorImpl(plcDriverManager);
            scraper = new TriggeredScraperImpl(scraperConfig, new ResultHandler() {
                @Override
                public void handle(String jobName, String sourceName, Map<String, Object> results) {
                    //TODO: use jobname etc for multiple connections
                    consumer.accept(results);
                }
            }, triggerCollector);
            scraper.start();
            triggerCollector.start();
        } catch (ScraperException e) {
            System.err.println("Error starting the scraper: "+ e);
        }
    }

    @Override
    public void stop() {
        triggerCollector.stop();
        scraper.stop();
    }

    @Override
    public void awaitStop() throws InterruptedException {
       done.await(); // blocks until input has stopped
    }

    @Override
    public Collection<PluginConfigSpec<?>> configSchema() {
        // should return a list of all configuration options for this plugin
        return Arrays.asList(JOB_CONFIG, SOURCE_CONFIG);
    }

    @Override
    public String getId() {
        return this.id;
    }
}
