package org.fluxy.core.model;

import org.fluxy.core.support.SimpleExecutionContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConditionAndOperatorTest {

    // ===== StandardOperator.EQ =====

    @Test
    void testEQ_matchingStrings() {
        assertTrue(StandardOperator.EQ.matches("hello", "hello"));
    }

    @Test
    void testEQ_nonMatchingStrings() {
        assertFalse(StandardOperator.EQ.matches("hello", "world"));
    }

    @Test
    void testEQ_bothNull() {
        assertTrue(StandardOperator.EQ.matches(null, null));
    }

    @Test
    void testEQ_expectedNullActualNotNull() {
        assertTrue(StandardOperator.EQ.matches(null, null));
        assertFalse(StandardOperator.EQ.matches(null, "value"));
    }

    @Test
    void testEQ_expectedNotNullActualNull() {
        assertFalse(StandardOperator.EQ.matches("value", null));
    }

    // ===== StandardOperator.NEQ =====

    @Test
    void testNEQ_differentStrings() {
        assertTrue(StandardOperator.NEQ.matches("hello", "world"));
    }

    @Test
    void testNEQ_sameStrings() {
        assertFalse(StandardOperator.NEQ.matches("hello", "hello"));
    }

    @Test
    void testNEQ_nulls() {
        assertFalse(StandardOperator.NEQ.matches(null, null));
    }

    // ===== StandardOperator.GT =====

    @Test
    void testGT_actualGreaterThanExpected() {
        // actual (10) > expected (5)
        assertTrue(StandardOperator.GT.matches(5, 10));
    }

    @Test
    void testGT_actualLessThanExpected() {
        assertFalse(StandardOperator.GT.matches(10, 5));
    }

    @Test
    void testGT_equal() {
        assertFalse(StandardOperator.GT.matches(5, 5));
    }

    @Test
    void testGT_withStrings() {
        // "banana" > "apple" lexicographically
        assertTrue(StandardOperator.GT.matches("apple", "banana"));
        assertFalse(StandardOperator.GT.matches("banana", "apple"));
    }

    @Test
    void testGT_nonComparable() {
        assertFalse(StandardOperator.GT.matches(new Object(), new Object()));
    }

    // ===== StandardOperator.LT =====

    @Test
    void testLT_actualLessThanExpected() {
        // actual (3) < expected (7)
        assertTrue(StandardOperator.LT.matches(7, 3));
    }

    @Test
    void testLT_actualGreaterThanExpected() {
        assertFalse(StandardOperator.LT.matches(3, 7));
    }

    @Test
    void testLT_equal() {
        assertFalse(StandardOperator.LT.matches(5, 5));
    }

    @Test
    void testLT_withStrings() {
        assertTrue(StandardOperator.LT.matches("banana", "apple"));
        assertFalse(StandardOperator.LT.matches("apple", "banana"));
    }

    // ===== StandardOperator.CONTAINS =====

    @Test
    void testCONTAINS_substringPresent() {
        // actual "hello world" contains expected "world"
        assertTrue(StandardOperator.CONTAINS.matches("world", "hello world"));
    }

    @Test
    void testCONTAINS_substringAbsent() {
        assertFalse(StandardOperator.CONTAINS.matches("xyz", "hello world"));
    }

    @Test
    void testCONTAINS_nonStringTypes() {
        assertFalse(StandardOperator.CONTAINS.matches(42, 420));
    }

    @Test
    void testCONTAINS_emptyString() {
        assertTrue(StandardOperator.CONTAINS.matches("", "anything"));
    }

    // ===== Condition.evaluate =====

    @Test
    void testConditionEvaluate_EQ_true() {
        ExecutionContext context = new SimpleExecutionContext("test", "1.0");
        context.addParameter("status", "active");

        Condition condition = new Condition(StandardOperator.EQ, "active", "status");
        assertTrue(condition.evaluate(context));
    }

    @Test
    void testConditionEvaluate_EQ_false() {
        ExecutionContext context = new SimpleExecutionContext("test", "1.0");
        context.addParameter("status", "inactive");

        Condition condition = new Condition(StandardOperator.EQ, "active", "status");
        assertFalse(condition.evaluate(context));
    }

    @Test
    void testConditionEvaluate_NEQ_true() {
        ExecutionContext context = new SimpleExecutionContext("test", "1.0");
        context.addParameter("status", "inactive");

        Condition condition = new Condition(StandardOperator.NEQ, "active", "status");
        assertTrue(condition.evaluate(context));
    }

    @Test
    void testConditionEvaluate_CONTAINS_true() {
        ExecutionContext context = new SimpleExecutionContext("test", "1.0");
        context.addParameter("message", "Error: something went wrong");

        Condition condition = new Condition(StandardOperator.CONTAINS, "Error", "message");
        assertTrue(condition.evaluate(context));
    }

    @Test
    void testConditionEvaluate_CONTAINS_false() {
        ExecutionContext context = new SimpleExecutionContext("test", "1.0");
        context.addParameter("message", "All good");

        Condition condition = new Condition(StandardOperator.CONTAINS, "Error", "message");
        assertFalse(condition.evaluate(context));
    }

    @Test
    void testConditionEvaluate_variableNotFound() {
        ExecutionContext context = new SimpleExecutionContext("test", "1.0");

        Condition condition = new Condition(StandardOperator.EQ, "value", "nonexistent");
        // variableByPath returns null, EQ compares "value".equals(null) → false
        assertFalse(condition.evaluate(context));
    }

    @Test
    void testConditionEvaluate_variableNotFound_EQNull() {
        ExecutionContext context = new SimpleExecutionContext("test", "1.0");

        Condition condition = new Condition(StandardOperator.EQ, null, "nonexistent");
        // variableByPath returns null, EQ compares null == null → true
        assertTrue(condition.evaluate(context));
    }
}

