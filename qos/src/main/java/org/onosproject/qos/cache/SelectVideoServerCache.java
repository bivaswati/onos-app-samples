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
/**
 * SelectVideoServerCache.java
 * Author : E.Ravikumaran
 * Date   : 15-Oct-2015
 **/
package org.onosproject.qos.cache;

import org.onosproject.qos.model.ClientServerCost;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * pojo class for the setting the best video server in the cache.
 **/
public class SelectVideoServerCache {

    protected SelectVideoServerCache(){

    }
    public static HashMap<String, ArrayList<ClientServerCost>> clientToVideoServerObj =
            new HashMap<String, ArrayList<ClientServerCost>>();
    public static HashMap<String, Integer> indexClientToVideoServerObj = new HashMap<String, Integer>();

    /**
     * To get the client to video server object.
     */
    public static ArrayList<ClientServerCost> getClientToVideoServerObj(
            String clientIP) {
        return clientToVideoServerObj.get(clientIP);
    }

    /**
     * To set the best server object.
     */
    public static void setClientToVideoServerObj(String clientIP,
                                                 ArrayList<ClientServerCost> clientToVideoServerObj) {
        SelectVideoServerCache.clientToVideoServerObj.put(clientIP,
                                                          clientToVideoServerObj);
    }

    /**
     * TO set the Client to video server object and index of the server object.
     */
    public static void setIndexClientToVideoServerObj(String clientIP,
                                                      Integer index) {
        SelectVideoServerCache.indexClientToVideoServerObj.put(clientIP, index);
    }
}