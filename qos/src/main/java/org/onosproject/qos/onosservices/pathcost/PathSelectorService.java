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
 * Author : Shahabuddeen
 **/

package org.onosproject.qos.onosservices.pathcost;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.onlab.graph.DijkstraGraphSearch;
import org.onlab.graph.GraphPathSearch;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.JsonCodec;
import org.onosproject.common.DefaultTopologyGraph;
import org.onosproject.incubator.net.PortStatisticsService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultEdgeLink;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.DeviceId;
import org.onosproject.net.EdgeLink;
import org.onosproject.net.ElementId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.host.HostService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.statistic.Load;
import org.onosproject.net.topology.PathService;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.net.topology.TopologyVertex;
import org.onosproject.qos.rest.client.ServiceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.core.CoreService.CORE_PROVIDER_ID;

/**
 * Get path from ONOS for the Application.
 */
public final class PathSelectorService implements CodecContext {

    static final Logger LOGGER = LoggerFactory.getLogger(PathSelectorService.class);
    protected static ObjectMapper objectMapper;
    protected static ServiceDirectory serviceDirectory;
    protected static PathSelectorService pathSelectorService;

    private static final ProviderId PID = new ProviderId("core", "org.onosproject.core");
    private static final PortNumber P0 = PortNumber.portNumber(0);

