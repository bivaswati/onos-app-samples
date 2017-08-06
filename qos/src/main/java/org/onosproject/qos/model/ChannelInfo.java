package org.onosproject.qos.model;


public class ChannelInfo {
    private static String chnlStatus = "stop";
    private String channelName;
    private String serverUrl;
    private int capacity = 0;
    private int currCapacity = 0;
    private String sessionId = null;


    public ChannelInfo(String channelName, String serverUrl, int capacity) {
        this.channelName = channelName;
        this.serverUrl = serverUrl;
        this.capacity = capacity;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getCurrCapacity() {
        return currCapacity;
    }

    public void setCurrCapacity(int currCapacity) {
        this.currCapacity = currCapacity;
    }

    public String getChannelName() {
        return channelName;
    }
    public String getChnlStatus() {
        return chnlStatus;
    }

    public void setChnlStatus(String chnlStatus) {
        this.chnlStatus = chnlStatus;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
