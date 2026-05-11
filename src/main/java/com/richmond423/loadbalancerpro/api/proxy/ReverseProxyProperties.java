package com.richmond423.loadbalancerpro.api.proxy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "loadbalancerpro.proxy")
public class ReverseProxyProperties {
    private boolean enabled = false;
    private String strategy = "ROUND_ROBIN";
    private Duration requestTimeout = Duration.ofSeconds(2);
    private long maxRequestBytes = 65_536;
    private HealthCheck healthCheck = new HealthCheck();
    private Retry retry = new Retry();
    private Cooldown cooldown = new Cooldown();
    private List<Upstream> upstreams = new ArrayList<>();
    private Map<String, Route> routes = new LinkedHashMap<>();

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

    public Retry getRetry() {
        return retry;
    }

    public void setRetry(Retry retry) {
        this.retry = retry == null ? new Retry() : retry;
    }

    public Cooldown getCooldown() {
        return cooldown;
    }

    public void setCooldown(Cooldown cooldown) {
        this.cooldown = cooldown == null ? new Cooldown() : cooldown;
    }

    public List<Upstream> getUpstreams() {
        return upstreams;
    }

    public void setUpstreams(List<Upstream> upstreams) {
        this.upstreams = upstreams == null ? new ArrayList<>() : new ArrayList<>(upstreams);
    }

    public Map<String, Route> getRoutes() {
        return routes;
    }

    public void setRoutes(Map<String, Route> routes) {
        this.routes = routes == null ? new LinkedHashMap<>() : new LinkedHashMap<>(routes);
    }

    public static final class Route {
        private String pathPrefix = "/";
        private String strategy;
        private List<Upstream> targets = new ArrayList<>();

        public String getPathPrefix() {
            return pathPrefix;
        }

        public void setPathPrefix(String pathPrefix) {
            this.pathPrefix = pathPrefix;
        }

        public String getStrategy() {
            return strategy;
        }

        public void setStrategy(String strategy) {
            this.strategy = strategy;
        }

        public List<Upstream> getTargets() {
            return targets;
        }

        public void setTargets(List<Upstream> targets) {
            this.targets = targets == null ? new ArrayList<>() : new ArrayList<>(targets);
        }
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

    public static final class Retry {
        private boolean enabled = false;
        private int maxAttempts = 2;
        private boolean retryNonIdempotent = false;
        private Set<String> methods = new LinkedHashSet<>(Set.of("GET", "HEAD"));
        private Set<Integer> retryStatuses = new LinkedHashSet<>(Set.of(502, 503, 504));

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public boolean isRetryNonIdempotent() {
            return retryNonIdempotent;
        }

        public void setRetryNonIdempotent(boolean retryNonIdempotent) {
            this.retryNonIdempotent = retryNonIdempotent;
        }

        public Set<String> getMethods() {
            return methods;
        }

        public void setMethods(Set<String> methods) {
            this.methods = methods == null ? new LinkedHashSet<>() : new LinkedHashSet<>(methods);
        }

        public Set<Integer> getRetryStatuses() {
            return retryStatuses;
        }

        public void setRetryStatuses(Set<Integer> retryStatuses) {
            this.retryStatuses = retryStatuses == null ? new LinkedHashSet<>() : new LinkedHashSet<>(retryStatuses);
        }
    }

    public static final class Cooldown {
        private boolean enabled = false;
        private int consecutiveFailureThreshold = 2;
        private Duration duration = Duration.ofSeconds(30);
        private boolean recoverOnSuccessfulHealthCheck = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getConsecutiveFailureThreshold() {
            return consecutiveFailureThreshold;
        }

        public void setConsecutiveFailureThreshold(int consecutiveFailureThreshold) {
            this.consecutiveFailureThreshold = consecutiveFailureThreshold;
        }

        public Duration getDuration() {
            return duration;
        }

        public void setDuration(Duration duration) {
            this.duration = duration == null ? Duration.ofSeconds(30) : duration;
        }

        public boolean isRecoverOnSuccessfulHealthCheck() {
            return recoverOnSuccessfulHealthCheck;
        }

        public void setRecoverOnSuccessfulHealthCheck(boolean recoverOnSuccessfulHealthCheck) {
            this.recoverOnSuccessfulHealthCheck = recoverOnSuccessfulHealthCheck;
        }
    }
}
