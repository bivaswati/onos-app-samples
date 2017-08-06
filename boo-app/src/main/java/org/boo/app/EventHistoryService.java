/*
 * Copyright 2014 Open Networking Laboratory
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

package org.boo.app;

import com.google.common.annotations.Beta;
import org.onosproject.event.Event;

import java.util.Deque;

/**
 * Provides history of instance local ONOS Events.
 */
@Beta
public interface EventHistoryService {

    /**
     * Returns unmodifiable view of ONOS events history.
     *
     * @return ONOS events (First element is the oldest event stored)
     */
    Deque<Event<?, ?>> history();

    /**
     * Clears all stored history.
     */
    void clear();
}
