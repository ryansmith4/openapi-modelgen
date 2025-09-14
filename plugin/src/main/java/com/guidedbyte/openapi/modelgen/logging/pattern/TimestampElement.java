package com.guidedbyte.openapi.modelgen.logging.pattern;

import com.guidedbyte.openapi.modelgen.constants.LoggingConstants;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * A pattern element that represents a timestamp in a log pattern.
 * 
 * <p>Examples: %d, %d{HH:mm:ss}, %d{ISO8601}, %d{yyyy-MM-dd HH:mm:ss.SSS}</p>
 * 
 * @author GuidedByte Technologies Inc.
 * @since 2.1.0
 */
public class TimestampElement implements PatternElement {
    private final DateTimeFormatter formatter;
    private final FormatModifier modifier;
    private final int estimatedLength;
    
    public TimestampElement(String datePattern, FormatModifier modifier) {
        this.modifier = modifier != null ? modifier : FormatModifier.NONE;
        this.formatter = createFormatter(datePattern);
        this.estimatedLength = estimateTimestampLength(datePattern);
    }
    
    public TimestampElement(String datePattern) {
        this(datePattern, FormatModifier.NONE);
    }
    
    public TimestampElement() {
        this("HH:mm:ss"); // Default format
    }
    
    @Override
    public void append(StringBuilder sb, String message, String spec, String template, String component) {
        String timestamp = LocalDateTime.now().format(formatter);
        String formatted = modifier.apply(timestamp);
        sb.append(formatted);
    }
    
    @Override
    public int estimateMaxLength() {
        int baseEstimate = estimatedLength;
        
        // Apply format modifier constraints
        if (modifier.getMaxWidth() > 0) {
            baseEstimate = Math.min(baseEstimate, modifier.getMaxWidth());
        }
        if (modifier.getMinWidth() > 0) {
            baseEstimate = Math.max(baseEstimate, modifier.getMinWidth());
        }
        
        return baseEstimate;
    }
    
    private DateTimeFormatter createFormatter(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return DateTimeFormatter.ofPattern("HH:mm:ss");
        }
        
        switch (StringUtils.toRootUpperCase(pattern)) {
            case "ISO8601":
                return DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            case "ABSOLUTE":
                return DateTimeFormatter.ofPattern("HH:mm:ss,SSS");
            case "DATE":
                return DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss,SSS");
            default:
                try {
                    return DateTimeFormatter.ofPattern(pattern);
                } catch (Exception e) {
                    // Fallback to default format if pattern is invalid
                    return DateTimeFormatter.ofPattern("HH:mm:ss");
                }
        }
    }
    
    private int estimateTimestampLength(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return 8; // HH:mm:ss
        }
        
        switch (StringUtils.toRootUpperCase(pattern)) {
            case "ISO8601":
                return LoggingConstants.ISO_TIMESTAMP_LENGTH; // 2023-12-25T14:30:45.123
            case "ABSOLUTE":
                return LoggingConstants.TIME_ONLY_LENGTH; // HH:mm:ss,SSS
            case "DATE":
                return LoggingConstants.READABLE_TIMESTAMP_LENGTH; // 25 Dec 2023 14:30:45,123
            default:
                // Estimate based on pattern length + some buffer
                return Math.max(pattern.length() + 5, 8);
        }
    }
    
    public String getDatePattern() {
        return formatter.toString();
    }
    
    public FormatModifier getModifier() {
        return modifier;
    }
    
    @Override
    public String toString() {
        if (modifier.isNone()) {
            return "TimestampElement[" + getDatePattern() + "]";
        } else {
            return "TimestampElement[" + getDatePattern() + ", " + modifier + "]";
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimestampElement)) return false;
        
        TimestampElement that = (TimestampElement) o;
        return formatter.equals(that.formatter) && modifier.equals(that.modifier);
    }
    
    @Override
    public int hashCode() {
        return LoggingConstants.HASH_PRIME * formatter.hashCode() + modifier.hashCode();
    }
}