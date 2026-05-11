package com.skyblockexp.ezrtp.performance;

import java.util.Locale;

/** Immutable point-in-time snapshot of EzRTP performance metrics. */
public final class PerformanceSnapshot {

    private final String generatedAt;
    private final long totalRtps;
    private final long successRtps;
    private final long failureRtps;
    private final double successRatePercent;
    private final double timingP50Ms;
    private final double timingP75Ms;
    private final double timingP90Ms;
    private final double timingP95Ms;
    private final double timingP99Ms;
    private final long timingMinMs;
    private final long timingMaxMs;
    private final long slowOperations;
    private final double cacheHitRatePercent;
    private final long cacheHits;
    private final long cacheMisses;
    private final long cacheEvictions;
    private final long chunkLoadsTotal;
    private final long chunkLoadsAsync;
    private final long chunkLoadsSync;
    private final long biomeRejectionsTotal;
    private final long biomeRejectionsMaxPerRtp;
    private final long biomeFilterTimeouts;

    public PerformanceSnapshot(
            String generatedAt,
            long totalRtps,
            long successRtps,
            long failureRtps,
            double successRatePercent,
            double timingP50Ms,
            double timingP75Ms,
            double timingP90Ms,
            double timingP95Ms,
            double timingP99Ms,
            long timingMinMs,
            long timingMaxMs,
            long slowOperations,
            double cacheHitRatePercent,
            long cacheHits,
            long cacheMisses,
            long cacheEvictions,
            long chunkLoadsTotal,
            long chunkLoadsAsync,
            long chunkLoadsSync,
            long biomeRejectionsTotal,
            long biomeRejectionsMaxPerRtp,
            long biomeFilterTimeouts) {
        this.generatedAt = generatedAt;
        this.totalRtps = totalRtps;
        this.successRtps = successRtps;
        this.failureRtps = failureRtps;
        this.successRatePercent = successRatePercent;
        this.timingP50Ms = timingP50Ms;
        this.timingP75Ms = timingP75Ms;
        this.timingP90Ms = timingP90Ms;
        this.timingP95Ms = timingP95Ms;
        this.timingP99Ms = timingP99Ms;
        this.timingMinMs = timingMinMs;
        this.timingMaxMs = timingMaxMs;
        this.slowOperations = slowOperations;
        this.cacheHitRatePercent = cacheHitRatePercent;
        this.cacheHits = cacheHits;
        this.cacheMisses = cacheMisses;
        this.cacheEvictions = cacheEvictions;
        this.chunkLoadsTotal = chunkLoadsTotal;
        this.chunkLoadsAsync = chunkLoadsAsync;
        this.chunkLoadsSync = chunkLoadsSync;
        this.biomeRejectionsTotal = biomeRejectionsTotal;
        this.biomeRejectionsMaxPerRtp = biomeRejectionsMaxPerRtp;
        this.biomeFilterTimeouts = biomeFilterTimeouts;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public long getTotalRtps() {
        return totalRtps;
    }

    public long getSuccessRtps() {
        return successRtps;
    }

    public long getFailureRtps() {
        return failureRtps;
    }

    public double getSuccessRatePercent() {
        return successRatePercent;
    }

    public double getTimingP50Ms() {
        return timingP50Ms;
    }

    public double getTimingP75Ms() {
        return timingP75Ms;
    }

    public double getTimingP90Ms() {
        return timingP90Ms;
    }

    public double getTimingP95Ms() {
        return timingP95Ms;
    }

    public double getTimingP99Ms() {
        return timingP99Ms;
    }

    public long getTimingMinMs() {
        return timingMinMs;
    }

    public long getTimingMaxMs() {
        return timingMaxMs;
    }

    public long getSlowOperations() {
        return slowOperations;
    }

    public double getCacheHitRatePercent() {
        return cacheHitRatePercent;
    }

    public long getCacheHits() {
        return cacheHits;
    }

    public long getCacheMisses() {
        return cacheMisses;
    }

    public long getCacheEvictions() {
        return cacheEvictions;
    }

    public long getChunkLoadsTotal() {
        return chunkLoadsTotal;
    }

    public long getChunkLoadsAsync() {
        return chunkLoadsAsync;
    }

    public long getChunkLoadsSync() {
        return chunkLoadsSync;
    }

    public long getBiomeRejectionsTotal() {
        return biomeRejectionsTotal;
    }

    public long getBiomeRejectionsMaxPerRtp() {
        return biomeRejectionsMaxPerRtp;
    }

    public long getBiomeFilterTimeouts() {
        return biomeFilterTimeouts;
    }

    /** Serialises this snapshot as a pretty-printed JSON string. */
    public String toJson() {
        StringBuilder sb = new StringBuilder(512);
        sb.append("{\n");
        sb.append("  \"generatedAt\": \"").append(generatedAt).append("\",\n");
        sb.append("  \"rtp\": {\n");
        sb.append("    \"total\": ").append(totalRtps).append(",\n");
        sb.append("    \"success\": ").append(successRtps).append(",\n");
        sb.append("    \"failure\": ").append(failureRtps).append(",\n");
        sb.append("    \"successRatePercent\": ").append(fmt(successRatePercent)).append("\n");
        sb.append("  },\n");
        sb.append("  \"timing\": {\n");
        sb.append("    \"p50ms\": ").append(fmt(timingP50Ms)).append(",\n");
        sb.append("    \"p75ms\": ").append(fmt(timingP75Ms)).append(",\n");
        sb.append("    \"p90ms\": ").append(fmt(timingP90Ms)).append(",\n");
        sb.append("    \"p95ms\": ").append(fmt(timingP95Ms)).append(",\n");
        sb.append("    \"p99ms\": ").append(fmt(timingP99Ms)).append(",\n");
        sb.append("    \"minMs\": ").append(timingMinMs).append(",\n");
        sb.append("    \"maxMs\": ").append(timingMaxMs).append(",\n");
        sb.append("    \"slowOperations\": ").append(slowOperations).append("\n");
        sb.append("  },\n");
        sb.append("  \"cache\": {\n");
        sb.append("    \"hitRatePercent\": ").append(fmt(cacheHitRatePercent)).append(",\n");
        sb.append("    \"hits\": ").append(cacheHits).append(",\n");
        sb.append("    \"misses\": ").append(cacheMisses).append(",\n");
        sb.append("    \"evictions\": ").append(cacheEvictions).append("\n");
        sb.append("  },\n");
        sb.append("  \"chunkLoading\": {\n");
        sb.append("    \"total\": ").append(chunkLoadsTotal).append(",\n");
        sb.append("    \"async\": ").append(chunkLoadsAsync).append(",\n");
        sb.append("    \"sync\": ").append(chunkLoadsSync).append("\n");
        sb.append("  },\n");
        sb.append("  \"biomeFiltering\": {\n");
        sb.append("    \"totalRejections\": ").append(biomeRejectionsTotal).append(",\n");
        sb.append("    \"maxRejectionsPerRtp\": ").append(biomeRejectionsMaxPerRtp).append(",\n");
        sb.append("    \"filterTimeouts\": ").append(biomeFilterTimeouts).append("\n");
        sb.append("  }\n");
        sb.append("}");
        return sb.toString();
    }

    private static String fmt(double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}
