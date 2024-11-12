package com.angelone.metrics;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CustomMetricFilter implements MetricFilter {

    private static final String METRIC_FILTER_ALLOWED_METRICS_SUFFIX = "allowed-metrics";
    private static final Logger log = LoggerFactory.getLogger(CustomMetricFilter.class);
    private final Set<String> whitelistedMetricNames;

    public CustomMetricFilter(Map<String, String> metricConfiguration) {
        whitelistedMetricNames =
                Arrays
                        .stream(metricConfiguration
                                .getOrDefault(METRIC_FILTER_ALLOWED_METRICS_SUFFIX, "")
                                .split(","))
                        .collect(Collectors.toSet());
        log.info("allowedMetricList: {}",whitelistedMetricNames);
    }

    @Override
    public boolean matches(String name, Metric metric) {
        log.info("name: {} -> metric: {}", name, metric);
        return whitelistedMetricNames.contains(name);
    }
}
