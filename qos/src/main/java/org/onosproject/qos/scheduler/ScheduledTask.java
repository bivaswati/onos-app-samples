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
 * ClientServerCostCompare.java
 * Author : Shahabudeen
 * Date   : 16-Oct-2015
 **/

package org.onosproject.qos.scheduler;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.onosproject.net.intent.IntentId;
import org.onosproject.qos.application.VideoSelector;
import org.onosproject.qos.application.VideoSelectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Scheduler to check wheather congession between the installed intents.
 * and creates the new intent
 */
@Component(immediate = true)
public class ScheduledTask {
    static final Logger LOGGER = LoggerFactory.getLogger(ScheduledTask.class);
    private static Timer timer;
    public static boolean processLock = true;
    private static final String CONGESTION_OCCURED = "CONGESTION_OCCURED";
    private static final String CONGESTION_AVOIDED = "CONGESTION_AVOIDED";

    @Activate
    protected void activate() {
        TimerTask tasknew = new TimerSchedulePeriod();
        timer = new Timer();
        // scheduling the task at interval
        timer.schedule(tasknew, 5000, 5000);
    }

    @Deactivate
    protected void deactivate() {
        timer.cancel();
        timer.purge();
    }
    /**
     *This class will doing the task of time scheduling.
     *
     */
    public class TimerSchedulePeriod extends TimerTask {
        @Override
        public final void run() {
            if (!ScheduledTask.processLock) {
                return;
            }
            ScheduledTask.processLock = false;
            try {
                VideoSelectorService videoSelectorService = VideoSelectorService.getInstance();
                ArrayList<IntentId> congestedIntents = videoSelectorService.findCongestionPath();
                for (IntentId congestedIntent : congestedIntents) {
                    VideoSelector.notifyClient(congestedIntent, CONGESTION_OCCURED);
                    IntentId newIntentId = videoSelectorService.createAlternatePathIntent(congestedIntent, false);
                    VideoSelector.notifyClient(newIntentId, CONGESTION_AVOIDED);
                }
            } catch (Exception e) {
                LOGGER.error("qos - ScheduledTaskSheduler - TimerSchedulePeriod - run() - exception :", e);
            }
            ScheduledTask.processLock = true;
        }
    }
}