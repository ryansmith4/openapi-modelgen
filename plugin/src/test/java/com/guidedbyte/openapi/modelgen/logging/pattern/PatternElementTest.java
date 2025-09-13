package com.guidedbyte.openapi.modelgen.logging.pattern;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for pattern element implementations.
 */
class PatternElementTest {

    @Test
    void testLiteralElement() {
        LiteralElement element = new LiteralElement("test literal");
        
        StringBuilder sb = new StringBuilder();
        element.append(sb, "msg", "spec", "template", "component");
        assertEquals("test literal", sb.toString());
        
        assertEquals("test literal".length(), element.estimateMaxLength());
        assertTrue(element.toString().contains("test literal"));
    }

    @Test
    void testLiteralElementWithNull() {
        LiteralElement element = new LiteralElement(null);
        
        StringBuilder sb = new StringBuilder();
        element.append(sb, "msg", "spec", "template", "component");
        assertEquals("", sb.toString());
        
        assertEquals(0, element.estimateMaxLength());
    }

    @Test
    void testMDCElementSpec() {
        MDCElement element = new MDCElement("spec");
        
        StringBuilder sb = new StringBuilder();
        element.append(sb, "msg", "testspec", "template", "component");
        assertEquals("testspec", sb.toString());
        
        assertEquals("spec", element.getKey());
        assertTrue(element.getModifier().isNone());
        assertTrue(element.estimateMaxLength() > 0);
    }

    @Test
    void testMDCElementTemplate() {
        MDCElement element = new MDCElement("template");
        
        StringBuilder sb = new StringBuilder();
        element.append(sb, "msg", "spec", "testtemplate", "component");
        assertEquals("testtemplate", sb.toString());
    }

    @Test
    void testMDCElementComponent() {
        MDCElement element = new MDCElement("component");
        
        StringBuilder sb = new StringBuilder();
        element.append(sb, "msg", "spec", "template", "testcomponent");
        assertEquals("testcomponent", sb.toString());
    }

    @Test
    void testMDCElementUnknownKey() {
        MDCElement element = new MDCElement("unknownkey");
        
        StringBuilder sb = new StringBuilder();
        element.append(sb, "msg", "spec", "template", "component");
        assertEquals("", sb.toString());
    }

    @Test
    void testMDCElementWithNullValues() {
        MDCElement element = new MDCElement("spec");
        
        StringBuilder sb = new StringBuilder();
        element.append(sb, "msg", null, "template", "component");
        assertEquals("", sb.toString());
    }

    @Test
    void testMDCElementWithModifier() {
        FormatModifier modifier = new FormatModifier(true, 10, 0);
        MDCElement element = new MDCElement("spec", modifier);
        
        StringBuilder sb = new StringBuilder();
        element.append(sb, "msg", "test", "template", "component");
        assertEquals("test      ", sb.toString());
        
        assertEquals(modifier, element.getModifier());
        assertTrue(element.estimateMaxLength() >= 10);
    }

    @Test
    void testMessageElement() {
        MessageElement element = new MessageElement();
        
        StringBuilder sb = new StringBuilder();
        element.append(sb, "test message", "spec", "template", "component");
        assertEquals("test message", sb.toString());
        
        assertTrue(element.getModifier().isNone());
        assertTrue(element.estimateMaxLength() > 0);
    }

    @Test
    void testMessageElementWithNull() {
        MessageElement element = new MessageElement();
        
        StringBuilder sb = new StringBuilder();
        element.append(sb, null, "spec", "template", "component");
        assertEquals("", sb.toString());
    }

    @Test
    void testMessageElementWithModifier() {
        FormatModifier modifier = new FormatModifier(false, 20, 0);
        MessageElement element = new MessageElement(modifier);
        
        StringBuilder sb = new StringBuilder();
        element.append(sb, "short", "spec", "template", "component");
        assertEquals("               short", sb.toString());
        
        assertEquals(modifier, element.getModifier());
    }

    @Test
    void testTimestampElement() {
        TimestampElement element = new TimestampElement();
        
        StringBuilder sb = new StringBuilder();
        element.append(sb, "msg", "spec", "template", "component");
        
        // Should produce a timestamp
        assertTrue(sb.length() > 0);
        assertTrue(sb.toString().matches("\\d{2}:\\d{2}:\\d{2}"));
        assertTrue(element.estimateMaxLength() > 0);
    }

    @Test
    void testTimestampElementWithCustomFormat() {
        TimestampElement element = new TimestampElement("yyyy-MM-dd");
        
        StringBuilder sb = new StringBuilder();
        element.append(sb, "msg", "spec", "template", "component");
        
        // Should produce a date
        assertTrue(sb.length() > 0);
        assertTrue(sb.toString().matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    void testTimestampElementWithInvalidFormat() {
        TimestampElement element = new TimestampElement("invalid pattern");
        
        StringBuilder sb = new StringBuilder();
        element.append(sb, "msg", "spec", "template", "component");
        
        // Should fall back to default format
        assertTrue(sb.length() > 0);
        assertTrue(sb.toString().matches("\\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void testTimestampElementWithModifier() {
        FormatModifier modifier = new FormatModifier(true, 15, 0);
        TimestampElement element = new TimestampElement("HH:mm", modifier);
        
        StringBuilder sb = new StringBuilder();
        element.append(sb, "msg", "spec", "template", "component");
        
        // Should be padded to 15 characters
        assertEquals(15, sb.length());
        assertTrue(sb.toString().contains(":"));
    }

    @Test
    void testElementEquality() {
        // Test LiteralElement equality
        LiteralElement lit1 = new LiteralElement("test");
        LiteralElement lit2 = new LiteralElement("test");
        LiteralElement lit3 = new LiteralElement("different");
        
        assertEquals(lit1, lit2);
        assertEquals(lit1.hashCode(), lit2.hashCode());
        assertNotEquals(lit1, lit3);
        
        // Test MDCElement equality
        MDCElement mdc1 = new MDCElement("spec");
        MDCElement mdc2 = new MDCElement("spec");
        MDCElement mdc3 = new MDCElement("template");
        
        assertEquals(mdc1, mdc2);
        assertEquals(mdc1.hashCode(), mdc2.hashCode());
        assertNotEquals(mdc1, mdc3);
        
        // Test with different modifiers
        FormatModifier mod = new FormatModifier(true, 10, 0);
        MDCElement mdc4 = new MDCElement("spec", mod);
        assertNotEquals(mdc1, mdc4);
        
        // Test MessageElement equality
        MessageElement msg1 = new MessageElement();
        MessageElement msg2 = new MessageElement();
        MessageElement msg3 = new MessageElement(mod);
        
        assertEquals(msg1, msg2);
        assertEquals(msg1.hashCode(), msg2.hashCode());
        assertNotEquals(msg1, msg3);
    }

    @Test
    void testElementToString() {
        LiteralElement literal = new LiteralElement("test");
        assertTrue(literal.toString().contains("test"));
        
        MDCElement mdc = new MDCElement("spec");
        assertTrue(mdc.toString().contains("spec"));
        
        MessageElement msg = new MessageElement();
        assertNotNull(msg.toString());
        
        TimestampElement ts = new TimestampElement();
        assertNotNull(ts.toString());
    }
}