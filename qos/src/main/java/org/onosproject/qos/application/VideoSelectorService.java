/*
 * Copyright 2016 WIPRO Technologies Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
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
 * VideoSelector.java.
 * Author : E.Ravikumaran,Bivas,R.Eswarraj.
 * Date   : 15-Oct-2015
 **/
package org.onosproject.qos.application;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.incubator.net.PortStatisticsService;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.statistic.Load;
import org.onosproject.qos.cache.HostDataCache;
import org.onosproject.qos.cache.IntentDataCache;
import org.onosproject.qos.model.ChannelInfo;
import org.onosproject.qos.model.ClientServerCost;
import org.onosproject.qos.model.ClientServerCostCompare;
import org.onosproject.qos.model.IntentDataModel;
import org.onosproject.qos.model.ServerInfo;
import org.onosproject.qos.onosservices.pathcost.PathSelectorService;
import org.onosproject.qos.rest.client.ServiceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

public class VideoSelectorService {

    static final Logger LOGGER = LoggerFactory.getLogger(VideoSelectorService.class);
    protected static DefaultServiceDirectory serviceDirectory = new DefaultServiceDirectory();

    protected VideoSelectorService(){

    }
    private static VideoSelectorService singleton = null;

    public static VideoSelectorService getInstance() {
        if (singleton == null) {
            singleton = new VideoSelectorService();
        }
        return singleton;
    }

    /**
     * To calculate the cost and links between client and list of servers, set to pojo object.
     * Then it returns the least cost server object to playchannel
     * @param clientServerCostObj - object contains client and list of server data
     * @return - array of costs between client and server
     * @throws IOException
     */
    public ClientServerCost checkCost(ClientServerCost clientServerCostObj) throws IOException {
        HostId clientHostId = clientServerCostObj.getClientInfo().id();
        HostId serverHostId;
        try {
            for (int i = 0; i < clientServerCostObj.getSeverinfo().size(); i++) {
                serverHostId = (clientServerCostObj.getSeverinfo().get(i).getServerInfo().id());
                PathSelectorService pathSelectorService = PathSelectorService.getInstance();
                Set<Path> paths;
                Path path;
                try {
                    paths = pathSelectorService.getOptimalPath(serverHostId, clientHostId);
                    path = paths.iterator().next();
                    clientServerCostObj.getSeverinfo().get(i).setPath(path);
                    clientServerCostObj.getSeverinfo().get(i).setCost(path.cost());
                } catch (Exception ex) {
                    LOGGER.error("Qos - VideoSelectorService - CheckCost - Exception at fetching optimal path- ", ex);
                }
            }
        } catch (Exception e) {
            LOGGER.error("QoS - VideoSelectorService.checkCost - Exception", e);
        }
        return clientServerCostObj;
    }

    /**
     * sort the server according to the cost.
     * @param listServerInfo - array of serverData which contains cost and links
     * @return - sorted list of servers
     */
    public ArrayList<ServerInfo> sortVideoServer(ArrayList<ServerInfo> listServerInfo) {
        Collections.sort(listServerInfo, new ClientServerCostCompare());
        return listServerInfo;
    }

    /**
     * finds the congestion path in the installed Path intents.
     * @return congested path intent ids and its session id
     */
    public ArrayList<IntentId> findCongestionPath() {
        ArrayList<IntentId> congestedPath = new ArrayList<>();
        PortStatisticsService portStatisticsService = serviceDirectory.get(PortStatisticsService.class);
        try {
            for (Link link : IntentDataCache.getLinks()) {
                Load load = portStatisticsService.load(link.src());
                if (load != null && load.rate() > ServiceConstants.CONGESTION_THRESHOLD) {
                    congestedPath.addAll(IntentDataCache.getIntentIds(link));
                }
            }
        } catch (Exception e) {
            LOGGER.error("QoS - VideoSelectorService.findCongestionPath - Exception", e);
        }
        return congestedPath;
    }

