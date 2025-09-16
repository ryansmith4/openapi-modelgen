package com.guidedbyte.openapi.modelgen.util;

import com.guidedbyte.openapi.modelgen.services.LoggingContext;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Build progress tracking and reporting for the OpenAPI Model Generator plugin.
 *
 * <p>Provides real-time progress indicators and comprehensive build summaries with:</p>
 * <ul>
 *   <li><strong>Console Progress:</strong> User-friendly progress messages for long operations</li>
 *   <li><strong>Rich File Logging:</strong> Detailed progress reports for troubleshooting</li>
 *   <li><strong>Phase Tracking:</strong> Template extraction, code generation, validation phases</li>
 *   <li><strong>Time Estimation:</strong> Predicts remaining time based on current progress</li>
 *   <li><strong>Resource Monitoring:</strong> Memory usage, parallel efficiency, cache performance</li>
 * </ul>
 *
 * <h2>Usage Examples:</h2>
 * <pre>{@code
 * // Initialize build tracking
 * BuildProgressTracker.startBuild(2, true); // 2 specs, parallel processing
 *
 * // Track spec progress
 * BuildProgressTracker.logSpecProgress("pets", 1, 2, Duration.ofSeconds(1), Duration.ofSeconds(2));
 *
 * // Phase transitions
 * ProgressInfo info = new ProgressInfo().withElapsed(Duration.ofSeconds(1)).withEstimatedRemaining(Duration.ofSeconds(2));
 * BuildProgressTracker.logPhaseTransition("template_extraction", "pets", info);
 *
 * // Complete build with summary
 * BuildSummaryMetrics summary = new BuildSummaryMetrics()
 *     .withTotalDuration(Duration.ofSeconds(15))
 *     .withSpecsProcessed(2)
 *     .withFilesGenerated(94)
 *     .withCacheHitRate(0.78)
 *     .withParallelEfficiency(0.85)
 *     .withMemoryPeakMB(256);
 * BuildProgressTracker.logBuildSummary(summary);
 * }</pre>
 *
 * <h2>Console Output Examples:</h2>
 * <pre>
 * [INFO] Processing spec pets (1/2) - Template extraction: 1.2s elapsed, ~2.1s remaining
 * [INFO] Processing spec pets (1/2) - Code generation: 3.4s elapsed, ~1.8s remaining
 * [INFO] Build completed successfully in 15.7s (Cache hit rate: 78%, Parallel efficiency: 85%)
 * </pre>
 *
 * @author GuidedByte Technologies Inc.
 * @since 2.1.0
 */
public final class BuildProgressTracker {

    private static final PluginLogger logger = (PluginLogger) PluginLoggerFactory.getLogger(BuildProgressTracker.class);

    // Build-level tracking
    private static volatile Instant buildStartTime;
    private static volatile int totalSpecs = 0;
    private static volatile boolean parallelProcessing = false;
    private static final AtomicInteger completedSpecs = new AtomicInteger(0);
    private static final AtomicLong totalFilesGenerated = new AtomicLong(0);

    // Spec-level tracking
    private static final Map<String, SpecProgress> specProgressMap = new ConcurrentHashMap<>();

    // Phase timing for estimation
    private static final Map<String, Long> phaseAverageDurations = new ConcurrentHashMap<>();

    private BuildProgressTracker() {
        // Utility class - prevent instantiation
    }

    /**
     * Initializes build progress tracking.
     *
     * @param totalSpecs total number of specs to be processed
     * @param parallelProcessing whether parallel processing is enabled
     */
    public static void startBuild(int totalSpecs, boolean parallelProcessing) {
        BuildProgressTracker.buildStartTime = Instant.now();
        BuildProgressTracker.totalSpecs = totalSpecs;
        BuildProgressTracker.parallelProcessing = parallelProcessing;
        completedSpecs.set(0);
        totalFilesGenerated.set(0);
        specProgressMap.clear();

        LoggingContext.setComponent("BuildProgress");
        try {
            logger.info("BUILD_START: total_specs={} parallel={}", totalSpecs, parallelProcessing);

            // Console progress message
            if (totalSpecs > 1) {
                logger.info("Starting OpenAPI model generation for {} specifications{}",
                    totalSpecs, parallelProcessing ? " (parallel processing)" : "");
            } else {
                logger.info("Starting OpenAPI model generation");
            }
        } finally {
            // Keep component context for build tracking
        }
    }

