/*
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
 * ClientServerCostCompare.java
 * Author : E.Ravikumaran
 * Date   : 16-Oct-2015
 **/

package org.onosproject.qos.rest.client;

/**
 * Class to declare the constants using throughout the project.
 */
    public final class ServiceConstants {
        private static final long QUEUE_SIZE = 2000000;
        private static final long THRESHOLD_PERCENTAGE = 80;

        public static final long CONGESTION_THRESHOLD = (QUEUE_SIZE * THRESHOLD_PERCENTAGE) / 100;
        public static final int PATH_INTENT_PRIORITY = 100;

        private ServiceConstants() {

        }

}