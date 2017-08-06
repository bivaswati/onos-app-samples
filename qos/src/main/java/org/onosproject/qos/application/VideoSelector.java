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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Host;
import org.onosproject.net.intent.IntentId;
import org.onosproject.qos.cache.HostDataCache;
import org.onosproject.qos.cache.IntentDataCache;
import org.onosproject.qos.model.ChannelInfo;
import org.onosproject.qos.model.ChannellistModel;
import org.onosproject.qos.model.ClientServerCost;
import org.onosproject.qos.model.IntentDataModel;
import org.onosproject.qos.model.ServerInfo;
import org.onosproject.qos.onosservices.pathcost.PathSelectorService;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Class has methods to get client side data, selects the best video server and to create intents.
 */
@javax.ws.rs.Path("/sdnc")
@Component(immediate = true)
public class VideoSelector extends AbstractWebResource {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    long queuePrityId;
    private static ApplicationId appId;
    static final Logger LOGGER = LoggerFactory.getLogger(VideoSelector.class);
    public static final String URL = "http://10.0.0.1:8080/QOSUI-1/rest/sdncoe/";
    public static final String FETCH_CHNL_URL = URL + "getChannelList";
    public static final String EVENT_URL = URL + "notifyEvent";

    public static final String OPEN = "open";
    public static final String STOP = "stop";
    public static final String PLAY = "play";

