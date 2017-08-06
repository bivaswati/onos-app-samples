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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.joda.time.LocalDateTime;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.event.Event;
import org.onosproject.net.Link;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.link.LinkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * Command to print history of instance local ONOS Events.
 */
@Command(scope = "onos", name = "link-app",
        description = "Command to print history of instance local ONOS Events")
public class EventsCommand extends AbstractShellCommand {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Option(name = "--all", aliases = "-a",
            description = "Include all Events (default behavior)",
            required = false)
    private boolean all = false;

    @Option(name = "--mastership", aliases = "-m",
            description = "Include MastershipEvent",
            required = false)
    private boolean mastership = false;

    @Option(name = "--device", aliases = "-d",
            description = "Include DeviceEvent",
            required = false)
    private boolean device = false;

    @Option(name = "--link", aliases = "-l",
            description = "Include LinkEvent",
            required = false)
    private boolean link = false;

    @Option(name = "--topology", aliases = "-t",
            description = "Include TopologyEvent",
            required = false)
    private boolean topology = false;

    @Option(name = "--host", aliases = "-t",
            description = "Include HostEvent",
            required = false)
    private boolean host = false;

    @Option(name = "--cluster", aliases = "-c",
            description = "Include ClusterEvent",
            required = false)
    private boolean cluster = false;

    @Option(name = "--max-events", aliases = "-n",
            description = "Maximum number of events to print",
            required = false,
            valueToShowInHelp = "-1 [no limit]")
    private long maxSize = -1;

    @Override
    protected void execute() {
        EventHistoryService eventHistoryService = get(EventHistoryService.class);

        Stream<Event<?, ?>> events = eventHistoryService.history().stream();

        boolean dumpAll = all || !(mastership || device || link || topology || host);

        if (!dumpAll) {
            Predicate<Event<?, ?>> filter = (defaultIs) -> false;

            if (device) {
                log.info("Bivas - Device....................");
                filter = filter.or(evt -> evt instanceof DeviceEvent);
            }
            if (link) {
                log.info("Bivas - Link....................");
                filter = filter.or(evt -> evt instanceof LinkEvent);
            }

            events = events.filter(filter);
        }

        if (maxSize > 0) {
            events = events.limit(maxSize);
        }

        if (outputJson()) {
            ArrayNode jsonEvents = events.map(this::json).collect(toArrayNode());
            printJson(jsonEvents);
        } else {
            events.forEach(this::printEvent);
        }

    }

    private Collector<JsonNode, ArrayNode, ArrayNode> toArrayNode() {
        return Collector.of(() -> mapper().createArrayNode(),
                            ArrayNode::add,
                            ArrayNode::addAll);
    }

    private ObjectNode json(Event<?, ?> event) {
        ObjectNode result = mapper().createObjectNode();

        result.put("time", event.time())
                .put("type", event.type().toString())
                .put("event", event.toString());

        return result;
    }

    /**
     * Print JsonNode using default pretty printer.
     *
     * @param json JSON node to print
     */
    @java.lang.SuppressWarnings("squid:S1148")
    private void printJson(JsonNode json) {
        try {
            print("%s", mapper().writerWithDefaultPrettyPrinter().writeValueAsString(json));
        } catch (JsonProcessingException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            print("[ERROR] %s\n%s", e.getMessage(), sw.toString());
        }
    }

    private void printEvent(Event<?, ?> event) {
        if (event instanceof DeviceEvent) {
            log.info("Bivas - Device event caught.");
            DeviceEvent deviceEvent = (DeviceEvent) event;
            if (event.type().toString().startsWith("PORT")) {
                // Port event
                print("%s %s\t%s/%s [%s]",
                      new LocalDateTime(event.time()),
                      event.type(),
                      deviceEvent.subject().id(), deviceEvent.port().number(),
                      deviceEvent.port()
                );
            } else {
                // Device event
                print("Bivas - DE is %s %s\t%s [%s]",
                      new LocalDateTime(event.time()),
                      event.type(),
                      deviceEvent.subject().id(),
                      deviceEvent.subject()
                );
            }
        } else if (event instanceof LinkEvent) {
            log.info("Bivas -Link  event caught.");
            LinkEvent linkEvent = (LinkEvent) event;
            Link link = linkEvent.subject();
            print("Bivas - LE is %s %s\t%s/%s-%s/%s [%s]",
                  new LocalDateTime(event.time()),
                  event.type(),
                  link.src().deviceId(), link.src().port(), link.dst().deviceId(), link.dst().port(), link);
        } else {
            // Unknown Event?
            print("Bivas -Unknown Event %s %s\t%s [%s]",
                  new LocalDateTime(event.time()),
                  event.type(), event.subject(), event);
        }
    }
}
