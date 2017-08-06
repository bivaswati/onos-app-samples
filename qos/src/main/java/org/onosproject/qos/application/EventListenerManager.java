/*
 *  Copyright 2016 WIPRO Technologies Limited
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
 * File Name    : EventListenerManager.java.
 * Author       : Bivas
 * Date         : 08-Apr-2016
 */
package org.onosproject.qos.application;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.event.AbstractEvent;
import org.onosproject.event.ListenerTracker;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkService;
import org.onosproject.qos.cache.HostDataCache;
import org.onosproject.qos.cache.IntentDataCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.onlab.util.Tools.groupedThreads;


/**
 * Desciription : This class acts as Listener and fetches the change status in link (Up/Down).
 */
@Component(immediate = true)
public class EventListenerManager {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    @Property(name = "excludeStatsEvent", boolValue = true,
            label = "Exclude stats related events")
    private boolean excludeStatsEvent = true;
    private ListenerTracker listeners;

    private InternalLinkListener internalLinkListener = new InternalLinkListener();
    private ExecutorService linkEventExecutorService =
            Executors.newSingleThreadExecutor(groupedThreads("onos/qos", "link-event"));

    private synchronized void handleLinkRemoved(LinkEvent item) {
        CopyOnWriteArrayList<IntentId> intents = IntentDataCache.getIntentIds(item.subject());
        if (intents != null && intents.size() > 0) {
            for (IntentId intent : intents) {
                VideoSelector.notifyClient(intent, "LINK_DOWN");
                IntentId id = null;
                try {
                    try {
                        id = VideoSelectorService.getInstance().createAlternatePathIntent(intent, true);
                    } catch (Exception ex) {
                        log.error("No path to Reach Server");
                    }
                    if (id != null) {
                        VideoSelector.notifyClient(id, "LINK_RESTORED");
                    } else {
                        String currSessn = IntentDataCache.getSessnbyIntent(intent);
                        String curUrl = IntentDataCache.fetchCurrurl(currSessn);
                        VideoSelectorService.nextServer(currSessn, curUrl, intent);
                    }
                } catch (Exception e) {
                    log.error("QoS - EventListenerManager.handleLinkRemoved - Exception : ", e);
                }

            }
        }
    }

    @Activate
    protected void activate() {
        try {
            HostDataCache.updateHostDataCache();
        } catch (Exception e) {
            log.error("QoS - EventListenerManager.activate - Exception", e);
        }
        listeners = new ListenerTracker();
        listeners.addListener(linkService, internalLinkListener)
                .addListener(deviceService, new InternalDeviceListener())
                .addListener(hostService, new InternalHostListener());
    }

    @Deactivate
    protected void deactivate() {
        listeners.removeListeners();
        linkEventExecutorService.shutdown();
    }

    class InternalDeviceListener  implements DeviceListener {
        @Override
        public boolean isRelevant(DeviceEvent event) {
            return !excludeStatsEvent || event.type() != DeviceEvent.Type.PORT_STATS_UPDATED;
        }
        @Override
        public void event(DeviceEvent event) {
            log.info("QoS - EventListenerManager - InternalDeviceListener.event" + event.toString());
        }
    }

    class InternalLinkListener implements LinkListener {

        @Override
        public void event(LinkEvent linkEvent) {
            linkEventExecutorService.execute(new InternalEventHandler(linkEvent));
        }
    }
    class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            HostDataCache.updateHostDataCache();
        }
    }
    private class InternalEventHandler implements Runnable {

        volatile AbstractEvent event;

        InternalEventHandler(AbstractEvent event) {
            this.event = event;
        }

        @Override
        public void run() {
            if (event instanceof LinkEvent) {
                LinkEvent linkEvent = (LinkEvent) event;
                switch (linkEvent.type()) {
                    case LINK_REMOVED:
                        handleLinkRemoved(linkEvent);
                        break;
                    default:
                        break;
                }
            }
        }
    }
}