    /**
     * This function get triggered when congession occurs,this method filters the congessted path and creates a.
     * forward and reverse intents
     * @param congestedIntentId Congested swithes between the server and client
     */
    public IntentId createAlternatePathIntent(IntentId congestedIntentId, boolean isLinkDown) {
        IntentDataModel intentData = IntentDataCache.getIntentData(congestedIntentId);
        long queueId = intentData.getQueueId();
        try {
            PathSelectorService pathSelectorService = PathSelectorService.getInstance();
            Set<Path> paths;
            if (isLinkDown) {
                    paths = pathSelectorService.getOptimalPath(intentData.getServerFwd(), intentData.getClientFwd());
            } else {
                paths = pathSelectorService.getNonCongestedPaths(intentData.getServerFwd(), intentData.getClientFwd());
            }
            if (paths.size() > 0) {
                Path path = paths.iterator().next();
                IntentId fwdIntentId = PathIntentService.createPathIntent(path, queueId);
                Path reversePath = PathSelectorService.reversePath(path);
                IntentId revIntentId = PathIntentService.createPathIntent(reversePath, queueId);
                if (fwdIntentId != null && revIntentId != null) {
                    PathIntentService.performDelete(intentData.getForwardintentId());
                    PathIntentService.performDelete(intentData.getReverseintentId());
                    IntentDataCache.removeEntry(congestedIntentId);
                    IntentDataCache.addEntry(intentData.getSessionId(), fwdIntentId, revIntentId);
                    return fwdIntentId;
                } else if (fwdIntentId == null && revIntentId == null) {
                    LOGGER.warn("QoS - VideoSelectorService.createAlternatePathIntent - Both Intents Not Created");
                } else if (fwdIntentId == null) {
                    LOGGER.warn("QoS - VideoSelectorService.createAlternatePathIntent - Forward Intent Not Created");
                } else {
                    LOGGER.warn("QoS - VideoSelectorService.createAlternatePathIntent - Reverse Intent Not Created");
                }
            } else {
                return null;
            }

        } catch (Exception expObj) {
            LOGGER.error("QoS - VideoSelectorService.createAlternatePathIntent - Exception", expObj);
        }
        return null;
    }

    /**
     * filter the server urls according to the capacity and server status.
     * @param arrServeruRL arraylist of server unfiltered urls
     * @return arrServerActual filtered list of server urls
     */
    public ArrayList<String> filterServerUrl(ArrayList<String> arrServeruRL) {
        ArrayList<String> arrServerActual = new ArrayList<>();
        try {
            for (int i = 0; i < arrServeruRL.size(); i++) {
                ChannelInfo channelInfo = IntentDataCache.getChannelInfo(arrServeruRL.get(i));
                if ((channelInfo.getCurrCapacity() < channelInfo.getCapacity()) &&
                        (channelInfo.getChnlStatus().equals("play"))) {
                    arrServerActual.add(arrServeruRL.get(i));
                }
            }
            if (arrServerActual.size() == 0) {
                LOGGER.info("QoS - VideoSelectorService.filterServerUrl - " +
                                    "URL Selected when arrServerActual size is 0 " + arrServerActual);
                return arrServeruRL;
            }
        } catch (Exception e) {
            LOGGER.error("QoS - VideoSelectorService.filterServerUrl - Exception", e);
        }
        LOGGER.info("QoS - VideoSelectorService.filterServerUrl - URL Selected -- " + arrServerActual);
        return arrServerActual;
    }

    public static IpAddress fetchClientIp(IntentId intentId) {
        IpAddress clientIP = null;
            try {
                MacAddress temp = IntentDataCache.getIntentData(intentId).getClientFwd().mac();
                Set<IpAddress> ips = HostDataCache.hostHashMap.keySet();
                for (IpAddress ip : ips) {
                    if (HostDataCache.hostHashMap.get(ip).mac() == temp) {
                        clientIP = ip;
                        break;
                    }
                }
            } catch (Exception e) {
                LOGGER.error("QoS - fetchClientIp - Exception", e);
            }
        return clientIP;
    }