    private static final EdgeLink NOT_HOST = new NotHost();
    /**
     * Private constructor for singleton.
     */
    private PathSelectorService() {
        LOGGER.info("QoS - PathSelectorService - PathSelectorService created");
        if (serviceDirectory == null) {
            serviceDirectory = new DefaultServiceDirectory();
        }
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
        }
    }

    public static PathSelectorService getInstance() {
        if (pathSelectorService == null) {
            pathSelectorService = new PathSelectorService();
        }
        return pathSelectorService;
    }

    /**
     * Get shortest path between server and client.
     * @param serverHostId HostId of server
     * @param clientHostId HostId of client
     * @return List of shortest path between server and client in JSON
     */
    public Set<Path>  getOptimalPath(ElementId serverHostId,
                                     ElementId clientHostId) {
        Set<Path> paths = getNonCongestedPaths(serverHostId, clientHostId);
        if (paths == null || paths.size() == 0) {
            PathService pathService = serviceDirectory.get(PathService.class);
            LOGGER.info("QoS - PathSelectorService.getOptimalPath - " +
                    "Cannot find non-congested paths trying for shortest path instead.");
            paths = pathService.getPaths(serverHostId, clientHostId);
        }
        return paths;
    }

    /**
     * Get alternate non congested shortest path.
     * @param srcId ElementId of source
     * @param dstId ElementId of destination
     * @return List of shortest non congested path between source and destination
     */
    public Set<Path> getNonCongestedPaths(ElementId srcId, ElementId dstId) {
        // Get the source and destination edge locations
        EdgeLink srcEdge = getEdgeLink(srcId, true);
        EdgeLink dstEdge = getEdgeLink(dstId, false);

        // If either edge is null, bail with no paths.
        if (srcEdge == null || dstEdge == null) {
            return ImmutableSet.of();
        }

        DeviceId srcDevice = srcEdge != NOT_HOST ? srcEdge.dst().deviceId() : (DeviceId) srcId;
        DeviceId dstDevice = dstEdge != NOT_HOST ? dstEdge.src().deviceId() : (DeviceId) dstId;

        // If the source and destination are on the same edge device, there
        // is just one path, so build it and return it.
        if (srcDevice.equals(dstDevice)) {
            return edgeToEdgePaths(srcEdge, dstEdge);
        }

        PortStatisticsService portStatisticsService = serviceDirectory.get(PortStatisticsService.class);
        TopologyService topologyService = serviceDirectory.get(TopologyService.class);

        //Get Topology Graph from Topology Service.
        TopologyGraph topologyGraph = topologyService.getGraph(topologyService.currentTopology());
        Set<TopologyEdge> edgeSet = topologyGraph.getEdges();
        Set<TopologyVertex> vertexSet = topologyGraph.getVertexes();
        if (edgeSet.isEmpty()) {
            ImmutableSet.Builder<Path> emptyPathBuilder = ImmutableSet.builder();
            return emptyPathBuilder.build();
        }
        ImmutableSet.Builder<TopologyEdge> filteredEdges = ImmutableSet.builder();
        final DeviceId finalSrcId = deviceId(srcId);
        final DeviceId finalDstId = deviceId(dstId);
        //Get set of edges having congestion.
        Set<TopologyEdge> congestedEdges = new HashSet<>();
        edgeSet.forEach(p -> {
            Load portLoad = portStatisticsService.load(p.link().src());
            if (portLoad != null &&
                    portLoad.rate() >= ServiceConstants.CONGESTION_THRESHOLD) {
                congestedEdges.add(p);
            }
        });
        //Get the links not having congestion and build new topology graph.
        edgeSet.forEach(p -> {
            if (!congestedEdges.contains(p)) {
                filteredEdges.add(p);
            }
        });
        ImmutableSet<TopologyEdge> finalEdges = filteredEdges.build();
        DefaultTopologyGraph graph = new DefaultTopologyGraph(vertexSet, finalEdges);
        // Get the topology vertex of the device and calculate
        // shortest paths between source and destination switches.
        final TopologyVertex[] src = {null};
        final TopologyVertex[] dst = {null};
        if (srcId == null || dstId == null) {
            LOGGER.info("QoS - PathSelectorService.getNonCongestedPaths - Device ID is null");
            ImmutableSet.Builder<Path> emptyPathBuilder = ImmutableSet.builder();
            return emptyPathBuilder.build();
        } else {
            vertexSet.forEach(p -> {
                if (p.deviceId().equals(finalSrcId)) {
                    src[0] = p;
                }
                if (p.deviceId().equals(finalDstId)) {
                    dst[0] = p;
                }
            });
        }
        if (src[0] == null || dst[0] == null) {
            LOGGER.info("QoS - PathSelectorService.getNonCongestedPaths - Device ID is null");
            ImmutableSet.Builder<Path> emptyPathBuilder = ImmutableSet.builder();
            return emptyPathBuilder.build();
        }
        DijkstraGraphSearch<TopologyVertex, TopologyEdge> graphSearch = new DijkstraGraphSearch<>();
        GraphPathSearch.Result<TopologyVertex, TopologyEdge> result =
                graphSearch.search(graph, src[0], dst[0], null, 100);
        ImmutableSet.Builder<Path> builder = ImmutableSet.builder();
        //Convert the Path format and add edge links
        for (org.onlab.graph.Path<TopologyVertex, TopologyEdge> path : result.paths()) {
            builder.add(networkPath(path));
        }
        return edgeToEdgePaths(srcEdge, dstEdge, builder.build());
    }

    /**
     * Convert org.onlab.graph.Path to org.onosproject.net.Path.
     * @param path org.onlab.graph.Path
     * @return org.onosproject.net.Path
     */
    private Path networkPath(org.onlab.graph.Path<TopologyVertex, TopologyEdge> path) {
        List<Link> links = path.edges().stream().map(TopologyEdge::link).collect(Collectors.toList());
        return new DefaultPath(CORE_PROVIDER_ID, links, path.cost());
    }

    /**
     * Cast the ElementId to DeviceId if the element is switch or gets DeviceId of the switch the host connected to.
     * @param id ElementId
     * @return DeviceId
     */
    private DeviceId deviceId(ElementId id) {
        PathSelectorService singleton = getInstance();
        if (id instanceof HostId) {
            HostService hostService = singleton.getService(HostService.class);
            return hostService.getHost((HostId) id).location().deviceId();
        } else if (id instanceof DeviceId) {
            return (DeviceId) id;
        } else {
            LOGGER.info("QoS - PathSelectorService.deviceId - Element is netither host nor device");
            return null;
        }
    }

    /**
     * Finds the host edge link if the element ID is a host id of an existing
     * host. Otherwise, if the host does not exist, it returns null and if
     * the element ID is not a host ID, returns NOT_HOST edge link.
     * @param elementId Id of the host
     * @param isIngress Is the edge ingress
     * @return EdgeLink of the host
     */
    private EdgeLink getEdgeLink(ElementId elementId, boolean isIngress) {
        if (elementId instanceof HostId) {
            HostService hostService = serviceDirectory.get(HostService.class);
            // Resolve the host, return null.
            Host host = hostService.getHost((HostId) elementId);
            if (host == null) {
                return null;
            }
            return new DefaultEdgeLink(PID, new ConnectPoint(elementId, P0),
                    host.location(), isIngress);
        }
        return NOT_HOST;
    }

    /**
     * Produces a set of edge-to-edge paths using the set of infrastructure
     * paths and the given edge links.
     * @param srcLink Source Link
     * @param dstLink Destination Link
     * @return Set of edge to edge paths
     */
    private Set<Path> edgeToEdgePaths(EdgeLink srcLink, EdgeLink dstLink) {
        Set<Path> endToEndPaths = Sets.newHashSetWithExpectedSize(1);
        endToEndPaths.add(edgeToEdgePath(srcLink, dstLink, null));
        return endToEndPaths;
    }

    /**
     * Produces a set of edge-to-edge paths using the set of infrastructure
     * paths and the given edge links.
     * @param srcLink Source link
     * @param dstLink Destination Link
     * @param paths Set of Paths between source and destination switches
     * @return Set of edge to edge paths
     */
    private Set<Path> edgeToEdgePaths(EdgeLink srcLink, EdgeLink dstLink, Set<Path> paths) {
        Set<Path> endToEndPaths = Sets.newHashSetWithExpectedSize(paths.size());
        for (Path path : paths) {
            endToEndPaths.add(edgeToEdgePath(srcLink, dstLink, path));
        }
        return endToEndPaths;
    }

    /**
     * Produces a direct edge-to-edge path.
     * @param srcLink Source link
     * @param dstLink Destination Link
     * @param path Path between source and destination switches
     * @return Edge to edge path
     */
    private Path edgeToEdgePath(EdgeLink srcLink, EdgeLink dstLink, Path path) {
        List<Link> links = Lists.newArrayListWithCapacity(2);
        // Add source and destination edge links only if they are real and
        // add the infrastructure path only if it is not null.
        if (srcLink != NOT_HOST) {
            links.add(srcLink);
        }
        if (path != null) {
            links.addAll(path.links());
        }
        if (dstLink != NOT_HOST) {
            links.add(dstLink);
        }
        return new DefaultPath(PID, links, 2);
    }

    /**
     * Special value for edge link to represent that this is really not an
     * edge link since the src or dst are really an infrastructure device.
     */
    private static class NotHost extends DefaultEdgeLink implements EdgeLink {
        NotHost() {
            super(PID, new ConnectPoint(HostId.NONE, P0),
                    new HostLocation(DeviceId.NONE, P0, 0L), false);
        }
    }
    /**
     *Get object mapper for JSON Codec.
     * @return Object mapper
     */
    @Override
    public ObjectMapper mapper() {
        return objectMapper;
    }

    /**
     *Gets JSON codec implementation for a class.
     * @param aClass Class of the object that needs to encoded or decoded
     * @param <T> Class of the object that needs to encoded or decoded
     * @return JSON codec implementation for that class
     */
    @Override
    public <T> JsonCodec<T> codec(Class<T> aClass) {
        return serviceDirectory.get(CodecService.class).getCodec(aClass);
    }

    /**
     *Get the service class implementation from ONOS.
     * @param aClass Service class that is need
     * @param <T> Class of the Service
     * @return Implementation of the service class
     */
    @Override
    public <T> T getService(Class<T> aClass) {
        return serviceDirectory.get(aClass);
    }
    /**
     * Returns the reverse path.
     * @param path - path between source and destination
     * @return - array of reversed links
     */
    public static Path reversePath(Path path) {
        List<Link> reverseLinks = new ArrayList<>(path.links().size());
        for (Link link : path.links()) {
            reverseLinks.add(0, reverseLink(link));
        }
        return new DefaultPath(path.providerId(), reverseLinks, path.cost());
    }

    /**
     * Reverse the given link.
     * @param link - link between the two adjacent nodes
     * @return - reversed links
     */
    public static Link reverseLink(Link link) {
        return DefaultLink.builder().providerId(link.providerId())
                .src(link.dst())
                .dst(link.src())
                .type(link.type())
                .state(link.state())
                .isExpected(link.isExpected())
                .build();
    }
}