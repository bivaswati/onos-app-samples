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
 * File Name  : ClientServerCost.java
 * Author     : E.Ravikumaran
 * Date       : 10-Oct-2015
 **/


package org.onosproject.qos.model;

import org.onosproject.net.Host;

import java.util.ArrayList;

/*.
 * To Store Server,Client Data, cost and link details between the server and client - Model Data
 */

public class ClientServerCost {
    Host clientInfo;
    ArrayList<ServerInfo> severInfo;

    public ClientServerCost(Host clientInfo) {
        this.clientInfo = clientInfo;
    }
    public Host getClientInfo() {
        return clientInfo;
    }

    public ArrayList<ServerInfo> getSeverinfo() {
        return severInfo;
    }
    public void addServerInfo(ServerInfo serverInfo) {
        if (this.severInfo == null) {
            this.severInfo = new ArrayList<>();
        }
        this.severInfo.add(serverInfo);
    }
}