    public static void nextServer(String currentSessnId, String currentChnl, IntentId intent) throws JSONException {
        String channelName = IntentDataCache.getChannelInfo(currentChnl).getChannelName();
        ArrayList<String> altServerUrls = IntentDataCache.getAlternateUrls(channelName, currentChnl);
        VideoSelectorService vs = VideoSelectorService.getInstance();
        if (!(altServerUrls.isEmpty())) {
            JSONObject altSrvrData = vs.playAltServer(currentSessnId, intent, altServerUrls);
            IntentId newId = (IntentId) altSrvrData.get("id");
            String newUrl = altSrvrData.getString("NewServerUrl");
            LOGGER.info("QoS - alternate url is : " + newUrl);
            VideoSelector.notifyClient(newId, "SERVER_RESTORED", newUrl);
        } else {
            LOGGER.error("Alternate Server's URL is not found");
        }
    }

    public JSONObject playAltServer(String sessionId, IntentId intentId, ArrayList<String> unfilteredServerUrls) {
        ServerInfo bestServerObj;
        JSONObject respObj = new JSONObject();

        try {
            IpAddress clientIP = fetchClientIp(intentId);

            IntentDataModel intentDataModel  = IntentDataCache.getIntentData(sessionId);
            Host clientInfo  = HostDataCache.getHostInfo(clientIP);
            ClientServerCost clientServerObj = new ClientServerCost(clientInfo);

            String serverIP;
            String serverUrl;
            Host serverObj;
            ArrayList<String> arrServeruRL = filterServerUrl(unfilteredServerUrls);

            for (int i = 0; i < arrServeruRL.size(); i++) {
                serverUrl = arrServeruRL.get(i);
                serverIP = serverUrl.substring(serverUrl.indexOf("/") + 2, serverUrl.lastIndexOf(":"));
                serverObj = HostDataCache.getHostInfo(IpAddress.valueOf(serverIP));
                ServerInfo serverInfo = new ServerInfo(serverUrl, serverObj);
                clientServerObj.addServerInfo(serverInfo);
            }
            clientServerObj = checkCost(clientServerObj);
            ArrayList<ServerInfo> listServerInfo = sortVideoServer(clientServerObj.getSeverinfo());
            bestServerObj = listServerInfo.get(0);
            IntentDataCache.storeSessnChnl(sessionId, bestServerObj.getServerUrl());
            Path bestPath =  bestServerObj.getPath();
            MacAddress clientMac = clientInfo.mac();
            IntentId oldintentKeyToRemove = null;
            IntentDataModel intentData;
            ArrayList<IntentId> intentsToBeDel = new ArrayList<>();
            intentData = IntentDataCache.getIntentData(sessionId);

            if ((intentData != null) && (clientMac.equals(intentData.getClientFwd().mac()))) {
                intentsToBeDel.add(intentData.getForwardintentId());
                intentsToBeDel.add(intentData.getReverseintentId());
                oldintentKeyToRemove = intentData.getForwardintentId();
            }

            IntentId forwardintentid = PathIntentService.createPathIntent(bestPath, intentDataModel.getQueueId());
            Path reversePath = PathSelectorService.reversePath(bestPath);
            IntentId reverseintentid = PathIntentService.createPathIntent(reversePath, intentDataModel.getQueueId());

            if (forwardintentid != null && reverseintentid != null) {
                respObj.put("id", forwardintentid);
                IntentDataCache.addEntry(sessionId, forwardintentid, reverseintentid);
                ChannelInfo channelInfo = IntentDataCache.getChannelInfo(bestServerObj.getServerUrl());
                channelInfo.setSessionId(sessionId);
                for (IntentId intentid : intentsToBeDel) {
                    PathIntentService.performDelete(intentid);
                }
                IntentDataCache.removeEntry(oldintentKeyToRemove);
            } else if (forwardintentid == null && reverseintentid == null) {
                LOGGER.warn("QoS - VideoSelector.playChannel - Both Intents Not Created");
            } else if (forwardintentid == null) {
                LOGGER.warn("QoS - VideoSelector.playChannel - Forward Intent Not Created");
            } else {
                LOGGER.warn("QoS - VideoSelector.playChannel - Reverse Intent Not Created");
            }
            respObj.put("NewServerUrl", bestServerObj.getServerUrl());
        } catch (Exception e) {
            LOGGER.error("QoS - VideoSelector.playChannel - Exception", e);
        }
        return respObj;
    }
}