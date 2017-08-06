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
 * File Name  : ClientServerCostCompare.java
 * Author     : E.Ravikumaran
 * Date       : 16-Oct-2015
 **/

package org.onosproject.qos.model;

import java.util.Comparator;

/**.
 * * Class to compare two path cost.
 */
public class ClientServerCostCompare implements Comparator<ServerInfo> {

    /**.
     * Comparing two objects
     */
    @Override
    public int compare(ServerInfo serverInfoObj1, ServerInfo serverInfoObj2) {
        if (serverInfoObj1.getCost() < serverInfoObj2.getCost()) {
            return -1;
        } else {
            return 1;
        }
    }
}