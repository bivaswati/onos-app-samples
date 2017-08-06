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
 * File Name   : IntentDataModel.java
 * Author      : E.Ravikumaran,Bivas
 * Date        : 15-Oct-2015
 **/

package org.onosproject.qos.model;

import org.onosproject.net.HostId;
import org.onosproject.net.Path;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.intent.PathIntent;

import static com.google.common.base.Preconditions.checkNotNull;

/**.
 * Class to store as set of intent (Fwd & Rev) data.
 * Has getter and setter methods
 */
public class IntentDataModel {
    private HostId serverFwd;
    private HostId clientFwd;
    private long queueId;
    private IntentId forwardintentId;
    private IntentId reverseintentId;
    private String sessionId;
    private Path fwdpath;

    public IntentDataModel(PathIntent fwdIntent, PathIntent revIntent, String sessionId) {
        checkNotNull(fwdIntent);
        checkNotNull(revIntent);
        this.serverFwd = fwdIntent.path().links().get(0).src().hostId();
        this.clientFwd = fwdIntent.path().links().get(fwdIntent.path().links().size() - 1)
                .dst().hostId();
        fwdIntent.treatment().allInstructions().stream()
                .filter(instruction -> instruction instanceof Instructions.SetQueueInstruction)
                .forEach(instruction -> this.queueId
                        = ((Instructions.SetQueueInstruction) instruction).queueId());
        this.forwardintentId = fwdIntent.id();
        this.reverseintentId = revIntent.id();
        this.sessionId = sessionId;
        this.fwdpath = fwdIntent.path();
    }

    public IntentDataModel() {

    }

    /**.
     * Gets Foward Intent Id
     * @return Forward Intent Id as String
     */
    public IntentId getForwardintentId() {
        return forwardintentId;
    }

    /**.
     * Gets Reverse Intent Id
     * @return Reverse Intent Id
     */
    public IntentId getReverseintentId() {
        return reverseintentId;
    }

    /**.
     * Gets Forward Server
     * @return Forward Server
     */
    public HostId getServerFwd() {
        return serverFwd;
    }

    /**.
     * Gets Forward Client
     * @return Forward Client
     */
    public HostId getClientFwd() {
        return clientFwd;
    }

    /**.
     * Gets Queue ID
     * @return Queue ID
     */
    public long getQueueId() {
        return queueId;
    }

    public String getSessionId() {
        return this.sessionId;
    }

    public Path getFwdPath() {
        return this.fwdpath;
    }
    /**.
     * Method to display the Intent DB data stored in this class.
     * @return Intent values as String
     */
    public String toString() {
        return "server_Fwd   :: " + serverFwd + "\n" +
               "client_Fwd   :: " + clientFwd + "\n" +
               "queueId         :: " + queueId + "\n" +
               "forwardintentId :: " + forwardintentId + "\n" +
               "reverseintentId :: " + reverseintentId + "\n" +
               "Session Id :: " + sessionId;
    }

}