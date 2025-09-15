package com.guidedbyte.openapi.modelgen.utils;

import com.guidedbyte.openapi.modelgen.ResolvedSpecConfig;
import org.semver4j.Semver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized utility class for all version comparison operations.
 * Consolidates duplicate version logic from multiple classes and provides
 * consistent semantic version handling using the semver4j library.
 *
 * <p>This class replaces duplicate implementations found in:</p>
 * <ul>
 *   <li>OpenApiModelGenPlugin</li>
 *   <li>ConfigurationValidator</li>
 *   <li>LibraryProcessor</li>
 * </ul>
 *
 * @author GuidedByte Technologies Inc.
 * @since 1.0.0
 */
public final class VersionUtils {
    private static final Logger logger = LoggerFactory.getLogger(VersionUtils.class);

    // Private constructor to prevent instantiation
    private VersionUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Gets the current plugin version for validation.
     *
     * @return the current plugin version, or null if not available
     */
    public static String getCurrentPluginVersion() {
        return ResolvedSpecConfig.getPluginVersion();
    }

    /**
     * Checks if the current version meets the minimum requirement.
     * Uses proper semantic versioning comparison.
     *
     * @param currentVersion the current version to check
     * @param minVersion the minimum required version
     * @return true if current version meets minimum requirement, false otherwise
     */
    public static boolean isVersionCompatible(String currentVersion, String minVersion) {
        if (currentVersion == null || minVersion == null) {
            return false;
        }

        try {
            Semver current = normalizeAndParseSemver(currentVersion);
            Semver minimum = normalizeAndParseSemver(minVersion);

            boolean compatible = current.isGreaterThanOrEqualTo(minimum);
            logger.debug("Version compatibility check: {} >= {} = {}", currentVersion, minVersion, compatible);
            return compatible;

        } catch (Exception e) {
            logger.warn("Failed to parse versions for compatibility check: '{}' >= '{}': {}",
                       currentVersion, minVersion, e.getMessage());
            // Fallback to string comparison for malformed versions
            return currentVersion.compareTo(minVersion) >= 0;
        }
    }

    /**
     * Checks if the current version is incompatible with (below) the minimum requirement.
     * This is a convenience method that provides clearer intent than negating isVersionCompatible().
     *
     * @param currentVersion the current version to check
     * @param minVersion the minimum required version
     * @return true if current version is incompatible (below minimum), false otherwise
     */
    public static boolean isVersionIncompatible(String currentVersion, String minVersion) {
        return !isVersionCompatible(currentVersion, minVersion);
    }

    /**
     * Checks if a version is below the minimum version.
     *
     * @param version the version to check
     * @param minVersion the minimum version threshold
     * @return true if version is below minimum, false otherwise
     */
    public static boolean isVersionBelow(String version, String minVersion) {
        if (version == null || minVersion == null) {
            return false;
        }

        try {
            Semver versionSemver = normalizeAndParseSemver(version);
            Semver minSemver = normalizeAndParseSemver(minVersion);

            boolean below = versionSemver.isLowerThan(minSemver);
            logger.debug("Version below check: {} < {} = {}", version, minVersion, below);
            return below;

        } catch (Exception e) {
            logger.warn("Failed to parse versions for below check: '{}' < '{}': {}",
                       version, minVersion, e.getMessage());
            // If we can't parse, assume it's compatible
            return false;
        }
    }

    /**
     * Checks if a version is above the maximum version.
     *
     * @param version the version to check
     * @param maxVersion the maximum version threshold
     * @return true if version is above maximum, false otherwise
     */
    public static boolean isVersionAbove(String version, String maxVersion) {
        if (version == null || maxVersion == null) {
            return false;
        }

        try {
            Semver versionSemver = normalizeAndParseSemver(version);
            Semver maxSemver = normalizeAndParseSemver(maxVersion);

            boolean above = versionSemver.isGreaterThan(maxSemver);
            logger.debug("Version above check: {} > {} = {}", version, maxVersion, above);
            return above;

        } catch (Exception e) {
            logger.warn("Failed to parse versions for above check: '{}' > '{}': {}",
                       version, maxVersion, e.getMessage());
            // If we can't parse, assume it's compatible
            return false;
        }
    }


    /**
     * Normalizes a version string and parses it as a semantic version.
     * Handles common version format variations and normalizes them for semver4j.
     *
     * @param version the version string to normalize and parse
     * @return the normalized Semver object
     * @throws IllegalArgumentException if the version cannot be normalized or parsed
     */
    public static Semver normalizeAndParseSemver(String version) throws IllegalArgumentException {
        if (version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("Version cannot be null or empty");
        }

        String normalized = normalizeVersion(version.trim());
        return Semver.parse(normalized);
    }

    /**
     * Normalizes version strings to be compatible with semantic versioning.
     * Handles various version formats and converts them to proper semantic versions.
     *
     * @param version the version string to normalize
     * @return the normalized version string
     */
    public static String normalizeVersion(String version) {
        if (version == null || version.trim().isEmpty()) {
            return "0.0.0";
        }

        // Remove leading 'v' prefix if present
        String normalized = version.trim();
        if (normalized.toLowerCase().startsWith("v")) {
            normalized = normalized.substring(1);
        }

        // Split version parts
        String[] parts = normalized.split("[.-]");

        // Ensure we have at least 3 numeric parts for semantic versioning
        int major = parseVersionPart(parts.length > 0 ? parts[0] : "0");
        int minor = parseVersionPart(parts.length > 1 ? parts[1] : "0");
        int patch = parseVersionPart(parts.length > 2 ? parts[2] : "0");

        // Build pre-release and build metadata if present
        StringBuilder sb = new StringBuilder();
        sb.append(major).append('.').append(minor).append('.').append(patch);

        // Handle pre-release identifiers (SNAPSHOT, RC, beta, etc.)
        if (parts.length > 3) {
            boolean hasPreRelease = false;
            for (int i = 3; i < parts.length; i++) {
                String part = parts[i];
                if (!part.matches("\\d+")) { // Non-numeric part indicates pre-release
                    if (!hasPreRelease) {
                        sb.append('-');
                        hasPreRelease = true;
                    } else {
                        sb.append('.');
                    }
                    sb.append(part.toLowerCase());
                }
            }
        }

        String result = sb.toString();
        logger.debug("Normalized version '{}' to '{}'", version, result);
        return result;
    }

    /**
     * Parses a version part, extracting the numeric portion and handling non-numeric suffixes.
     * This is the same logic that was duplicated across multiple classes.
     *
     * @param part the version part to parse
     * @return the numeric value of the version part
     */
    private static int parseVersionPart(String part) {
        if (part == null || part.trim().isEmpty()) {
            return 0;
        }

        try {
            // Extract numeric part (ignore suffixes like "SNAPSHOT", "RC", etc.)
            String numericPart = part.replaceAll("[^0-9].*", "");
            return numericPart.isEmpty() ? 0 : Integer.parseInt(numericPart);
        } catch (NumberFormatException e) {
            logger.debug("Failed to parse version part '{}' as number: {}", part, e.getMessage());
            return 0;
        }
    }
}