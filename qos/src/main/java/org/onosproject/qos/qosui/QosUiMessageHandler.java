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
 * File Name : QosUiMessageHandler.java
 * Author    : Bivas
 * Date      : 2-April-2016
 **/

package org.onosproject.qos.qosui;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.onosproject.net.intent.IntentId;
import org.onosproject.qos.application.PathIntentService;
import org.onosproject.qos.cache.IntentDataCache;
import org.onosproject.qos.model.IntentDataModel;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * QOS UI message handler.It captures the events sent from client-side.
 * And triggers specific actions according to the event
 */
public class QosUiMessageHandler extends UiMessageHandler {

    private static final String QOSUI_DATA_REQ = "qosuiDataRequest";
    private static final String QOSUI_DATA_RESP = "qosuiDataResponse";
    private static final String QOSUIS = "qosuis";

    private static final String INTENT_MGMT_REQ = "intentManagementRequest";

    private static final String SESSIONID = "sid";
    private static final String SMAC = "smac";
    private static final String CMAC = "cmac";
    private static final String QID = "qid";
    private static final String FID = "id";
    private static final String RID = "rid";

    private static final String[] COLUMN_IDS = {SESSIONID, SMAC, CMAC, QID, FID, RID};

    private final Logger log = LoggerFactory.getLogger(getClass());

    //provide request handler implementations , for specific event types from our view:
    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new QosuiDataRequestHandler(),
                new IntentMgmtRequest()
        );
    }
    // handler for ui table requests
    private final class QosuiDataRequestHandler extends TableRequestHandler {

        private QosuiDataRequestHandler() {
            super(QOSUI_DATA_REQ, QOSUI_DATA_RESP, QOSUIS);
        }

        @Override
        protected String[] getColumnIds() {
            return COLUMN_IDS;
        }

        @Override
        protected void populateTable(TableModel tm, ObjectNode payload) {
            for (IntentId intentId: IntentDataCache.getIntentIds()) {
                populateRow(tm.addRow(), IntentDataCache.getIntentData(intentId));
            }
        }
    //Populate the datas to be displayed by row-wise under specific columns
        private void populateRow(TableModel.Row row, IntentDataModel intentDataModel) {
            row.cell(SESSIONID, intentDataModel.getSessionId())
                    .cell(SMAC, intentDataModel.getServerFwd().mac())
                    .cell(CMAC, intentDataModel.getClientFwd().mac())
                    .cell(QID,  intentDataModel.getQueueId())
                    .cell(FID, intentDataModel.getForwardintentId())
                    .cell(RID, intentDataModel.getReverseintentId());
        }
        @Override
        protected String noRowsMessage(ObjectNode payload) {
            return "No Intents Row Found";
        }
    }

    /*
    Handler for Delete control button  of Intents data Row in UI.The constructor takes the name
    of the event to be handled.At run time, an instance of this handler is bound to the event name
    and the process() method invoked each time such an event is received from the client
    */
    private final class IntentMgmtRequest extends RequestHandler {

        private IntentMgmtRequest() {
            super(INTENT_MGMT_REQ);
        }
        @Override
        public void process(long sid, ObjectNode payload) {
            String action = string(payload, "action");
            String fid = string(payload, "name");
            if (action != null && fid != null) {
                if (action.equals("REMOVE")) {
                    try {
                        IntentDataModel intentDataModel = IntentDataCache
                                .getIntentData(IntentId.valueOf(Long.decode(fid)));
                        boolean fwdIntDelStatus = PathIntentService.performDelete(intentDataModel.getForwardintentId());
                        boolean revIntDelStatus = PathIntentService.performDelete(intentDataModel.getReverseintentId());
                        if (fwdIntDelStatus && revIntDelStatus) {
                            IntentDataCache.removeEntry(intentDataModel.getForwardintentId());
                        } else {
                            log.error("qos -QosUiMessageHandler-IntentMgmtRequest-" +
                                              "Error in PathIntentService.performDelete()");
                        }
                    } catch (Exception e) {
                        log.error("qos -QosUiMessageHandler.IntentMgmtRequest-Exception" +
                                          "-Could n't delete intents ids ", e);
                    }
                } else {
                    log.warn("qos -QosUiMessageHandler -IntentMgmtRequest - " +
                                     "Required action Not Found.Check with action name");
                }
                chain(QOSUI_DATA_REQ, sid, payload);
            } else {
                log.error("qos -QosUiMessageHandler -IntentMgmtRequest - Required action and id not sent from ui");
            }
        }
    }
}