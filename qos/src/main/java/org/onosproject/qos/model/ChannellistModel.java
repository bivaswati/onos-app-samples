package org.onosproject.qos.model;

import java.util.ArrayList;


public class ChannellistModel {
    private String channelName;
    private ArrayList<String> chInfo;

    public ChannellistModel(String channelName) {
        this.channelName = channelName;
        this.chInfo = new ArrayList<>();
    }

    public ArrayList<String> getChInfo() {
        return chInfo;
    }

    public void addChInfo(String chInfo) {
        this.chInfo.add(chInfo);
    }
}