    /**
     * Logs progress for a specific spec.
     *
     * @param specName the name of the spec being processed
     * @param current current spec number (1-based)
     * @param total total number of specs
     * @param elapsed time elapsed for this spec
     * @param estimated estimated remaining time for this spec
     */
    public static void logSpecProgress(String specName, int current, int total,
                                     Duration elapsed, Duration estimated) {
        LoggingContext.setSpec(specName);
        LoggingContext.setComponent("BuildProgress");
        try {
            // Update spec progress
            specProgressMap.put(specName, new SpecProgress(current, elapsed, estimated));

            // Rich file logging
            logger.debug("SPEC_PROGRESS: spec={} progress={}/{} elapsed={}ms estimated_remaining={}ms",
                specName, current, total, elapsed.toMillis(), estimated.toMillis());

            // Console progress (more user-friendly)
            if (total > 1) {
                logger.info("Processing spec {} ({}/{}) - {:.1f}s elapsed, ~{:.1f}s remaining",
                    specName, current, total, elapsed.toMillis() / 1000.0, estimated.toMillis() / 1000.0);
            } else {
                logger.info("Processing spec {} - {:.1f}s elapsed, ~{:.1f}s remaining",
                    specName, elapsed.toMillis() / 1000.0, estimated.toMillis() / 1000.0);
            }
        } finally {
            // Keep context for subsequent operations
        }
    }

    /**
     * Logs phase transitions for a spec.
     *
     * @param phase the phase being entered
     * @param specName the spec being processed
     * @param info progress information for the phase
     */
    public static void logPhaseTransition(String phase, String specName, ProgressInfo info) {
        LoggingContext.setSpec(specName);
        LoggingContext.setComponent("BuildProgress");
        try {
            // Update phase timing averages for better estimation
            if (info.getElapsed() != null && info.getElapsed().toMillis() > 0) {
                phaseAverageDurations.compute(phase, (key, existing) -> {
                    long newDuration = info.getElapsed().toMillis();
                    return existing == null ? newDuration : (existing + newDuration) / 2;
                });
            }

            // Rich file logging
            logger.debug("PHASE_TRANSITION: spec={} phase={} elapsed={}ms estimated_remaining={}ms",
                specName, phase,
                info.getElapsed() != null ? info.getElapsed().toMillis() : 0,
                info.getEstimatedRemaining() != null ? info.getEstimatedRemaining().toMillis() : 0);

            // Console progress for major phases
            if (shouldLogPhaseToConsole(phase)) {
                String phaseDisplay = formatPhaseForConsole(phase);
                if (totalSpecs > 1) {
                    int currentSpec = getCurrentSpecNumber(specName);
                    logger.info("Processing spec {} ({}/{}) - {}: {:.1f}s elapsed{}",
                        specName, currentSpec, totalSpecs, phaseDisplay,
                        info.getElapsed() != null ? info.getElapsed().toMillis() / 1000.0 : 0,
                        info.getEstimatedRemaining() != null ?
                            String.format(", ~%.1fs remaining", info.getEstimatedRemaining().toMillis() / 1000.0) : "");
                }
            }
        } finally {
            // Keep context for subsequent operations
        }
    }

