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
    private Duration connectTimeout = Duration.ofSeconds(1);
    private Duration requestTimeout = Duration.ofSeconds(2);
    private long maxRequestBytes = 65_536;
    private long maxResponseBytes = 0;
    private PrivateNetworkValidation privateNetworkValidation = new PrivateNetworkValidation();
    private PrivateNetworkLiveValidation privateNetworkLiveValidation = new PrivateNetworkLiveValidation();
    private HealthCheck healthCheck = new HealthCheck();
    private Retry retry = new Retry();
    private Cooldown cooldown = new Cooldown();
    private SlowStart slowStart = new SlowStart();
    private Forwarded forwarded = new Forwarded();
    private Limits limits = new Limits();
    private Shedding shedding = new Shedding();
    private BackendTls backendTls = new BackendTls();
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

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout == null ? Duration.ofSeconds(1) : connectTimeout;
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

    public long getMaxResponseBytes() {
        return maxResponseBytes;
    }

    public void setMaxResponseBytes(long maxResponseBytes) {
        this.maxResponseBytes = maxResponseBytes;
    }

    public PrivateNetworkValidation getPrivateNetworkValidation() {
        return privateNetworkValidation;
    }

    public void setPrivateNetworkValidation(PrivateNetworkValidation privateNetworkValidation) {
        this.privateNetworkValidation = privateNetworkValidation == null
                ? new PrivateNetworkValidation()
                : privateNetworkValidation;
    }

    public PrivateNetworkLiveValidation getPrivateNetworkLiveValidation() {
        return privateNetworkLiveValidation;
    }

    public void setPrivateNetworkLiveValidation(PrivateNetworkLiveValidation privateNetworkLiveValidation) {
        this.privateNetworkLiveValidation = privateNetworkLiveValidation == null
                ? new PrivateNetworkLiveValidation()
                : privateNetworkLiveValidation;
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

    public SlowStart getSlowStart() {
        return slowStart;
    }

    public void setSlowStart(SlowStart slowStart) {
        this.slowStart = slowStart == null ? new SlowStart() : slowStart;
    }

    public Forwarded getForwarded() {
        return forwarded;
    }

    public void setForwarded(Forwarded forwarded) {
        this.forwarded = forwarded == null ? new Forwarded() : forwarded;
    }

    public Limits getLimits() {
        return limits;
    }

    public void setLimits(Limits limits) {
        this.limits = limits == null ? new Limits() : limits;
    }

    public Shedding getShedding() {
        return shedding;
    }

    public void setShedding(Shedding shedding) {
        this.shedding = shedding == null ? new Shedding() : shedding;
    }

    public BackendTls getBackendTls() {
        return backendTls;
    }

    public void setBackendTls(BackendTls backendTls) {
        this.backendTls = backendTls == null ? new BackendTls() : backendTls;
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
        private String hashOn = "client-ip";
        private Duration requestTimeout;
        private Affinity affinity = new Affinity();
        private Headers headers = new Headers();
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

        public String getHashOn() {
            return hashOn;
        }

        public void setHashOn(String hashOn) {
            this.hashOn = hashOn;
        }

        public Duration getRequestTimeout() {
            return requestTimeout;
        }

        public void setRequestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
        }

        public Affinity getAffinity() {
            return affinity;
        }

        public void setAffinity(Affinity affinity) {
            this.affinity = affinity == null ? new Affinity() : affinity;
        }

        public Headers getHeaders() {
            return headers;
        }

        public void setHeaders(Headers headers) {
            this.headers = headers == null ? new Headers() : headers;
        }

        public List<Upstream> getTargets() {
            return targets;
        }

        public void setTargets(List<Upstream> targets) {
            this.targets = targets == null ? new ArrayList<>() : new ArrayList<>(targets);
        }
    }

    public static final class Affinity {
        private String cookieName = "";
        private String hmacKey = "";

        public String getCookieName() {
            return cookieName;
        }

        public void setCookieName(String cookieName) {
            this.cookieName = cookieName;
        }

        public String getHmacKey() {
            return hmacKey;
        }

        public void setHmacKey(String hmacKey) {
            this.hmacKey = hmacKey;
        }
    }

    public static final class Forwarded {
        private String mode = "strip-and-set";
        private List<String> trustedProxies = new ArrayList<>();

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public List<String> getTrustedProxies() {
            return trustedProxies;
        }

        public void setTrustedProxies(List<String> trustedProxies) {
            this.trustedProxies = trustedProxies == null ? new ArrayList<>() : new ArrayList<>(trustedProxies);
        }
    }

    public static final class Headers {
        private Map<String, String> add = new LinkedHashMap<>();
        private Map<String, String> set = new LinkedHashMap<>();
        private Map<String, Boolean> remove = new LinkedHashMap<>();

        public Map<String, String> getAdd() {
            return add;
        }

        public void setAdd(Map<String, String> add) {
            this.add = add == null ? new LinkedHashMap<>() : new LinkedHashMap<>(add);
        }

        public Map<String, String> getSet() {
            return set;
        }

        public void setSet(Map<String, String> set) {
            this.set = set == null ? new LinkedHashMap<>() : new LinkedHashMap<>(set);
        }

        public Map<String, Boolean> getRemove() {
            return remove;
        }

        public void setRemove(Map<String, Boolean> remove) {
            this.remove = remove == null ? new LinkedHashMap<>() : new LinkedHashMap<>(remove);
        }
    }

    public static final class Limits {
        private int maxInFlight = 0;
        private boolean adaptive = false;

        public int getMaxInFlight() {
            return maxInFlight;
        }

        public void setMaxInFlight(int maxInFlight) {
            this.maxInFlight = maxInFlight;
        }

        public boolean isAdaptive() {
            return adaptive;
        }

        public void setAdaptive(boolean adaptive) {
            this.adaptive = adaptive;
        }
    }

    public static final class Shedding {
        private boolean enabled = false;
        private double softUtilizationThreshold = 0.75;
        private double hardUtilizationThreshold = 0.90;
        private int maxQueueDepth = 20;
        private double maxP95LatencyMillis = 250.0;
        private double maxErrorRate = 0.10;
        private boolean criticalBypassEnabled = true;
        private boolean shedUserOnHardPressure = true;
        private String priorityHeader = "";
        private Duration retryAfter = Duration.ofSeconds(1);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public double getSoftUtilizationThreshold() {
            return softUtilizationThreshold;
        }

        public void setSoftUtilizationThreshold(double softUtilizationThreshold) {
            this.softUtilizationThreshold = softUtilizationThreshold;
        }

        public double getHardUtilizationThreshold() {
            return hardUtilizationThreshold;
        }

        public void setHardUtilizationThreshold(double hardUtilizationThreshold) {
            this.hardUtilizationThreshold = hardUtilizationThreshold;
        }

        public int getMaxQueueDepth() {
            return maxQueueDepth;
        }

        public void setMaxQueueDepth(int maxQueueDepth) {
            this.maxQueueDepth = maxQueueDepth;
        }

        public double getMaxP95LatencyMillis() {
            return maxP95LatencyMillis;
        }

        public void setMaxP95LatencyMillis(double maxP95LatencyMillis) {
            this.maxP95LatencyMillis = maxP95LatencyMillis;
        }

        public double getMaxErrorRate() {
            return maxErrorRate;
        }

        public void setMaxErrorRate(double maxErrorRate) {
            this.maxErrorRate = maxErrorRate;
        }

        public boolean isCriticalBypassEnabled() {
            return criticalBypassEnabled;
        }

        public void setCriticalBypassEnabled(boolean criticalBypassEnabled) {
            this.criticalBypassEnabled = criticalBypassEnabled;
        }

        public boolean isShedUserOnHardPressure() {
            return shedUserOnHardPressure;
        }

        public void setShedUserOnHardPressure(boolean shedUserOnHardPressure) {
            this.shedUserOnHardPressure = shedUserOnHardPressure;
        }

        public String getPriorityHeader() {
            return priorityHeader;
        }

        public void setPriorityHeader(String priorityHeader) {
            this.priorityHeader = priorityHeader;
        }

        public Duration getRetryAfter() {
            return retryAfter;
        }

        public void setRetryAfter(Duration retryAfter) {
            this.retryAfter = retryAfter == null ? Duration.ofSeconds(1) : retryAfter;
        }
    }

    /**
     * Configured in-flight, queue, latency, and error-rate values are deprecated as continuously authoritative
     * routing telemetry. They remain binding-compatible cold-start seed/fallback values for one compatibility
     * window; live proxy observations replace each corresponding signal when evidence is available.
     */
    public static final class Upstream {
        private String id;
        private String url;
        private boolean healthy = true;
        private int inFlightRequestCount = 0;
        private Double configuredCapacity = 100.0;
        private Double estimatedConcurrencyLimit = 100.0;
        private int maxInFlight = 0;
        private double weight = 1.0;
        private double averageLatencyMillis = 1.0;
        private double p95LatencyMillis = 1.0;
        private double p99LatencyMillis = 1.0;
        private double recentErrorRate = 0.0;
        private Integer queueDepth = 0;
        private Tls tls = new Tls();

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

        public int getMaxInFlight() {
            return maxInFlight;
        }

        public void setMaxInFlight(int maxInFlight) {
            this.maxInFlight = maxInFlight;
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

        public Tls getTls() {
            return tls;
        }

        public void setTls(Tls tls) {
            this.tls = tls == null ? new Tls() : tls;
        }
    }

    public static final class BackendTls {
        private String truststore = "";

        public String getTruststore() {
            return truststore;
        }

        public void setTruststore(String truststore) {
            this.truststore = truststore == null ? "" : truststore;
        }
    }

    public static final class Tls {
        private boolean verify = true;
        private String clientCert = "";

        public boolean isVerify() {
            return verify;
        }

        public void setVerify(boolean verify) {
            this.verify = verify;
        }

        public String getClientCert() {
            return clientCert;
        }

        public void setClientCert(String clientCert) {
            this.clientCert = clientCert == null ? "" : clientCert;
        }
    }

    public static final class PrivateNetworkValidation {
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static final class PrivateNetworkLiveValidation {
        private boolean enabled = false;
        private boolean operatorApproved = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isOperatorApproved() {
            return operatorApproved;
        }

        public void setOperatorApproved(boolean operatorApproved) {
            this.operatorApproved = operatorApproved;
        }
    }

    public static final class HealthCheck {
        private boolean enabled = false;
        private String path = "/health";
        private Duration timeout = Duration.ofSeconds(1);
        private Duration interval = Duration.ofSeconds(30);
        private int healthyThreshold = 2;
        private int unhealthyThreshold = 3;

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

        public int getHealthyThreshold() {
            return healthyThreshold;
        }

        public void setHealthyThreshold(int healthyThreshold) {
            this.healthyThreshold = healthyThreshold;
        }

        public int getUnhealthyThreshold() {
            return unhealthyThreshold;
        }

        public void setUnhealthyThreshold(int unhealthyThreshold) {
            this.unhealthyThreshold = unhealthyThreshold;
        }
    }

    public static final class Retry {
        private boolean enabled = false;
        private int maxAttempts = 2;
        private int budgetPercent = 20;
        private Backoff backoff = new Backoff();
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

        public int getBudgetPercent() {
            return budgetPercent;
        }

        public void setBudgetPercent(int budgetPercent) {
            this.budgetPercent = budgetPercent;
        }

        public Backoff getBackoff() {
            return backoff;
        }

        public void setBackoff(Backoff backoff) {
            this.backoff = backoff == null ? new Backoff() : backoff;
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

    public static final class Backoff {
        private Duration base = Duration.ofMillis(50);
        private Duration max = Duration.ofSeconds(1);

        public Duration getBase() {
            return base;
        }

        public void setBase(Duration base) {
            this.base = base == null ? Duration.ofMillis(50) : base;
        }

        public Duration getMax() {
            return max;
        }

        public void setMax(Duration max) {
            this.max = max == null ? Duration.ofSeconds(1) : max;
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

    public static final class SlowStart {
        private Duration duration = Duration.ZERO;

        public Duration getDuration() {
            return duration;
        }

        public void setDuration(Duration duration) {
            this.duration = duration == null ? Duration.ZERO : duration;
        }
    }
}
