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

package com.navercorp.pinpoint.web.service;

import com.navercorp.pinpoint.common.server.bo.SpanBo;
import com.navercorp.pinpoint.common.server.bo.SpanEventBo;
import com.navercorp.pinpoint.common.server.util.time.Range;
import com.navercorp.pinpoint.common.trace.HistogramSchema;
import com.navercorp.pinpoint.common.trace.HistogramSlot;
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.loader.service.ServiceTypeRegistryService;
import com.navercorp.pinpoint.web.TestTraceUtils;
import com.navercorp.pinpoint.web.applicationmap.ApplicationMap;
import com.navercorp.pinpoint.web.applicationmap.ApplicationMapBuilderFactory;
import com.navercorp.pinpoint.web.applicationmap.appender.histogram.NodeHistogramAppenderFactory;
import com.navercorp.pinpoint.web.applicationmap.appender.metric.DefaultMetricInfoAppenderFactory;
import com.navercorp.pinpoint.web.applicationmap.appender.server.ServerInfoAppenderFactory;
import com.navercorp.pinpoint.web.applicationmap.appender.server.datasource.AgentInfoServerInstanceListDataSource;
import com.navercorp.pinpoint.web.applicationmap.histogram.Histogram;
import com.navercorp.pinpoint.web.applicationmap.histogram.NodeHistogram;
import com.navercorp.pinpoint.web.applicationmap.histogram.TimeHistogramFormat;
import com.navercorp.pinpoint.web.applicationmap.link.Link;
import com.navercorp.pinpoint.web.applicationmap.nodes.Node;
import com.navercorp.pinpoint.web.dao.ApplicationTraceIndexDao;
import com.navercorp.pinpoint.web.dao.TraceDao;
import com.navercorp.pinpoint.web.filter.Filter;
import com.navercorp.pinpoint.web.hyperlink.HyperLinkFactory;
import com.navercorp.pinpoint.web.util.TimeWindow;
import com.navercorp.pinpoint.web.util.TimeWindowDownSampler;
import com.navercorp.pinpoint.web.view.AgentResponseTimeViewModel;
import com.navercorp.pinpoint.web.view.AgentResponseTimeViewModelList;
import com.navercorp.pinpoint.web.view.ResponseTimeViewModel;
import com.navercorp.pinpoint.web.view.TimeViewModel;
import com.navercorp.pinpoint.web.vo.Application;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author HyunGil Jeong
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class FilteredMapServiceImplTest {

    private static final Random RANDOM = new Random();

    private final ExecutorService executor = Executors.newFixedThreadPool(8);


    @Mock
    private TraceDao traceDao;

    @Mock
    private ApplicationTraceIndexDao applicationTraceIndexDao;

    @Mock
    private ApplicationFactory applicationFactory;

    @Mock
    private ServerInstanceDatasourceService serverInstanceDatasourceService;

    // Mocked
    private final ServiceTypeRegistryService registry = TestTraceUtils.mockServiceTypeRegistryService();

    @Spy
    private ApplicationMapBuilderFactory applicationMapBuilderFactory = new ApplicationMapBuilderFactory(
            new NodeHistogramAppenderFactory(executor),
            new ServerInfoAppenderFactory(executor),
            Optional.of(new DefaultMetricInfoAppenderFactory())
    );

    private FilteredMapService filteredMapService;

    @BeforeEach
    public void init() {
        when(applicationFactory.createApplication(anyString(), anyShort()))
                .thenAnswer(invocation -> {
                    String applicationName = invocation.getArgument(0);
                    ServiceType serviceType = registry.findServiceType(invocation.getArgument(1));
                    return new Application(applicationName, serviceType);
                });
        when(applicationFactory.createApplication(anyString(), any(ServiceType.class)))
                .thenAnswer(invocation -> {
                    String applicationName = invocation.getArgument(0);
                    ServiceType serviceType = invocation.getArgument(1);
                    return new Application(applicationName, serviceType);
                });
        when(applicationFactory.createApplicationByTypeName(anyString(), anyString()))
                .thenAnswer(invocation -> {
                    String applicationName = invocation.getArgument(0);
                    ServiceType serviceType = registry.findServiceTypeByName(invocation.getArgument(1));
                    return new Application(applicationName, serviceType);
                });

        when(serverInstanceDatasourceService.getServerInstanceListDataSource())
                .thenAnswer(invocation -> {
                    AgentInfoService agentInfoService = mock(AgentInfoService.class);
                    HyperLinkFactory hyperLinkFactory = new HyperLinkFactory(Collections.emptyList());
                    return new AgentInfoServerInstanceListDataSource(agentInfoService, hyperLinkFactory);
                });

        filteredMapService = new FilteredMapServiceImpl(traceDao, applicationTraceIndexDao,
                registry, applicationFactory, serverInstanceDatasourceService, Optional.empty(), applicationMapBuilderFactory);

    }

    @AfterEach
    public void cleanUp() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * USER -> ROOT_APP -> APP_A -> CACHE
     */
    @Test
    public void twoTier() {
        // Given
        Range originalRange = Range.between(1000, 2000);
        Range scanRange = Range.between(1000, 2000);
        final TimeWindow timeWindow = new TimeWindow(originalRange, TimeWindowDownSampler.SAMPLER);

        // root app span
        long rootSpanId = RANDOM.nextLong();
        long rootSpanStartTime = 1000L;
        long rootSpanCollectorAcceptTime = 1210L;
        int rootSpanElapsed = 200;
        SpanBo rootSpan = new TestTraceUtils.SpanBuilder("ROOT_APP", "root-agent")
                .spanId(rootSpanId)
                .startTime(rootSpanStartTime)
                .collectorAcceptTime(rootSpanCollectorAcceptTime)
                .elapsed(rootSpanElapsed)
                .build();
        // app A span
        long appASpanId = RANDOM.nextLong();
        long appASpanStartTime = 1020L;
        long appASpanCollectorAcceptTime = 1090L;
        int appASpanElapsed = 160;
        SpanBo appASpan = new TestTraceUtils.SpanBuilder("APP_A", "app-a")
                .spanId(appASpanId)
                .parentSpan(rootSpan)
                .startTime(appASpanStartTime)
                .collectorAcceptTime(appASpanCollectorAcceptTime)
                .elapsed(appASpanElapsed)
                .build();
        // root app -> app A rpc span event
        SpanEventBo rootRpcSpanEvent = new TestTraceUtils.RpcSpanEventBuilder("www.foo.com/bar", 10, 190)
                .nextSpanId(appASpanId)
                .build();
        rootSpan.addSpanEvent(rootRpcSpanEvent);
        // app A -> cache span event
        int cacheStartElapsed = 20;
        int cacheEndElapsed = 130;
        SpanEventBo appACacheSpanEvent = new TestTraceUtils.CacheSpanEventBuilder("CacheName", "1.1.1.1", cacheStartElapsed, cacheEndElapsed).build();
        appASpan.addSpanEvent(appACacheSpanEvent);

        when(traceDao.selectAllSpans(anyList(), isNull())).thenReturn(Collections.singletonList(Arrays.asList(rootSpan, appASpan)));

        // When
        final FilteredMapServiceOption option = new FilteredMapServiceOption.Builder(Collections.emptyList(), originalRange, scanRange, 1, 1, Filter.acceptAllFilter(), 0).build();
        ApplicationMap applicationMap = filteredMapService.selectApplicationMapWithScatterData(option);

        // Then
        Collection<Node> nodes = applicationMap.getNodes();
        Assertions.assertEquals(4, nodes.size());
        for (Node node : nodes) {
            Application application = node.getApplication();
            if (application.getName().equals("ROOT_APP") && application.getServiceType().getCode() == TestTraceUtils.USER_TYPE_CODE) {
                // USER node
                NodeHistogram nodeHistogram = node.getNodeHistogram();
                // histogram
                Histogram applicationHistogram = nodeHistogram.getApplicationHistogram();
                assertHistogram(applicationHistogram, 1, 0, 0, 0, 0);
                Map<String, Histogram> agentHistogramMap = nodeHistogram.getAgentHistogramMap();
                Assertions.assertTrue(agentHistogramMap.isEmpty());
                // time histogram
                HistogramSchema histogramSchema = node.getServiceType().getHistogramSchema();
                List<ResponseTimeViewModel.TimeCount> expectedTimeCounts = Collections.singletonList(
                        new ResponseTimeViewModel.TimeCount(timeWindow.refineTimestamp(rootSpanCollectorAcceptTime), 1)
                );
                List<TimeViewModel> applicationTimeHistogram = nodeHistogram.getApplicationTimeHistogram(node.getTimeHistogramFormat());
                assertTimeHistogram(applicationTimeHistogram, histogramSchema.getFastSlot(), expectedTimeCounts);
                AgentResponseTimeViewModelList agentTimeHistogram = nodeHistogram.getAgentTimeHistogram(TimeHistogramFormat.V1);
                Assertions.assertTrue(agentTimeHistogram.getAgentResponseTimeViewModelList().isEmpty());
            } else if (application.getName().equals("ROOT_APP") && application.getServiceType().getCode() == TestTraceUtils.TEST_STAND_ALONE_TYPE_CODE) {
                // ROOT_APP node
                NodeHistogram nodeHistogram = node.getNodeHistogram();
                // histogram
                Histogram applicationHistogram = nodeHistogram.getApplicationHistogram();
                assertHistogram(applicationHistogram, 1, 0, 0, 0, 0);
                Map<String, Histogram> agentHistogramMap = nodeHistogram.getAgentHistogramMap();
                assertAgentHistogram(agentHistogramMap, "root-agent", 1, 0, 0, 0, 0);
                // time histogram
                HistogramSchema histogramSchema = node.getServiceType().getHistogramSchema();
                List<ResponseTimeViewModel.TimeCount> expectedTimeCounts = Collections.singletonList(
                        new ResponseTimeViewModel.TimeCount(timeWindow.refineTimestamp(rootSpanCollectorAcceptTime), 1)
                );
                List<TimeViewModel> applicationTimeHistogram = nodeHistogram.getApplicationTimeHistogram(node.getTimeHistogramFormat());
                assertTimeHistogram(applicationTimeHistogram, histogramSchema.getFastSlot(), expectedTimeCounts);
                AgentResponseTimeViewModelList agentTimeHistogram = nodeHistogram.getAgentTimeHistogram(TimeHistogramFormat.V1);
                assertAgentTimeHistogram(agentTimeHistogram.getAgentResponseTimeViewModelList(), "root-agent", histogramSchema.getFastSlot(), expectedTimeCounts);
            } else if (application.getName().equals("APP_A") && application.getServiceType().getCode() == TestTraceUtils.TEST_STAND_ALONE_TYPE_CODE) {
                // APP_A node
                NodeHistogram nodeHistogram = node.getNodeHistogram();
                // histogram
                Histogram applicationHistogram = nodeHistogram.getApplicationHistogram();
                assertHistogram(applicationHistogram, 1, 0, 0, 0, 0);
                Map<String, Histogram> agentHistogramMap = nodeHistogram.getAgentHistogramMap();
                assertAgentHistogram(agentHistogramMap, "app-a", 1, 0, 0, 0, 0);
                // time histogram
                HistogramSchema histogramSchema = node.getServiceType().getHistogramSchema();
                List<ResponseTimeViewModel.TimeCount> expectedTimeCounts = Collections.singletonList(
                        new ResponseTimeViewModel.TimeCount(timeWindow.refineTimestamp(appASpanCollectorAcceptTime), 1)
                );
                List<TimeViewModel> applicationTimeHistogram = nodeHistogram.getApplicationTimeHistogram(node.getTimeHistogramFormat());
                assertTimeHistogram(applicationTimeHistogram, histogramSchema.getFastSlot(), expectedTimeCounts);
                AgentResponseTimeViewModelList agentTimeHistogram = nodeHistogram.getAgentTimeHistogram(TimeHistogramFormat.V1);
                assertAgentTimeHistogram(agentTimeHistogram.getAgentResponseTimeViewModelList(), "app-a", histogramSchema.getFastSlot(), expectedTimeCounts);
            } else if (application.getName().equals("CacheName") && application.getServiceType().getCode() == TestTraceUtils.CACHE_TYPE_CODE) {
                // CACHE node
                NodeHistogram nodeHistogram = node.getNodeHistogram();
                // histogram
                Histogram applicationHistogram = nodeHistogram.getApplicationHistogram();
                assertHistogram(applicationHistogram, 0, 1, 0, 0, 0);
                Map<String, Histogram> agentHistogramMap = nodeHistogram.getAgentHistogramMap();
                assertAgentHistogram(agentHistogramMap, "1.1.1.1", 0, 1, 0, 0, 0);
                // time histogram
                HistogramSchema histogramSchema = node.getServiceType().getHistogramSchema();
                List<ResponseTimeViewModel.TimeCount> expectedTimeCounts = Collections.singletonList(
                        new ResponseTimeViewModel.TimeCount(timeWindow.refineTimestamp(appASpanStartTime + cacheStartElapsed), 1)
                );
                List<TimeViewModel> applicationTimeHistogram = nodeHistogram.getApplicationTimeHistogram(node.getTimeHistogramFormat());
                assertTimeHistogram(applicationTimeHistogram, histogramSchema.getNormalSlot(), expectedTimeCounts);
                AgentResponseTimeViewModelList agentTimeHistogram = nodeHistogram.getAgentTimeHistogram(TimeHistogramFormat.V1);
                assertAgentTimeHistogram(agentTimeHistogram.getAgentResponseTimeViewModelList(), "1.1.1.1", histogramSchema.getNormalSlot(), expectedTimeCounts);
            } else {
                fail("Unexpected node : " + node);
            }
        }

        Collection<Link> links = applicationMap.getLinks();
        Assertions.assertEquals(3, links.size());
        for (Link link : links) {
            Application fromApplication = link.getFrom().getApplication();
            Application toApplication = link.getTo().getApplication();
            if ((fromApplication.getName().equals("ROOT_APP") && fromApplication.getServiceType().getCode() == TestTraceUtils.USER_TYPE_CODE) &&
                    (toApplication.getName().equals("ROOT_APP") && toApplication.getServiceType().getCode() == TestTraceUtils.TEST_STAND_ALONE_TYPE_CODE)) {
                // histogram
                Histogram histogram = link.getHistogram();
                assertHistogram(histogram, 1, 0, 0, 0, 0);
                // time histogram
                List<TimeViewModel> linkApplicationTimeSeriesHistogram = link.getLinkApplicationTimeSeriesHistogram();
                HistogramSchema targetHistogramSchema = toApplication.getServiceType().getHistogramSchema();
                List<ResponseTimeViewModel.TimeCount> expectedTimeCounts = Collections.singletonList(
                        new ResponseTimeViewModel.TimeCount(timeWindow.refineTimestamp(rootSpanCollectorAcceptTime), 1)
                );
                assertTimeHistogram(linkApplicationTimeSeriesHistogram, targetHistogramSchema.getFastSlot(), expectedTimeCounts);
            } else if ((fromApplication.getName().equals("ROOT_APP") && fromApplication.getServiceType().getCode() == TestTraceUtils.TEST_STAND_ALONE_TYPE_CODE) &&
                    (toApplication.getName().equals("APP_A") && toApplication.getServiceType().getCode() == TestTraceUtils.TEST_STAND_ALONE_TYPE_CODE)) {
                // histogram
                Histogram histogram = link.getHistogram();
                assertHistogram(histogram, 1, 0, 0, 0, 0);
                // time histogram
                List<TimeViewModel> linkApplicationTimeSeriesHistogram = link.getLinkApplicationTimeSeriesHistogram();
                HistogramSchema targetHistogramSchema = toApplication.getServiceType().getHistogramSchema();
                List<ResponseTimeViewModel.TimeCount> expectedTimeCounts = Collections.singletonList(
                        new ResponseTimeViewModel.TimeCount(timeWindow.refineTimestamp(appASpanCollectorAcceptTime), 1)
                );
                assertTimeHistogram(linkApplicationTimeSeriesHistogram, targetHistogramSchema.getFastSlot(), expectedTimeCounts);
            } else if ((fromApplication.getName().equals("APP_A") && fromApplication.getServiceType().getCode() == TestTraceUtils.TEST_STAND_ALONE_TYPE_CODE) &&
                    (toApplication.getName().equals("CacheName") && toApplication.getServiceType().getCode() == TestTraceUtils.CACHE_TYPE_CODE
                    )) {
                // histogram
                Histogram histogram = link.getHistogram();
                assertHistogram(histogram, 0, 1, 0, 0, 0);
                // time histogram
                List<TimeViewModel> linkApplicationTimeSeriesHistogram = link.getLinkApplicationTimeSeriesHistogram();
                HistogramSchema targetHistogramSchema = toApplication.getServiceType().getHistogramSchema();
                List<ResponseTimeViewModel.TimeCount> expectedTimeCounts = Collections.singletonList(
                        new ResponseTimeViewModel.TimeCount(timeWindow.refineTimestamp(appASpanStartTime + cacheStartElapsed), 1)
                );
                assertTimeHistogram(linkApplicationTimeSeriesHistogram, targetHistogramSchema.getNormalSlot(), expectedTimeCounts);
            } else {
                fail("Unexpected link : " + link);
            }
        }
    }

    private void assertHistogram(Histogram histogram, int fastCount, int normalCount, int slowCount, int verySlowCount, int totalErrorCount) {
        Assertions.assertEquals(fastCount, histogram.getFastCount());
        Assertions.assertEquals(normalCount, histogram.getNormalCount());
        Assertions.assertEquals(slowCount, histogram.getSlowCount());
        Assertions.assertEquals(verySlowCount, histogram.getVerySlowCount());
        Assertions.assertEquals(totalErrorCount, histogram.getTotalErrorCount());
    }

    private void assertTimeHistogram(List<TimeViewModel> histogramList, HistogramSlot histogramSlot, List<ResponseTimeViewModel.TimeCount> expectedTimeCounts) {
        if (CollectionUtils.isEmpty(histogramList)) {
            fail("Checked against empty histogramList.");
        }
        String slotName = histogramSlot.getSlotName();
        for (TimeViewModel timeViewModel : histogramList) {
            ResponseTimeViewModel histogram = (ResponseTimeViewModel) timeViewModel;

            if (histogram.getColumnName().equals(slotName)) {
                for (ResponseTimeViewModel.TimeCount expectedTimeCount : expectedTimeCounts) {
                    boolean expectedTimeCountExists = false;
                    for (ResponseTimeViewModel.TimeCount actualTimeCount : histogram.getColumnValue()) {
                        if (expectedTimeCount.getTime() == actualTimeCount.getTime()) {
                            expectedTimeCountExists = true;
                            Assertions.assertEquals(expectedTimeCount.getCount(), actualTimeCount.getCount(), "TimeCount mismatch for slot : " + slotName);
                            break;
                        }
                    }
                    if (!expectedTimeCountExists) {
                        fail("Expected TimeCount for " + slotName + " not found, time : " + expectedTimeCount.getTime() + ", count : " + expectedTimeCount.getCount());
                    }
                }
                return;
            }
        }
        fail("Expected " + slotName + " but had none.");
    }

    private void assertAgentHistogram(Map<String, Histogram> agentHistogramMap, String agentId, int fastCount, int normalCount, int slowCount, int verySlowCount, int totalErrorCount) {
        Histogram agentHistogram = agentHistogramMap.get(agentId);
        if (agentHistogram != null) {
            assertHistogram(agentHistogram, fastCount, normalCount, slowCount, verySlowCount, totalErrorCount);
            return;
        }
        fail("Histogram not found for agent : " + agentId);
    }

    private void assertAgentTimeHistogram(List<AgentResponseTimeViewModel> histogramList, String agentId, HistogramSlot histogramSlot, List<ResponseTimeViewModel.TimeCount> expectedTimeCounts) {
        for (AgentResponseTimeViewModel histogram : histogramList) {
            if (agentId.equals(histogram.getAgentName())) {
                assertTimeHistogram(histogram.getResponseTimeViewModel(), histogramSlot, expectedTimeCounts);
                return;
            }
        }
        fail("Time histogram not found for agent : " + agentId);
    }
}
