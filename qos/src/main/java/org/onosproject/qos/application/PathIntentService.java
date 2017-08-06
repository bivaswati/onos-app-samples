/*
 * Copyright 2016 WIPRO Technologies Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * PathIntentService.java
 * Author : E.Ravikumaran,Bivas,R.Eswarraj,Shahabuddeen
 * Date   : 15-Oct-2015
 **/
package org.onosproject.qos.application;

import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.HostId;
import org.onosproject.net.Path;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.PathIntent;
import org.onosproject.qos.rest.client.ServiceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.onosproject.net.intent.IntentState.FAILED;
import static org.onosproject.net.intent.IntentState.WITHDRAWN;


/**
 * Provides API for creating, removing and getting PathIntents for the Application.
 */
public class PathIntentService {
   protected PathIntentService() {

    }
    protected static DefaultServiceDirectory serviceDirectory;
    protected static ApplicationId appID = VideoSelector.getAppId();

    static final Logger LOGGER = LoggerFactory.getLogger(PathIntentService.class);

    /**
     * Creates Path intents between client and server.
     * @param path Path between client and server
     * @param prityQueueVal QueueID
     * @return Intent ID and status of intent
     */
    public static IntentId createPathIntent(Path path, long prityQueueVal) {
        PathIntent intent;
        try {
            MacAddress srcMac = getSource(path).mac();
            MacAddress dstMac = getDestination(path).mac();
            PathIntent.Builder pathIntentBuilder = PathIntent.builder();
            pathIntentBuilder.appId(appID);
            TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
            selectorBuilder.matchEthDst(dstMac);
            selectorBuilder.matchEthSrc(srcMac);
            pathIntentBuilder.selector(selectorBuilder.build());
            TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
            treatmentBuilder.setQueue(prityQueueVal);
            pathIntentBuilder.treatment(treatmentBuilder.build());
            pathIntentBuilder.priority(ServiceConstants.PATH_INTENT_PRIORITY);
            pathIntentBuilder.path(path);
            intent = pathIntentBuilder.build();
            IntentService intentService;
            intentService = getOnosService(IntentService.class);

            if (intent != null) {
                Key k = intent.key();
                CountDownLatch latch = new CountDownLatch(1);
                ArrayList<IntentEvent.Type> events = new ArrayList<>();
                events.add(IntentEvent.Type.INSTALLED);

                IntentListener listener = new Listener(k, latch, events);
                intentService.addListener(listener);
                try {
                    // request to submit
                    intentService.submit(intent);
                    try {
                        latch.await(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        LOGGER.error("QoS - PathIntentService.createPathIntent - Exception", k);
                        return null;
                    }
                    return intent.id();
                } finally {
                    // clean up the listener
                    intentService.removeListener(listener);
                }
            }
        } catch (Exception e) {
            LOGGER.error("QoS - PathIntentService.createPathIntent - Exception", e);
        }
        return null;
    }

    static HostId getSource(Path path) {
        ConnectPoint src = path.links().get(0).src();
        return src.hostId();
    }

    static HostId getDestination(Path path) {
        ConnectPoint dst = path.links().get(path.links().size() - 1).dst();
        return dst.hostId();
    }

    /**
     * Get Intent from ONOS.
     * @param intentId Key of the Intent
     * @return Intent
     */
    public static Intent getIntent(IntentId intentId) {
        IntentService intentService = getOnosService(IntentService.class);

        Intent intent = intentService.getIntent(Key.of(intentId.toString(), appID));
        if (intent == null) {
            intent = intentService.getIntent(Key.of(intentId.fingerprint(), appID));
        }
        return intent;
    }

    /**
     * Get PathIntent only from ONOS.
     * @param intentId Key of the Intent
     * @return Intent
     */
    public static PathIntent getPathIntent(IntentId intentId) {
        Intent intent = getIntent(intentId);
        if (intent != null && intent instanceof PathIntent) {
            return (PathIntent) intent;
        }
        return null;
    }
    /**
     * Delete an Intent.
     * @param inputKey Key of the Intent that needs to be deleted
     * @return True if deleted, false if Intent is not deleted or don't exist
     */
    public static boolean performDelete(IntentId inputKey) {
        IntentService intentService = getOnosService(IntentService.class);

        Intent intent = intentService.getIntent(Key.of(inputKey.toString(), appID));
        if (intent == null) {
            intent = intentService.getIntent(Key.of(Long.decode(inputKey.toString()), appID));
        }
        if (intent == null) {
            LOGGER.info("QoS - PathIntentService.performDelete - Intent cannot be found for ID - " + inputKey);
            return false;
        }
        Key k = intent.key();
        // set up latch and listener to track uninstall progress
        CountDownLatch latch = new CountDownLatch(1);

        ArrayList<IntentEvent.Type> events = new ArrayList<>();
        events.add(IntentEvent.Type.WITHDRAWN);
        events.add(IntentEvent.Type.FAILED);

        IntentListener listener = new Listener(k, latch, events);
        intentService.addListener(listener);
        try {
            // request the withdraw
            intentService.withdraw(intent);
            try {
                latch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOGGER.error("QoS - PathIntentService.performDelete - Exception", k);
            }
            // double check the state
            IntentState state = intentService.getIntentState(k);
            if (state == WITHDRAWN || state == FAILED) {
                intentService.purge(intent);
            }

        } finally {
            // clean up the listener
            intentService.removeListener(listener);
        }
        return true;
    }

    /**
     * Listener class for Intent deletion - copied from ONOS.
     */
    static class Listener implements IntentListener {
        final Key key;
        final CountDownLatch latch;
        ArrayList<IntentEvent.Type> events = new ArrayList<>();

        Listener(Key key, CountDownLatch latch, ArrayList<IntentEvent.Type> events) {
            this.key = key;
            this.latch = latch;
            this.events = events;
        }

        @Override
        public void event(IntentEvent event) {
            if (Objects.equals(event.subject().key(), key) && events.contains(event.type())) {
                latch.countDown();
            }
        }
    }

    /**
     *Get the service class implementation from ONOS.
     * @param aClass Service class that is need
     * @param <T> Class of the Service
     * @return Implementation of the service class
     */
    protected static  <T> T getOnosService(Class<T> aClass) {
        if (serviceDirectory == null) {
            serviceDirectory = new DefaultServiceDirectory();
        }
        return serviceDirectory.get(aClass);
    }
}