    /**
     * Logs spec completion.
     *
     * @param specName the spec that was completed
     * @param duration total processing time for the spec
     * @param filesGenerated number of files generated for this spec
     * @param cacheHits number of cache hits during processing
     */
    public static void logSpecComplete(String specName, Duration duration, int filesGenerated, int cacheHits) {
        LoggingContext.setSpec(specName);
        LoggingContext.setComponent("BuildProgress");
        try {
            int completed = completedSpecs.incrementAndGet();
            totalFilesGenerated.addAndGet(filesGenerated);

            // Rich file logging
            logger.debug("SPEC_COMPLETE: spec={} duration={}ms files_generated={} cache_hits={} progress={}/{}",
                specName, duration.toMillis(), filesGenerated, cacheHits, completed, totalSpecs);

            // Console progress
            logger.info("Completed spec {} in {:.1f}s ({} files generated, {} cache hits) [{}/{}]",
                specName, duration.toMillis() / 1000.0, filesGenerated, cacheHits, completed, totalSpecs);

            // Performance metric
            Map<String, Object> context = Map.of(
                "spec", specName,
                "files_generated", filesGenerated,
                "cache_hits", cacheHits
            );
            PerformanceMetrics.logTiming("spec_processing", duration, context);
        } finally {
            LoggingContext.clearTemplate(); // Keep spec context
        }
    }

    /**
     * Logs comprehensive build completion summary.
     *
     * @param summary the build summary metrics
     */
    public static void logBuildSummary(BuildSummaryMetrics summary) {
        LoggingContext.setComponent("BuildSummary");
        try {
            Duration buildDuration = summary.getTotalDuration();
            if (buildStartTime != null && buildDuration.isZero()) {
                buildDuration = Duration.between(buildStartTime, Instant.now());
            }
            final Duration finalBuildDuration = buildDuration; // For lambda capture

            // Build completion always visible at INFO level
            logger.info("Build completed successfully in {:.1f}s", finalBuildDuration.toMillis() / 1000.0);

            // Performance summary at INFO level
            logger.info("BUILD_PERFORMANCE: cache_hit_rate={:.0f}% parallel_efficiency={:.0f}% memory_peak={}MB",
                summary.getCacheHitRate() * 100,
                summary.getParallelEfficiency() * 100,
                summary.getMemoryPeakMB());

            // Detailed metrics at DEBUG level
            logger.debug("BUILD_DETAILS: total_duration={}ms specs_processed={} files_generated={}",
                finalBuildDuration.toMillis(),
                summary.getSpecsProcessed(),
                summary.getFilesGenerated());

            // Rich file logging with comprehensive details (always for troubleshooting)
            logger.ifDebug(() -> {
                logger.info("=== Build Summary ===");
                logger.info("BUILD_COMPLETE: total_duration={}ms specs_processed={} files_generated={} " +
                           "cache_hit_rate={:.0f}% parallel_efficiency={:.0f}% memory_peak={}MB",
                    finalBuildDuration.toMillis(),
                    summary.getSpecsProcessed(),
                    summary.getFilesGenerated(),
                    summary.getCacheHitRate() * 100,
                    summary.getParallelEfficiency() * 100,
                    summary.getMemoryPeakMB());
            });

            // Log to performance metrics
            PerformanceMetrics.BuildMetrics buildMetrics = new PerformanceMetrics.BuildMetrics()
                .withTotalDuration(finalBuildDuration)
                .withSpecsProcessed(summary.getSpecsProcessed())
                .withFilesGenerated(summary.getFilesGenerated())
                .withCacheHitRate(summary.getCacheHitRate())
                .withParallelEfficiency(summary.getParallelEfficiency())
                .withMemoryPeakMB(summary.getMemoryPeakMB());
            PerformanceMetrics.logBuildStatistics(buildMetrics);
        } finally {
            LoggingContext.clear();
        }
    }

    /**
     * Gets the current build progress as a percentage.
     *
     * @return build progress percentage (0.0 to 1.0)
     */
    public static double getBuildProgress() {
        if (totalSpecs == 0) return 0.0;
        return (double) completedSpecs.get() / totalSpecs;
    }

