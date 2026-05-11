package com.richmond423.loadbalancerpro.api.proxy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "loadbalancerpro.proxy")
public class ReverseProxyProperties {
    private boolean enabled = false;
    private String strategy = "ROUND_ROBIN";
    private Duration requestTimeout = Duration.ofSeconds(2);
    private long maxRequestBytes = 65_536;
    private HealthCheck healthCheck = new HealthCheck();
    private List<Upstream> upstreams = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout == null ? Duration.ofSeconds(2) : requestTimeout;
    }

    public long getMaxRequestBytes() {
        return maxRequestBytes;
    }

    public void setMaxRequestBytes(long maxRequestBytes) {
        this.maxRequestBytes = maxRequestBytes;
    }

    public HealthCheck getHealthCheck() {
        return healthCheck;
    }

    public void setHealthCheck(HealthCheck healthCheck) {
        this.healthCheck = healthCheck == null ? new HealthCheck() : healthCheck;
    }

    public List<Upstream> getUpstreams() {
        return upstreams;
    }

    public void setUpstreams(List<Upstream> upstreams) {
        this.upstreams = upstreams == null ? new ArrayList<>() : new ArrayList<>(upstreams);
    }

    public static final class Upstream {
        private String id;
        private String url;
        private boolean healthy = true;
        private int inFlightRequestCount = 0;
        private Double configuredCapacity = 100.0;
        private Double estimatedConcurrencyLimit = 100.0;
        private double weight = 1.0;
        private double averageLatencyMillis = 1.0;
        private double p95LatencyMillis = 1.0;
        private double p99LatencyMillis = 1.0;
        private double recentErrorRate = 0.0;
        private Integer queueDepth = 0;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public boolean isHealthy() {
            return healthy;
        }

        public void setHealthy(boolean healthy) {
            this.healthy = healthy;
        }

        public int getInFlightRequestCount() {
            return inFlightRequestCount;
        }

        public void setInFlightRequestCount(int inFlightRequestCount) {
            this.inFlightRequestCount = inFlightRequestCount;
        }

        public Double getConfiguredCapacity() {
            return configuredCapacity;
        }

        public void setConfiguredCapacity(Double configuredCapacity) {
            this.configuredCapacity = configuredCapacity;
        }

        public Double getEstimatedConcurrencyLimit() {
            return estimatedConcurrencyLimit;
        }

        public void setEstimatedConcurrencyLimit(Double estimatedConcurrencyLimit) {
            this.estimatedConcurrencyLimit = estimatedConcurrencyLimit;
        }

        public double getWeight() {
            return weight;
        }

        public void setWeight(double weight) {
            this.weight = weight;
        }

        public double getAverageLatencyMillis() {
            return averageLatencyMillis;
        }

        public void setAverageLatencyMillis(double averageLatencyMillis) {
            this.averageLatencyMillis = averageLatencyMillis;
        }

        public double getP95LatencyMillis() {
            return p95LatencyMillis;
        }

        public void setP95LatencyMillis(double p95LatencyMillis) {
            this.p95LatencyMillis = p95LatencyMillis;
        }

        public double getP99LatencyMillis() {
            return p99LatencyMillis;
        }

        public void setP99LatencyMillis(double p99LatencyMillis) {
            this.p99LatencyMillis = p99LatencyMillis;
        }

        public double getRecentErrorRate() {
            return recentErrorRate;
        }

        public void setRecentErrorRate(double recentErrorRate) {
            this.recentErrorRate = recentErrorRate;
        }

        public Integer getQueueDepth() {
            return queueDepth;
        }

        public void setQueueDepth(Integer queueDepth) {
            this.queueDepth = queueDepth;
        }
    }

    public static final class HealthCheck {
        private boolean enabled = false;
        private String path = "/health";
        private Duration timeout = Duration.ofSeconds(1);
        private Duration interval = Duration.ofSeconds(30);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path == null || path.isBlank() ? "/health" : path;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout == null ? Duration.ofSeconds(1) : timeout;
        }

        public Duration getInterval() {
            return interval;
        }

        public void setInterval(Duration interval) {
            this.interval = interval == null ? Duration.ofSeconds(30) : interval;
        }
    }
}
