package com.guidedbyte.openapi.modelgen.util;

import com.guidedbyte.openapi.modelgen.services.LoggingContext;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance metrics utility for the OpenAPI Model Generator plugin.
 *
 * <p>Provides comprehensive performance tracking for build operations including:</p>
 * <ul>
 *   <li>Operation timing with structured logging</li>
 *   <li>Cache performance metrics (hits, misses, access times)</li>
 *   <li>Build statistics aggregation</li>
 *   <li>Thread-safe counters for parallel operations</li>
 * </ul>
 *
 * <p>All metrics are logged to both console (INFO level) and rich file logs with
 * structured context for easy analysis and troubleshooting.</p>
 *
 * <h2>Usage Examples:</h2>
 * <pre>{@code
 * // Basic timing
 * Timer timer = PerformanceMetrics.startTimer("template_extraction");
 * // ... perform operation
 * timer.stopAndLog();
 *
 * // Timing with additional context
 * Map<String, Object> context = Map.of("templates", 23, "cache_hits", 18);
 * timer.stopAndLog(context);
 *
 * // Cache performance tracking
 * PerformanceMetrics.logCachePerformance("template-cache", 45, 5, Duration.ofMillis(12));
 *
 * // Build statistics
 * BuildMetrics metrics = new BuildMetrics()
 *     .withTotalDuration(Duration.ofSeconds(15))
 *     .withSpecsProcessed(2)
 *     .withFilesGenerated(94)
 *     .withCacheHitRate(0.78)
 *     .withParallelEfficiency(0.85);
 * PerformanceMetrics.logBuildStatistics(metrics);
 * }</pre>
 *
 * <h2>Configuration Cache Compatibility</h2>
 * <p>This utility is fully compatible with Gradle's configuration cache:</p>
 * <ul>
 *   <li>Uses static logger instances only</li>
 *   <li>No Project references</li>
 *   <li>Thread-safe concurrent data structures</li>
 *   <li>Serializable metric objects</li>
 * </ul>
 *
 * @author GuidedByte Technologies Inc.
 * @since 2.1.0
 */
public final class PerformanceMetrics {

    private static final Logger logger = PluginLoggerFactory.getLogger(PerformanceMetrics.class);

    // Global performance counters for aggregation
    private static final AtomicLong totalOperations = new AtomicLong(0);
    private static final AtomicLong totalDuration = new AtomicLong(0);
    private static final Map<String, CacheStats> cacheStatsMap = new ConcurrentHashMap<>();

    private PerformanceMetrics() {
        // Utility class - prevent instantiation
    }

    /**
     * Starts a new timer for performance measurement.
     *
     * @param operation the name of the operation being timed
     * @return a Timer instance for tracking the operation
     */
    public static Timer startTimer(String operation) {
        return new Timer(operation);
    }

    /**
     * Logs timing information with optional context.
     *
     * @param operation the name of the operation
     * @param duration the duration of the operation
     * @param context additional context for the operation (can be null)
     */
    public static void logTiming(String operation, Duration duration, Map<String, Object> context) {
        totalOperations.incrementAndGet();
        totalDuration.addAndGet(duration.toMillis());

        StringBuilder message = new StringBuilder()
            .append("PERF: ")
            .append(operation)
            .append(" duration=").append(duration.toMillis()).append("ms");

        if (context != null && !context.isEmpty()) {
            context.forEach((key, value) ->
                message.append(" ").append(key).append("=").append(value));
        }

        logger.info(message.toString());
    }

    /**
     * Logs cache performance metrics.
     *
     * @param cacheType the type/name of the cache
     * @param hits number of cache hits
     * @param misses number of cache misses
     * @param accessTime average access time for the cache
     */
    public static void logCachePerformance(String cacheType, int hits, int misses, Duration accessTime) {
        // Update global cache statistics
        cacheStatsMap.compute(cacheType, (key, existing) -> {
            if (existing == null) {
                return new CacheStats(hits, misses, accessTime.toMillis());
            } else {
                return existing.add(hits, misses, accessTime.toMillis());
            }
        });

        double hitRate = hits + misses > 0 ? (double) hits / (hits + misses) * 100 : 0;

        logger.info("PERF: {} cache_hits={} cache_misses={} hit_rate={:.1f}% access_time={}ms",
            cacheType, hits, misses, hitRate, accessTime.toMillis());
    }

    /**
     * Logs comprehensive build statistics.
     *
     * @param metrics the build metrics to log
     */
    public static void logBuildStatistics(BuildMetrics metrics) {
        LoggingContext.setComponent("BuildSummary");
        try {
            logger.info("PERF: total_build duration={}ms specs={} cache_hit_rate={:.0f}% parallel_efficiency={:.0f}% files_generated={} memory_peak={}MB",
                metrics.getTotalDuration().toMillis(),
                metrics.getSpecsProcessed(),
                metrics.getCacheHitRate() * 100,
                metrics.getParallelEfficiency() * 100,
                metrics.getFilesGenerated(),
                metrics.getMemoryPeakMB());
        } finally {
            LoggingContext.clearTemplate(); // Keep spec context, clear component
        }
    }

