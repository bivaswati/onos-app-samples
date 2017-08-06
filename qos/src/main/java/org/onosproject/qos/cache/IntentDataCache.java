/**
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
/**.
 * File Name : IntentDataCache.java
 * Author    : Bivas ,Shahab ,E.Ravikumaran
 * Date      : 15-Oct-2015
 **/
package org.onosproject.qos.cache;

import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.intent.PathIntent;
import org.onosproject.qos.application.PathIntentService;
import org.onosproject.qos.model.ChannelInfo;
import org.onosproject.qos.model.ChannellistModel;
import org.onosproject.qos.model.IntentDataModel;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**.
 * Class acts as Cache to store all the IntentDB which has intent data .
 * Has getter and setter methods
 */
public class IntentDataCache {

    protected IntentDataCache(){
    }

    /**
     * Creats an Hashmap having key as intentid and value as IntentdataModel.
     */
    private static ConcurrentHashMap<IntentId, IntentDataModel> hmIntentData = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, IntentId> hmSessionData = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Link, CopyOnWriteArrayList<IntentId>> hmLinkData = new ConcurrentHashMap<>();
    // ServerUrl, Channel Info (ChannelName, Status, capacity)
    private static ConcurrentHashMap<String, ChannelInfo> hmChnInfo = new ConcurrentHashMap<>();
    //ChannelName, ChannelModel (ChannelName ChannelUrls)
    private static ConcurrentHashMap<String, ChannellistModel> hmChnList = new ConcurrentHashMap<>();
    //SessionId, ServerUrl
    private static ConcurrentHashMap<String, String> hmSessnChnl = new ConcurrentHashMap<>();

    private static final String INCREMENT = "inc";
    private static final String DECREMENT = "dec";

    public static void addEntry(String sessionId, IntentId fwdIntentId, IntentId revIntentId) {
        updateCapacity(sessionId, INCREMENT);
        if (hmIntentData.containsKey(fwdIntentId)) {
            return;
        }
        PathIntent fwdIntent = PathIntentService.getPathIntent(fwdIntentId);
        PathIntent revIntent = PathIntentService.getPathIntent(revIntentId);
        IntentDataModel intentDataModel = new IntentDataModel(fwdIntent, revIntent, sessionId);
        hmSessionData.put(sessionId, fwdIntentId);
        hmIntentData.put(fwdIntentId, intentDataModel);
        assert fwdIntent != null;
        for (Link link : fwdIntent.path().links()) {
            if (hmLinkData.get(link) == null) {
                hmLinkData.put(link, new CopyOnWriteArrayList<>());
            }
            hmLinkData.get(link).add(fwdIntentId);
        }
    }

    public static void removeEntry(IntentId fwdIntentId) {
        try {
                if (fwdIntentId == null) {
                    LoggerFactory.getLogger(IntentDataCache.class)
                    .debug("QoS - IntentDataCache.removeEntry - Null error Exception in IntentDataCache");
                    return;
                }
                if (!hmIntentData.containsKey(fwdIntentId)) {
                    return;
                }
                String sessionId = hmIntentData.get(fwdIntentId).getSessionId();
                Path fwdPath = hmIntentData.get(fwdIntentId).getFwdPath();

                updateCapacity(sessionId, DECREMENT);

                for (Link link : fwdPath.links()) {
                    hmLinkData.get(link).remove(fwdIntentId);
                    if (hmLinkData.get(link).size() == 0) {
                        hmLinkData.remove(link);
                    }
                }
                hmIntentData.remove(fwdIntentId);
                if (hmSessionData.get(sessionId).equals(fwdIntentId)) {
                    hmSessionData.remove(sessionId);
                }

        } catch (Exception e) {
               LoggerFactory.getLogger(IntentDataCache.class).error("QoS - IntentDataCache.removeEntry - Exception", e);
        }

    }

    private static void updateCapacity(String sessionId, String action) {
        String currentserverurl = hmSessnChnl.get(sessionId);
        ChannelInfo chinfo = hmChnInfo.get(currentserverurl);
        if (action.equals(INCREMENT)) {
            chinfo.setCurrCapacity(chinfo.getCurrCapacity() + 1);
        }
        if (action.equals(DECREMENT)) {
            chinfo.setCurrCapacity(chinfo.getCurrCapacity() - 1);
        }
    }


    public static void clearAll() {
        hmSessionData.clear();
        hmLinkData.clear();
        hmIntentData.clear();
        hmChnInfo.clear();
        hmChnList.clear();
        hmSessnChnl.clear();
    }
    public static Set<IntentId> getIntentIds() {
        return hmIntentData.keySet();
    }
    public static CopyOnWriteArrayList<IntentId> getIntentIds(Link link) {
        return hmLinkData.get(link);
    }

    public static IntentDataModel getIntentData(IntentId intentId) {
        return hmIntentData.get(intentId);
    }

    public static IntentDataModel getIntentData(String sessionId) {
        return hmIntentData.get(hmSessionData.get(sessionId));
    }

    public static Set<Link> getLinks() {
        return hmLinkData.keySet();
    }

    public static ArrayList<String> getChannelList(String channelName) {
        return hmChnList.get(channelName).getChInfo();
    }

    public static void addChnlInfo(String serverUrl, ChannelInfo chnlInfo) {
        hmChnInfo.put(serverUrl, chnlInfo);
    }

    public static void addChnlList(String channelName, ChannellistModel chnlModel) {
        hmChnList.put(channelName, chnlModel);
    }

    public static ChannelInfo getChannelInfo(String url) {
        return hmChnInfo.get(url);
    }

    public static void storeSessnChnl(String sessionId, String serveruRL) {
        hmSessnChnl.put(sessionId, serveruRL);
     }

    public static String getServerUrl(String sessionId) {
         return hmSessnChnl.get(sessionId);
    }

    public static void removeServerUrl(String sessionId) {
        hmSessnChnl.remove(sessionId);
    }
    public static String getSessnbyIntent(IntentId intentId) {
        String currsessnbyIntent = null;
        for (Map.Entry<String, IntentId> entry : hmSessionData.entrySet()) {
            if (entry.getValue().equals(intentId)) {
                currsessnbyIntent = entry.getKey();
                break;
            }
        }
        return currsessnbyIntent;
    }

    public static String fetchCurrurl(String currSessn) {
        return hmSessnChnl.get(currSessn);
    }

    public static ArrayList<String> getAlternateUrls(String channelName, String currUrl) {
        ArrayList<String> alternateUrls = hmChnList.get(channelName).getChInfo();
        ArrayList<String> tmpAlternateUrls = new ArrayList<>();
        for (int i = 0; i < alternateUrls.size(); i++) {
            if (!(alternateUrls.get(i).equals(currUrl))) {
                tmpAlternateUrls.add(alternateUrls.get(i));
            }
        }
        return tmpAlternateUrls;
    }
}