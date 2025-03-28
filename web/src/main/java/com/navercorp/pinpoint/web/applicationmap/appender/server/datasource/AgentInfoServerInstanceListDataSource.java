/*
 * Copyright 2017 NAVER Corp.
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

package com.navercorp.pinpoint.web.applicationmap.appender.server.datasource;

import com.navercorp.pinpoint.common.server.util.AgentLifeCycleState;
import com.navercorp.pinpoint.web.applicationmap.nodes.Node;
import com.navercorp.pinpoint.web.applicationmap.nodes.ServerBuilder;
import com.navercorp.pinpoint.web.applicationmap.nodes.ServerInstanceList;
import com.navercorp.pinpoint.web.applicationmap.histogram.Histogram;
import com.navercorp.pinpoint.web.applicationmap.histogram.NodeHistogram;
import com.navercorp.pinpoint.web.hyperlink.HyperLinkFactory;
import com.navercorp.pinpoint.web.service.AgentInfoService;
import com.navercorp.pinpoint.web.vo.AgentAndStatus;
import com.navercorp.pinpoint.web.vo.AgentInfo;
import com.navercorp.pinpoint.web.vo.AgentStatus;
import com.navercorp.pinpoint.web.vo.AgentStatusQuery;
import com.navercorp.pinpoint.web.vo.Application;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author HyunGil Jeong
 */
public class AgentInfoServerInstanceListDataSource implements ServerInstanceListDataSource {

    private final Logger logger = LogManager.getLogger(this.getClass());

    private final AgentInfoService agentInfoService;
    private final HyperLinkFactory hyperLinkFactory;

    public AgentInfoServerInstanceListDataSource(AgentInfoService agentInfoService, HyperLinkFactory hyperLinkFactory) {
        this.agentInfoService = Objects.requireNonNull(agentInfoService, "agentInfoService");
        this.hyperLinkFactory = Objects.requireNonNull(hyperLinkFactory, "hyperLinkFactory");
    }

    public ServerInstanceList createServerInstanceList(Node node, Instant timestamp) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(timestamp, "timestamp");
        if (timestamp.toEpochMilli() < 0) {
            return new ServerInstanceList();
        }

        Application application = node.getApplication();
        Set<AgentInfo> agentInfos = agentInfoService.getAgentsByApplicationNameWithoutStatus(application.getName(), timestamp.toEpochMilli());
        if (CollectionUtils.isEmpty(agentInfos)) {
            logger.warn("agentInfo not found. application:{}", application);
            return new ServerInstanceList();
        }

        logger.debug("unfiltered agentInfos {}", agentInfos);
        agentInfos = filterAgentInfos(agentInfos, timestamp, node);
        logger.debug("add agentInfos {} : {}", application, agentInfos);

        Set<AgentAndStatus> agentAndStatusSet = agentInfos.stream()
                .map(AgentAndStatus::new)
                .collect(Collectors.toSet());

        ServerBuilder builder = new ServerBuilder(hyperLinkFactory);
        builder.addAgentInfo(agentAndStatusSet);
        return builder.build();
    }

    // TODO Change to list of filters?
    private Set<AgentInfo> filterAgentInfos(Set<AgentInfo> agentInfos, Instant timestamp, Node node) {

        final Map<String, Histogram> agentHistogramMap = getAgentHistogramMap(node);

        Set<AgentInfo> filteredAgentInfos = new HashSet<>();
        List<AgentInfo> agentsToCheckStatus = new ArrayList<>();
        for (AgentInfo agentInfo : agentInfos) {
            String agentId = agentInfo.getAgentId();
            if (agentHistogramMap.containsKey(agentId)) {
                filteredAgentInfos.add(agentInfo);
            } else {
                agentsToCheckStatus.add(agentInfo);
            }
        }
        AgentStatusQuery query = AgentStatusQuery.buildQuery(agentInfos, timestamp);

        List<Optional<AgentStatus>> agentStatusList = agentInfoService.getAgentStatus(query);

        int idx = 0;
        for (AgentInfo agentInfo : agentsToCheckStatus) {
            Optional<AgentStatus> agentStatus = agentStatusList.get(idx++);
            if (agentStatus.isPresent()) {
                if (agentStatus.get().getState() == AgentLifeCycleState.RUNNING) {
                    filteredAgentInfos.add(agentInfo);
                }
            }
        }

        return filteredAgentInfos;
    }

    private Map<String, Histogram> getAgentHistogramMap(Node node) {
        NodeHistogram nodeHistogram = node.getNodeHistogram();
        if (nodeHistogram != null) {
            return nodeHistogram.getAgentHistogramMap();
        }
        return Collections.emptyMap();
    }
}