    /**
     * Gets current global performance statistics.
     *
     * @return a map of global performance statistics
     */
    public static Map<String, Object> getGlobalStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_operations", totalOperations.get());
        stats.put("total_duration_ms", totalDuration.get());
        stats.put("average_duration_ms",
            totalOperations.get() > 0 ? totalDuration.get() / totalOperations.get() : 0);

        // Add cache statistics
        Map<String, Map<String, Object>> cacheStats = new HashMap<>();
        cacheStatsMap.forEach((cacheType, stats1) -> {
            Map<String, Object> cacheInfo = new HashMap<>();
            cacheInfo.put("hits", stats1.getHits());
            cacheInfo.put("misses", stats1.getMisses());
            cacheInfo.put("hit_rate", stats1.getHitRate());
            cacheInfo.put("avg_access_time_ms", stats1.getAverageAccessTime());
            cacheStats.put(cacheType, cacheInfo);
        });
        stats.put("cache_stats", cacheStats);

        return stats;
    }

    /**
     * Resets all global performance counters.
     * Useful for testing or starting fresh measurements.
     */
    public static void reset() {
        totalOperations.set(0);
        totalDuration.set(0);
        cacheStatsMap.clear();
    }

    /**
     * Timer utility for measuring operation duration.
     */
    public static class Timer {
        private final String operation;
        private final Instant startTime;

        private Timer(String operation) {
            this.operation = operation;
            this.startTime = Instant.now();
        }

        /**
         * Stops the timer and logs the duration.
         *
         * @return the duration of the operation
         */
        public Duration stopAndLog() {
            return stopAndLog(null);
        }

        /**
         * Stops the timer and logs the duration with additional context.
         *
         * @param context additional context for the operation
         * @return the duration of the operation
         */
        public Duration stopAndLog(Map<String, Object> context) {
            Duration duration = Duration.between(startTime, Instant.now());
            logTiming(operation, duration, context);
            return duration;
        }

        /**
         * Gets the current elapsed time without stopping the timer.
         *
         * @return the current elapsed duration
         */
        public Duration getElapsed() {
            return Duration.between(startTime, Instant.now());
        }

        /**
         * Gets the operation name.
         *
         * @return the operation name
         */
        public String getOperation() {
            return operation;
        }
    }

    /**
     * Build metrics container for comprehensive build statistics.
     */
    public static class BuildMetrics {
        private Duration totalDuration = Duration.ZERO;
        private int specsProcessed = 0;
        private int filesGenerated = 0;
        private double cacheHitRate = 0.0;
        private double parallelEfficiency = 0.0;
        private long memoryPeakMB = 0;

        public BuildMetrics withTotalDuration(Duration duration) {
            this.totalDuration = duration;
            return this;
        }

        public BuildMetrics withSpecsProcessed(int specs) {
            this.specsProcessed = specs;
            return this;
        }

        public BuildMetrics withFilesGenerated(int files) {
            this.filesGenerated = files;
            return this;
        }

        public BuildMetrics withCacheHitRate(double rate) {
            this.cacheHitRate = rate;
            return this;
        }

        public BuildMetrics withParallelEfficiency(double efficiency) {
            this.parallelEfficiency = efficiency;
            return this;
        }

        public BuildMetrics withMemoryPeakMB(long memoryMB) {
            this.memoryPeakMB = memoryMB;
            return this;
        }

        // Getters
        public Duration getTotalDuration() { return totalDuration; }
        public int getSpecsProcessed() { return specsProcessed; }
        public int getFilesGenerated() { return filesGenerated; }
        public double getCacheHitRate() { return cacheHitRate; }
        public double getParallelEfficiency() { return parallelEfficiency; }
        public long getMemoryPeakMB() { return memoryPeakMB; }
    }

    /**
     * Internal cache statistics tracking.
     */
    private static class CacheStats {
        private final long hits;
        private final long misses;
        private final long totalAccessTime;
        private final long accessCount;

        public CacheStats(long hits, long misses, long accessTime) {
            this.hits = hits;
            this.misses = misses;
            this.totalAccessTime = accessTime;
            this.accessCount = 1;
        }

        private CacheStats(long hits, long misses, long totalAccessTime, long accessCount) {
            this.hits = hits;
            this.misses = misses;
            this.totalAccessTime = totalAccessTime;
            this.accessCount = accessCount;
        }

        public CacheStats add(long additionalHits, long additionalMisses, long accessTime) {
            return new CacheStats(
                this.hits + additionalHits,
                this.misses + additionalMisses,
                this.totalAccessTime + accessTime,
                this.accessCount + 1
            );
        }

        public long getHits() { return hits; }
        public long getMisses() { return misses; }
        public double getHitRate() {
            return hits + misses > 0 ? (double) hits / (hits + misses) : 0.0;
        }
        public double getAverageAccessTime() {
            return accessCount > 0 ? (double) totalAccessTime / accessCount : 0.0;
        }
    }
}