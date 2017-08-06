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
 * File Name  : ServerInfo.java
 * Author     : E.Ravikumaran
 * Date       : 13-APR-2016
 **/


package org.onosproject.qos.model;

import org.onosproject.net.Host;
import org.onosproject.net.Path;

/*.
 * To store the server details - Model Data
 */

public class ServerInfo {

    private Host serverInfo;
    private String serverUrl;
    private Path path;
    private double cost = 0.0;

    public ServerInfo(String serverUrl, Host serverInfo) {
        this.serverUrl = serverUrl;
        this.serverInfo = serverInfo;
    }
    public Host getServerInfo() {
        return serverInfo;
    }
    public Path getPath() {
        return path;
    }
    public void setPath(Path path) {
        this.path = path;
    }
    public double getCost() {
        return cost;
    }
    public String getServerUrl() {
        return serverUrl;
    }
    public void setCost(double cost) {
        this.cost = cost;
    }
}