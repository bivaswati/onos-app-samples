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
 * File Name : HostDataCache.java
 * Author    : E.Ravikumaran
 * Date      : 15-Oct-2015
 **/
package org.onosproject.qos.cache;


import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.Host;
import org.onosproject.net.host.HostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Set;

/**.
 * Class acts as Cache to store Host Data Objects
 * Has getter and setter methods
 */
public class HostDataCache {
    static final Logger LOGGER = LoggerFactory.getLogger(HostDataCache.class);
    public static HashMap<IpAddress, Host> hostHashMap = new HashMap<>();
    public static HashMap<MacAddress, IpAddress> hostIPMap = new HashMap<>();

    protected HostDataCache() {

    }
    /**
     * To get the host details from the ONOS and update the cache.
     */
    public static void updateHostDataCache() {
        ServiceDirectory serviceDirectory = new DefaultServiceDirectory();
        HostService hostService = serviceDirectory.get(HostService.class);
        Iterable<Host> hosts = hostService.getHosts();
        IpAddress hostIP;
        for (Host host : hosts) {
            Set<IpAddress> ips = host.ipAddresses();
            hostIP = null;
            for (IpAddress ip : ips) {
                try {
                    if (!ip.isZero()) {
                        hostIP = ip;
                        break;
                    }
                } catch (NullPointerException e) {
                    LOGGER.info("QoS - HostDataCache.updateHostDataCache - Exception", e);
                }
            }
            if (hostIP == null) {
                continue;
            }
            hostHashMap.put(hostIP, host);
            hostIPMap.put(host.mac(), hostIP);
        }
    }

    /**
     * Return back the Host information for particular ip received.
     * @param ip  address received
     * @return Host information
     */
    public static Host getHostInfo(IpAddress ip) {
        return hostHashMap.get(ip);
    }
}