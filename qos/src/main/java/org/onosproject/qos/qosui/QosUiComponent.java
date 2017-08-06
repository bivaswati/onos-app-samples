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
 * File Name : QosUiComponent.java
 * Author    : Bivas
 * Date      : 2-April-2016
 **/

package org.onosproject.qos.qosui;

import com.google.common.collect.ImmutableList;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.ui.UiExtension;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiMessageHandlerFactory;
import org.onosproject.ui.UiView;
import java.util.List;

/**
 * QOS UI application component.
 */
@Component(immediate = true)
public class QosUiComponent {

    //Provides access to the UI Extension Service, so that we can register our "view".
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected UiExtensionService uiExtensionService;

    private static final String QOSUI_VIEW_ID = "qosui";
    private static final String QOSUI_VIEW_TEXT = "QOSUI";

    // List of application views
    private final List<UiView> uiViews = ImmutableList.of(
            new UiView(UiView.Category.OTHER, QOSUI_VIEW_ID, QOSUI_VIEW_TEXT)
    );

    /*
    Factory for UI message handlers.
    Declaration of a UiMessageHandlerFactory to generate message handlers on demand.
    Generally, there should be one message handler for each contributed view.
    */
    private final UiMessageHandlerFactory messageHandlerFactory =
            () -> ImmutableList.of(
                    new QosUiMessageHandler()
            );
    /*
     Application UI extension.
     It should be configured with the previously declared UI view descriptors and message handler factory:
     */
    protected UiExtension extension =
            new UiExtension.Builder(getClass().getClassLoader(), uiViews)
                    .resourcePath(QOSUI_VIEW_ID)
                    .messageHandlerFactory(messageHandlerFactory)
                    .build();

    //Activation and deactivation callbacks that register and unregister the UI extension at the appropriate times
    @Activate
    protected void activate() {
        uiExtensionService.register(extension);
    }

    @Deactivate
    protected void deactivate() {
        uiExtensionService.unregister(extension);
    }

}