    /**
     * Gets the estimated time remaining for the build.
     *
     * @return estimated remaining duration, or null if cannot be estimated
     */
    public static Duration getEstimatedTimeRemaining() {
        if (buildStartTime == null || totalSpecs == 0 || completedSpecs.get() == 0) {
            return null;
        }

        Duration elapsed = Duration.between(buildStartTime, Instant.now());
        double progress = getBuildProgress();

        if (progress >= 1.0) return Duration.ZERO;
        if (progress <= 0.0) return null;

        long estimatedTotalMs = (long) (elapsed.toMillis() / progress);
        long remainingMs = estimatedTotalMs - elapsed.toMillis();

        return remainingMs > 0 ? Duration.ofMillis(remainingMs) : Duration.ZERO;
    }

    /**
     * Resets all progress tracking state.
     * Useful for testing or multiple builds in the same process.
     */
    public static void reset() {
        buildStartTime = null;
        totalSpecs = 0;
        parallelProcessing = false;
        completedSpecs.set(0);
        totalFilesGenerated.set(0);
        specProgressMap.clear();
        phaseAverageDurations.clear();
    }

    // Helper methods

    private static boolean shouldLogPhaseToConsole(String phase) {
        // Only log major phases to console to avoid spam
        return "template_extraction".equals(phase) ||
               "code_generation".equals(phase) ||
               "validation".equals(phase);
    }

    private static String formatPhaseForConsole(String phase) {
        switch (phase) {
            case "template_extraction": return "Template extraction";
            case "code_generation": return "Code generation";
            case "validation": return "Validation";
            default: return phase.replace('_', ' ');
        }
    }

    private static int getCurrentSpecNumber(String specName) {
        SpecProgress progress = specProgressMap.get(specName);
        return progress != null ? progress.getCurrentNumber() : completedSpecs.get() + 1;
    }

    /**
     * Progress information for a phase or operation.
     */
    public static class ProgressInfo {
        private Duration elapsed;
        private Duration estimatedRemaining;
        private Map<String, Object> additionalInfo;

        public ProgressInfo withElapsed(Duration elapsed) {
            this.elapsed = elapsed;
            return this;
        }

        public ProgressInfo withEstimatedRemaining(Duration estimatedRemaining) {
            this.estimatedRemaining = estimatedRemaining;
            return this;
        }

        public ProgressInfo withAdditionalInfo(Map<String, Object> additionalInfo) {
            this.additionalInfo = additionalInfo;
            return this;
        }

        // Getters
        public Duration getElapsed() { return elapsed; }
        public Duration getEstimatedRemaining() { return estimatedRemaining; }
        public Map<String, Object> getAdditionalInfo() { return additionalInfo; }
    }

    /**
     * Build summary metrics container.
     */
    public static class BuildSummaryMetrics {
        private Duration totalDuration = Duration.ZERO;
        private int specsProcessed = 0;
        private int filesGenerated = 0;
        private double cacheHitRate = 0.0;
        private double parallelEfficiency = 0.0;
        private long memoryPeakMB = 0;

        public BuildSummaryMetrics withTotalDuration(Duration duration) {
            this.totalDuration = duration;
            return this;
        }

        public BuildSummaryMetrics withSpecsProcessed(int specs) {
            this.specsProcessed = specs;
            return this;
        }

        public BuildSummaryMetrics withFilesGenerated(int files) {
            this.filesGenerated = files;
            return this;
        }

        public BuildSummaryMetrics withCacheHitRate(double rate) {
            this.cacheHitRate = rate;
            return this;
        }

        public BuildSummaryMetrics withParallelEfficiency(double efficiency) {
            this.parallelEfficiency = efficiency;
            return this;
        }

        public BuildSummaryMetrics withMemoryPeakMB(long memoryMB) {
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
     * Internal spec progress tracking.
     */
    private static class SpecProgress {
        private final int currentNumber;
        private final Duration elapsed;
        private final Duration estimated;

        public SpecProgress(int currentNumber, Duration elapsed, Duration estimated) {
            this.currentNumber = currentNumber;
            this.elapsed = elapsed;
            this.estimated = estimated;
        }

        public int getCurrentNumber() { return currentNumber; }
        public Duration getElapsed() { return elapsed; }
        public Duration getEstimated() { return estimated; }
    }
}