    /**
     * To activate the application on ONOS.
     */
    @Activate
    protected void activate() {
        appId = coreService.registerApplication("org.onosproject.qos");
        getChannelList();
    }
    @Deactivate
    protected void deactivate() {
        //Delete all the application created intents
        for (IntentId id: IntentDataCache.getIntentIds()) {
            PathIntentService.performDelete(IntentDataCache.getIntentData(id).getForwardintentId());
            PathIntentService.performDelete(IntentDataCache.getIntentData(id).getReverseintentId());
        }
        //Clear all the cache
        IntentDataCache.clearAll();
    }
    public static ApplicationId getAppId() {
      return appId;
    }
    /**
     *
     * @param stream - array of server data from client application.
     * @return - serverUrl
     * @throws JSONException
     */
    @POST
    @Path("/PlayChannel")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response playChannel(InputStream stream) throws JSONException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        JSONObject inputJsonObj = new JSONObject(mapper.readTree(stream).toString());
        JSONObject respObj = new JSONObject();
        boolean blnIntentExists = false;
        boolean isDeleted = false;
        long defaultQueueId = 1;
        try {
            String sessionId = inputJsonObj.getString("sessionId");
            this.queuePrityId = Long.parseLong(inputJsonObj.getString("queueID"));
            String clientIP = inputJsonObj.getString("clientIP");
            String  chName = inputJsonObj.getString("chnlName");
            ArrayList<String> arrServeruRL = IntentDataCache.getChannelList(chName);
            ServerInfo bestServerObj;
            Host clientInfo  = HostDataCache.getHostInfo(IpAddress.valueOf(clientIP));
            ClientServerCost clientServerObj = new ClientServerCost(clientInfo);

            if (this.queuePrityId == 0)  {
                String serverUrl = IntentDataCache.getServerUrl(sessionId);
                String serverIP = serverUrl.substring(serverUrl.indexOf("/") + 2, serverUrl.lastIndexOf(":"));
                Host serverObj = HostDataCache.getHostInfo(IpAddress.valueOf(serverIP));
                bestServerObj = new ServerInfo(serverUrl, serverObj);
                clientServerObj.addServerInfo(bestServerObj);
                VideoSelectorService.getInstance().checkCost(clientServerObj);

            } else {
                VideoSelectorService videoSelctorService = VideoSelectorService.getInstance();
                arrServeruRL = videoSelctorService.filterServerUrl(arrServeruRL);
                try {
                    String serverIP;
                    String serverUrl;
                    Host serverObj;
                    for (int i = 0; i < arrServeruRL.size(); i++) {
                        serverUrl = arrServeruRL.get(i);
                        serverIP = serverUrl.substring(serverUrl.indexOf("/") + 2, serverUrl.lastIndexOf(":"));
                        serverObj = HostDataCache.getHostInfo(IpAddress.valueOf(serverIP));
                        ServerInfo serverInfo = new ServerInfo(serverUrl, serverObj);
                        clientServerObj.addServerInfo(serverInfo);
                    }
                } catch (Exception expObj) {
                    LOGGER.error("QoS - VideoSelector.playChannel - Exception", expObj);
                }
                VideoSelectorService videoSelectorService = VideoSelectorService.getInstance();
                clientServerObj = videoSelectorService.checkCost(clientServerObj);
                ArrayList<ServerInfo> listServerInfo =
                        videoSelectorService.sortVideoServer(clientServerObj.getSeverinfo());
                bestServerObj = listServerInfo.get(0);
            }
            IntentDataCache.storeSessnChnl(sessionId, bestServerObj.getServerUrl());

            org.onosproject.net.Path bestPath =  bestServerObj.getPath();
            MacAddress clientMac = clientInfo.mac();
            MacAddress serverMac = bestServerObj.getServerInfo().mac();
            long strQueuePrtyChk = 0;
            IntentId oldintentKeyToRemove = null;
            IntentDataModel intentData;
            ArrayList<IntentId> intentsToBeDel = new ArrayList<>();
            try {
                intentData = IntentDataCache.getIntentData(sessionId);
                if ((intentData != null) && (clientMac.equals(intentData.getClientFwd().mac()) &&
                        serverMac.equals(intentData.getServerFwd().mac()))) {
                    blnIntentExists = true;
                    strQueuePrtyChk = intentData.getQueueId();
                    intentsToBeDel.add(intentData.getForwardintentId());
                    intentsToBeDel.add(intentData.getReverseintentId());
                    oldintentKeyToRemove = intentData.getForwardintentId();
                }
            } catch (Exception expObj) {
                blnIntentExists = false;
                strQueuePrtyChk = defaultQueueId;
            }
            if ((this.queuePrityId == 1  && !blnIntentExists) || ((this.queuePrityId == 0 && (strQueuePrtyChk == 1)))) {
                //Creates the intents between the client and server
                IntentId forwardintentid = PathIntentService.createPathIntent(bestPath, this.queuePrityId);

                // Create Reverse Intent
                org.onosproject.net.Path reversePath = PathSelectorService.reversePath(bestPath);

                //List of links in reverse order
                IntentId reverseintentid = PathIntentService.createPathIntent(reversePath, this.queuePrityId);

                if (forwardintentid != null && reverseintentid != null) {
                    IntentDataCache.addEntry(sessionId, forwardintentid, reverseintentid);
                    ChannelInfo channelInfo = IntentDataCache.getChannelInfo(bestServerObj.getServerUrl());
                    channelInfo.setSessionId(sessionId);
                    // Delete old intent
                    for (IntentId intentid : intentsToBeDel) {
                        isDeleted = PathIntentService.performDelete(intentid);
                    }
                    IntentDataCache.removeEntry(oldintentKeyToRemove);
                    LOGGER.info("QoS - VideoSelector.playChannel - Intent Deleted Status :: " + isDeleted);
                } else if (forwardintentid == null && (reverseintentid == null)) {
                    LOGGER.warn("QoS - VideoSelector.playChannel - Both Intents Not Created");
                } else if (forwardintentid == null) {
                    LOGGER.warn("QoS - VideoSelector.playChannel - Forward Intent Not Created");
                } else {
                    LOGGER.warn("QoS - VideoSelector.playChannel - Reverse Intent Not Created");
                }
            }
            respObj.put("chnlServer", bestServerObj.getServerUrl());
        } catch (Exception e) {
            LOGGER.error("QoS - VideoSelector.playChannel - Exception", e);
        }
        return ok(respObj).build();
    }

    /**
     * @param stream session
     * @return Deltete Intent Status
     * @throws JSONException IOException
     */
    @POST
    @Path("/deleteSession")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public static Response deleteSession(InputStream stream) throws JSONException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        JSONObject sessionObj = new JSONObject(mapper.readTree(stream).toString());
        String sessionId = sessionObj.getString("sessionId");
        JSONObject deletedIntentStatus = new JSONObject();
        IntentDataModel objIntentData;
        try {
            objIntentData = IntentDataCache.getIntentData(sessionId);
            boolean fwdStatus = PathIntentService.performDelete(objIntentData.getForwardintentId());
            boolean revStatus = PathIntentService.performDelete(objIntentData.getReverseintentId());
            if (fwdStatus && revStatus) {
                deletedIntentStatus.put("status", "All the specified intents deleted");
                IntentDataCache.removeEntry(objIntentData.getForwardintentId());
            } else {
                deletedIntentStatus.put("status", "Intents not deleted");
                LOGGER.info("QoS -VS-dltSession- Fwd/Rev Delete Status " + fwdStatus + " : " + revStatus);
            }
        } catch (Exception ex) {
            LOGGER.error("QoS - VideoSelector.deleteSession - Exception", ex);
        }
        IntentDataCache.removeServerUrl(sessionId);
    return ok(deletedIntentStatus).build();
    }

    /**
     * @param request Http request
     * @param stream server staus as event
     * @throws JSONException IOException
     */
    @POST
    @Path("/postServerEvent")
    @Consumes(MediaType.APPLICATION_JSON)
    public static void postServerEvent(@Context HttpServletRequest request, InputStream stream)
            throws JSONException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        JSONObject inputObj = new JSONObject(mapper.readTree(stream).toString());
        String serverEvent = inputObj.getString("serverEvent");
        String serverIp = request.getRemoteAddr();
        String serverPort = inputObj.getString("port");
        String chnlUrl = "http://" + serverIp + ":" + serverPort + "/";
        LOGGER.info("\nURL : " + chnlUrl + "\nStatusEvent : " + serverEvent);
        ChannelInfo curChannelInfo;
        switch (serverEvent.trim()) {
            case OPEN:
                LOGGER.info("File is Loading to Server " + serverIp);
                break;
            case STOP:
                    String sessionId = IntentDataCache.getChannelInfo(chnlUrl).getSessionId();
                    IntentDataCache.getChannelInfo(chnlUrl).setSessionId(null);
                    if (sessionId == null) {
                        LOGGER.info("No Client is Using Server " + serverIp);
                        return;
                    }
                    IntentId intentId = null;
                    try {
                        intentId = IntentDataCache.getIntentData(sessionId).getForwardintentId();
                    } catch (Exception ex) {
                        LOGGER.info("qos - postServerEvent - Error " + ex.toString());
                    }
                    notifyClient(intentId, "SERVER_DOWN");
                    curChannelInfo = IntentDataCache.getChannelInfo(chnlUrl);
                    curChannelInfo.setChnlStatus(STOP);
                    VideoSelectorService.nextServer(sessionId, chnlUrl, intentId);
                break;
            case PLAY:
                LOGGER.info("In Play");
                curChannelInfo = IntentDataCache.getChannelInfo(chnlUrl);
                curChannelInfo.setChnlStatus(PLAY);
                break;
            default:
                break;
        }
    }

    public static synchronized void notifyClient(IntentId intentid, String... event) {
        IpAddress clientIP;
        if (intentid != null) {
            try {
                clientIP = VideoSelectorService.fetchClientIp(intentid);
            } catch (Exception e) {
                LOGGER.error("QoS - VideoSelector.notifyClient error ", e);
                return;
            }
        } else {
            LOGGER.error("QoS - VideoSelector.notifyClient - IntentId is NULL ");
            return;
        }

        try {
            JSONObject jobjMsg = new JSONObject();
            jobjMsg.put("clientIP", clientIP);
            if (event[0].equals("SERVER_RESTORED")) {
                jobjMsg.put("event", event[0]);
                jobjMsg.put("NewServerUrl", event[1]);
            } else {
                jobjMsg.put("event", event[0]);
            }
            Client client = Client.create();
            WebResource webResource = client.resource(EVENT_URL);
            ClientResponse response = webResource.accept("text/plain").post(ClientResponse.class, jobjMsg.toString());
            if (response.getStatus() != 200) {
                LOGGER.error("QoS - VideoSelector.notifyClient: HTTP error code : " + response.getStatus());
            }
        } catch (Exception e) {
            LOGGER.error("QoS - VideoSelector.notifyClient - Exception while notifying to client ", e);
        }
    }

    public static synchronized void getChannelList() {
        try {
            Client client = Client.create();
            WebResource webResource = client.resource(FETCH_CHNL_URL);
            ClientResponse response = webResource.accept("text/plain").get(ClientResponse.class);
            if (response.getStatus() != 200) {
                LOGGER.error("QoS - VideoSelector.getChannelList : HTTP error code : " + response.getStatus());
            } else {
                String data = response.getEntity(String.class);
                JSONObject jObjOut = new JSONObject(data);
                populateChannelList(jObjOut);
            }
        } catch (Exception e) {
            LOGGER.error("QoS - VideoSelector.getChannelList - Exception ", e);
        }
    }

    private static void populateChannelList(JSONObject jObjOut) throws JSONException {
        try {
            JSONArray jArrChList = jObjOut.getJSONArray("channelList");
            for (int i = 0; i < jArrChList.length(); i++) {
                String chName = jArrChList.getJSONObject(i).getString("channelName");
                JSONArray jArrChUrls = jArrChList.getJSONObject(i).getJSONArray("urls");
                ChannellistModel chnModel = new ChannellistModel(chName);
                for (int j = 0; j < jArrChUrls.length(); j++) {
                    String jurl = jArrChUrls.get(j).toString();
                    JSONObject jourl = new JSONObject(jurl);
                    String url = jourl.getString("url");
                    int capacity = Integer.valueOf(jArrChUrls.getJSONObject(j).getString("capacity"));
                    ChannelInfo chInfo = new ChannelInfo(chName, url, capacity);
                    IntentDataCache.addChnlInfo(url, chInfo);
                    chnModel.addChInfo(url);
                }
                IntentDataCache.addChnlList(chName, chnModel);
            }
        } catch (Exception e) {
            LOGGER.error("QoS - VideoSelector.populateChannelList - Exception", e);
        }
    }
}

