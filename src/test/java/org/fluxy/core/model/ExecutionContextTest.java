package org.fluxy.core.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionContextTest {

    private ExecutionContext context;

    @BeforeEach
    void setUp() {
        context = new ExecutionContext("test-type", "1.0");
    }

    @Test
    void testInitialization() {
        assertEquals("test-type", context.getType());
        assertEquals("1.0", context.getVersion());
        assertNotNull(context.getVariables());
        assertNotNull(context.getReferences());
        assertTrue(context.getVariables().isEmpty());
        assertTrue(context.getReferences().isEmpty());
    }

    @Test
    void testAddAndGetVariableByNameValue() {
        context.addParameter("key1", "value1");

        Optional<String> result = context.getVariable("key1");
        assertTrue(result.isPresent());
        assertEquals("value1", result.get());
    }

    @Test
    void testAddAndGetVariableByObject() {
        Variable variable = new Variable("key2", "value2");
        context.addParameter(variable);

        Optional<String> result = context.getVariable("key2");
        assertTrue(result.isPresent());
        assertEquals("value2", result.get());
    }

    @Test
    void testGetVariableNotFound() {
        Optional<String> result = context.getVariable("nonexistent");
        assertFalse(result.isPresent());
    }

    @Test
    void testAddAndGetReferenceByNameValue() {
        context.addReference("refType", "refValue");

        Optional<String> result = context.getReference("refType");
        assertTrue(result.isPresent());
        assertEquals("refValue", result.get());
    }

    @Test
    void testAddAndGetReferenceByObject() {
        Reference reference = new Reference("refType2", "refValue2");
        context.addReference(reference);

        Optional<String> result = context.getReference("refType2");
        assertTrue(result.isPresent());
        assertEquals("refValue2", result.get());
    }

    @Test
    void testGetReferenceNotFound() {
        Optional<String> result = context.getReference("nonexistent");
        assertFalse(result.isPresent());
    }

    @Test
    void testGetVariableByPath() {
        context.addParameter("username", "john");
        context.addParameter("age", "30");

        Object result = context.getVariableByPath("username");
        assertEquals("john", result);

        Object result2 = context.getVariableByPath("age");
        assertEquals("30", result2);
    }

    @Test
    void testGetVariableByPathNotFound() {
        Object result = context.getVariableByPath("nonexistent");
        assertNull(result);
    }

    @Test
    void testMultipleVariables() {
        context.addParameter("a", "1");
        context.addParameter("b", "2");
        context.addParameter("c", "3");

        assertEquals(3, context.getVariables().size());
        assertEquals("1", context.getVariable("a").orElse(null));
        assertEquals("2", context.getVariable("b").orElse(null));
        assertEquals("3", context.getVariable("c").orElse(null));
    }

    @Test
    void testMultipleReferences() {
        context.addReference("type1", "val1");
        context.addReference("type2", "val2");

        assertEquals(2, context.getReferences().size());
        assertEquals("val1", context.getReference("type1").orElse(null));
        assertEquals("val2", context.getReference("type2").orElse(null));
    }

    @Test
    void testAddDuplicateReferenceTypeThrows() {
        context.addReference("orderId", "ORD-001");

        assertThrows(IllegalArgumentException.class,
                () -> context.addReference("orderId", "ORD-002"));
    }

    @Test
    void testAddDuplicateReferenceTypeByObjectThrows() {
        context.addReference(new Reference("orderId", "ORD-001"));

        assertThrows(IllegalArgumentException.class,
                () -> context.addReference(new Reference("orderId", "ORD-002")));
    }
}

