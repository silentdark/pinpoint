/*
 * Copyright 2018 NAVER Corp.
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

package com.navercorp.pinpoint.web.vo;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author HyunGil Jeong
 */
public class ApplicationAgentList {

    private final String groupName;
    private final List<AgentAndLink> agentInfoAndLinkList;

    public ApplicationAgentList(String groupName, List<AgentAndLink> agentInfoAndLinkList) {
        this.groupName = Objects.requireNonNull(groupName, "groupName");
        this.agentInfoAndLinkList = Objects.requireNonNull(agentInfoAndLinkList, "agentInfoAndLinkList");
    }

    public String getGroupName() {
        return groupName;
    }

    public List<AgentInfo> getAgentInfos() {
        return agentInfoAndLinkList.stream()
                .map(AgentAndLink::getAgentInfo)
                .collect(Collectors.toList());
    }

    public List<AgentAndLink> getAgentInfoAndLinks() {
        return agentInfoAndLinkList;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append('\'').append(groupName).append('\'');
        sb.append(":").append(agentInfoAndLinkList);
        sb.append('}');
        return sb.toString();
    }
}
