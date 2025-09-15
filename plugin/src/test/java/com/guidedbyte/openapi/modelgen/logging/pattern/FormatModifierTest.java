package com.guidedbyte.openapi.modelgen.logging.pattern;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for FormatModifier class.
 */
class FormatModifierTest {

    @Test
    void testNoModifier() {
        FormatModifier modifier = FormatModifier.NONE;
        
        assertTrue(modifier.isNone());
        assertEquals("test", modifier.apply("test"));
        assertEquals("", modifier.apply(null));
        assertEquals("", modifier.apply(""));
    }

    @Test
    void testLeftAlign() {
        FormatModifier modifier = new FormatModifier(true, 10, 0);
        
        assertFalse(modifier.isNone());
        assertTrue(modifier.isLeftAlign());
        assertEquals(10, modifier.getMinWidth());
        assertEquals(0, modifier.getMaxWidth());
        
        assertEquals("test      ", modifier.apply("test"));
        assertEquals("          ", modifier.apply(""));
        assertEquals("verylongtext", modifier.apply("verylongtext"));
    }

    @Test
    void testRightAlign() {
        FormatModifier modifier = new FormatModifier(false, 10, 0);
        
        assertFalse(modifier.isLeftAlign());
        assertEquals("      test", modifier.apply("test"));
        assertEquals("          ", modifier.apply(""));
        assertEquals("verylongtext", modifier.apply("verylongtext"));
    }

    @Test
    void testMaxWidth() {
        FormatModifier modifier = new FormatModifier(false, 0, 5);
        
        assertEquals(0, modifier.getMinWidth());
        assertEquals(5, modifier.getMaxWidth());
        
        assertEquals("test", modifier.apply("test"));
        assertEquals("veryl", modifier.apply("verylongtext"));
        assertEquals("", modifier.apply(""));
    }

    @Test
    void testMinAndMaxWidth() {
        FormatModifier modifier = new FormatModifier(true, 10, 15);
        
        // Short text - padded to min width
        assertEquals("test      ", modifier.apply("test"));
        
        // Medium text - no change
        assertEquals("mediumtext", modifier.apply("mediumtext"));
        
        // Long text - truncated to max width, then padded if needed
        assertEquals("verylongtexttha", modifier.apply("verylongtextthatiswaytoolong"));
        
        // Text exactly at min width
        assertEquals("exactlyten", modifier.apply("exactlyten"));
        
        // Text exactly at max width  
        assertEquals("exactlyfifteeen", modifier.apply("exactlyfifteeen"));
    }

    @Test
    void testNegativeWidths() {
        FormatModifier modifier = new FormatModifier(true, -5, -10);
        
        // Negative widths should be treated as 0
        assertEquals(0, modifier.getMinWidth());
        assertEquals(0, modifier.getMaxWidth());
        assertTrue(modifier.isNone());
        assertEquals("test", modifier.apply("test"));
    }

    @Test
    void testEquals() {
        FormatModifier modifier1 = new FormatModifier(true, 10, 20);
        FormatModifier modifier2 = new FormatModifier(true, 10, 20);
        FormatModifier modifier3 = new FormatModifier(false, 10, 20);
        FormatModifier modifier4 = new FormatModifier(true, 15, 20);
        
        assertEquals(modifier1, modifier2);
        assertEquals(modifier1.hashCode(), modifier2.hashCode());
        assertNotEquals(modifier1, modifier3);
        assertNotEquals(modifier1, modifier4);
        assertNotEquals(null, modifier1);
        assertNotEquals("string", modifier1);
    }

    @Test
    void testToString() {
        FormatModifier none = FormatModifier.NONE;
        FormatModifier leftAlign = new FormatModifier(true, 10, 0);
        FormatModifier rightAlign = new FormatModifier(false, 10, 0);
        FormatModifier maxWidth = new FormatModifier(false, 0, 20);
        FormatModifier both = new FormatModifier(true, 10, 20);
        
        assertEquals("FormatModifier.NONE", none.toString());
        assertTrue(leftAlign.toString().contains("left-align"));
        assertTrue(leftAlign.toString().contains("min=10"));
        assertTrue(rightAlign.toString().contains("min=10"));
        assertFalse(rightAlign.toString().contains("left-align"));
        assertTrue(maxWidth.toString().contains("max=20"));
        assertTrue(both.toString().contains("left-align"));
        assertTrue(both.toString().contains("min=10"));
        assertTrue(both.toString().contains("max=20"));
    }

    @Test
    void testEdgeCases() {
        FormatModifier modifier = new FormatModifier(true, 5, 3); // min > max
        
        // Should still work - max width applied first, then min width padding
        assertEquals("lon  ", modifier.apply("long")); // Truncated to 3 ("lon"), then padded to 5
        assertEquals("ab   ", modifier.apply("ab"));   // Not truncated (length < 3), padded to 5
    }
}