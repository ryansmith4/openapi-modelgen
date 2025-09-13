package com.guidedbyte.openapi.modelgen.logging.pattern;

/**
 * Represents SLF4J format modifiers for log pattern elements.
 * 
 * <p>Supports format modifiers like:</p>
 * <ul>
 *   <li><code>%-20X{spec}</code> - Left-align, minimum 20 characters</li>
 *   <li><code>%15X{template}</code> - Right-align, minimum 15 characters</li>
 *   <li><code>%.30X{component}</code> - Truncate to maximum 30 characters</li>
 *   <li><code>%-15.30X{spec}</code> - Left-align, minimum 15, maximum 30 characters</li>
 * </ul>
 * 
 * @author GuidedByte Technologies Inc.
 * @since 2.1.0
 */
public class FormatModifier {
    public static final FormatModifier NONE = new FormatModifier(false, 0, 0);
    
    private final boolean leftAlign;
    private final int minWidth;
    private final int maxWidth;
    
    public FormatModifier(boolean leftAlign, int minWidth, int maxWidth) {
        this.leftAlign = leftAlign;
        this.minWidth = Math.max(0, minWidth);
        this.maxWidth = Math.max(0, maxWidth);
    }
    
    /**
     * Applies the format modifier to a value.
     * 
     * @param value the value to format (null becomes empty string)
     * @return the formatted value
     */
    public String apply(String value) {
        if (value == null) {
            value = "";
        }
        
        // Apply maximum width truncation first
        if (maxWidth > 0 && value.length() > maxWidth) {
            value = value.substring(0, maxWidth);
        }
        
        // Apply minimum width padding
        if (minWidth > 0 && value.length() < minWidth) {
            if (leftAlign) {
                return String.format("%-" + minWidth + "s", value);
            } else {
                return String.format("%" + minWidth + "s", value);
            }
        }
        
        return value;
    }
    
    /**
     * Returns true if this modifier has no formatting effect.
     * 
     * @return true if no formatting is applied
     */
    public boolean isNone() {
        return minWidth == 0 && maxWidth == 0;
    }
    
    // Getters for testing
    public boolean isLeftAlign() {
        return leftAlign;
    }
    
    public int getMinWidth() {
        return minWidth;
    }
    
    public int getMaxWidth() {
        return maxWidth;
    }
    
    @Override
    public String toString() {
        if (isNone()) {
            return "FormatModifier.NONE";
        }
        
        StringBuilder sb = new StringBuilder("FormatModifier[");
        if (leftAlign) sb.append("left-align, ");
        if (minWidth > 0) sb.append("min=").append(minWidth).append(", ");
        if (maxWidth > 0) sb.append("max=").append(maxWidth).append(", ");
        if (sb.charAt(sb.length() - 2) == ',') {
            sb.setLength(sb.length() - 2); // Remove trailing ", "
        }
        sb.append("]");
        
        return sb.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FormatModifier)) return false;
        
        FormatModifier that = (FormatModifier) o;
        return leftAlign == that.leftAlign &&
               minWidth == that.minWidth &&
               maxWidth == that.maxWidth;
    }
    
    @Override
    public int hashCode() {
        int result = Boolean.hashCode(leftAlign);
        result = 31 * result + minWidth;
        result = 31 * result + maxWidth;
        return result;
    }
}