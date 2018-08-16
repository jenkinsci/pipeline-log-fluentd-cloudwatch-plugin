/*
 * The MIT License
 *
 * Copyright 2018 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.jenkins.plugins.pipeline_log_fluentd_cloudwatch;

import com.amazonaws.services.logs.model.AWSLogsException;
import hudson.ExtensionList;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import static org.hamcrest.Matchers.*;
import org.jenkinsci.plugins.workflow.log.LogStorage;
import org.jenkinsci.plugins.workflow.log.LogStorageTestBase;
import static org.junit.Assume.*;
import org.junit.Before;

public class PipelineBridgeTest extends LogStorageTestBase {

    private Map<String, TimestampTracker> timestampTrackers;

    @Before public void setUp() throws Exception {
        timestampTrackers = new ConcurrentHashMap<>();
        String logGroupName = System.getenv("CLOUDWATCH_LOG_GROUP_NAME");
        assumeThat("must define $CLOUDWATCH_LOG_GROUP_NAME", logGroupName, notNullValue());
        try {
            new Socket(FluentdLogger.host(), FluentdLogger.port()).close();
        } catch (ConnectException x) {
            assumeNoException("set $FLUENTD_SERVICE_HOST / $FLUENTD_SERVICE_PORT_TCP as needed", x);
        }
        /* No set of configuration options or flush calls I tried sufficed to make this throw an exception when Fluentd could not be contacted (just endless warnings in console):
        try (Fluency fluency = Fluency.defaultFluency(FluentdLogger.host(), FluentdLogger.port())) {
            fluency.emit("PipelineBridgeTest", Collections.singletonMap("ping", true));
            fluency.flush();
        }
        */
        CloudWatchAwsGlobalConfiguration configuration = ExtensionList.lookupSingleton(CloudWatchAwsGlobalConfiguration.class);
        configuration.setLogGroupName(logGroupName);
        try {
            configuration.getAWSLogsClientBuilder().build().describeLogGroups();
        } catch (AWSLogsException x) {
            assumeNoException(x);
        }
    }

    @Override protected LogStorage createStorage() throws Exception {
        return new PipelineBridge.LogStorageImpl("PipelineBridgeTest", "123", timestampTrackers);
    }

}