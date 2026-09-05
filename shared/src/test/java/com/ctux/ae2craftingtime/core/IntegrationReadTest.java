package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class IntegrationReadTest {
    static class Parent { protected String inherited() { return "parent"; } }
    static final class Fixture extends Parent {
        public String visible = "public";
        private String hidden = "private";
        public Object empty = null;
        private String getter() { return "value"; }
        private String argument(double amount) { return Double.toString(amount); }
        private Object nullResult() { return null; }
        private String overloaded(String value) { return value; }
        private String overloaded(Integer value) { return value.toString(); }
        private String ambiguous(CharSequence value) { return value.toString(); }
        private String ambiguous(Object value) { return value.toString(); }
        private Object throwsException() { throw failure; }
        private Object throwsError() { throw error; }
        final IllegalStateException failure = new IllegalStateException("original");
        final AssertionError error = new AssertionError("fatal");
    }

    @Test void fieldsPreserveNullAndPrivateFallback() {
        var fixture = new Fixture();
        assertEquals("public", IntegrationRead.field(fixture, "visible", String.class));
        assertEquals("private", IntegrationRead.field(fixture, "hidden", String.class));
        assertNull(IntegrationRead.field(fixture, "empty", String.class));
        assertNull(IntegrationRead.field(null, "empty", String.class));
        var missing = assertThrows(IntegrationRead.Failure.class, () -> IntegrationRead.field(fixture, "missing", Object.class));
        assertInstanceOf(NoSuchFieldException.class, missing.getCause());
        assertTrue(missing.getMessage().endsWith(".missing"));
        assertInstanceOf(ClassCastException.class, assertThrows(IntegrationRead.Failure.class,
                () -> IntegrationRead.field(fixture, "visible", Number.class)).getCause());
        assertInstanceOf(java.lang.reflect.InaccessibleObjectException.class, assertThrows(IntegrationRead.Failure.class,
                () -> IntegrationRead.field("closed-module", "value", Object.class)).getCause());
    }

    @Test void methodsResolveCompatibleArgumentsAndPreserveOriginalCause() {
        var fixture = new Fixture();
        assertEquals("value", IntegrationRead.invoke(fixture, "getter", String.class));
        assertEquals("parent", IntegrationRead.invoke(fixture, "inherited", String.class));
        assertEquals("2.0", IntegrationRead.invoke(fixture, "argument", String.class, 2.0));
        assertEquals("text", IntegrationRead.invoke(fixture, "overloaded", String.class, "text"));
        assertEquals("2", IntegrationRead.invoke(fixture, "overloaded", String.class, 2));
        assertNull(IntegrationRead.invoke(fixture, "nullResult", Object.class));
        assertNull(IntegrationRead.invoke(null, "getter", Object.class));
        assertThrows(IntegrationRead.Failure.class, () -> IntegrationRead.invoke(fixture, "ambiguous", Object.class, "both"));
        assertThrows(IntegrationRead.Failure.class, () -> IntegrationRead.invoke(fixture, "absent", Object.class));
        assertThrows(IntegrationRead.Failure.class, () -> IntegrationRead.invoke(fixture, "argument", Object.class, "wrong"));
        assertThrows(IntegrationRead.Failure.class, () -> IntegrationRead.invoke(fixture, "argument", Object.class));
        assertInstanceOf(ClassCastException.class, assertThrows(IntegrationRead.Failure.class,
                () -> IntegrationRead.invoke(fixture, "getter", Number.class)).getCause());
        assertSame(fixture.failure, assertThrows(IntegrationRead.Failure.class,
                () -> IntegrationRead.invoke(fixture, "throwsException", Object.class)).getCause());
        assertSame(fixture.error, assertThrows(AssertionError.class,
                () -> IntegrationRead.invoke(fixture, "throwsError", Object.class)));
    }

    @Test void primitiveAndReferenceParameterBoundaries() {
        assertTrue(IntegrationRead.compatible(new Class<?>[] {String.class}, new Object[] {null}));
        assertFalse(IntegrationRead.compatible(new Class<?>[] {int.class}, new Object[] {null}));
        assertFalse(IntegrationRead.compatible(new Class<?>[] {int.class}, new Object[] {}));
        assertFalse(IntegrationRead.compatible(new Class<?>[] {int.class}, new Object[] {"bad"}));
        for (var pair : List.of(new Object[] {boolean.class, true}, new Object[] {byte.class, (byte) 1},
                new Object[] {short.class, (short) 1}, new Object[] {int.class, 1}, new Object[] {long.class, 1L},
                new Object[] {float.class, 1f}, new Object[] {double.class, 1d}, new Object[] {char.class, 'a'})) {
            assertTrue(IntegrationRead.compatible(new Class<?>[] {(Class<?>) pair[0]}, new Object[] {pair[1]}));
        }
    }
    interface DefaultGetter { default String defaultValue() { return "default"; } }
    interface GenericGetter<T> { T value(); }
    static class GenericFixture implements GenericGetter<String>, DefaultGetter {
        public String value() { return "typed"; }
        Runnable lambda() { return () -> value(); }
    }
    @Test void inheritedDefaultsBridgeMethodsAndSyntheticMethods() {
        var fixture = new GenericFixture();
        assertEquals("typed", IntegrationRead.invoke(fixture, "value", String.class));
        assertEquals("default", IntegrationRead.invoke(fixture, "defaultValue", String.class));
        fixture.lambda().run();
        var synthetic = java.util.Arrays.stream(GenericFixture.class.getDeclaredMethods())
                .filter(method -> method.isSynthetic() && !method.isBridge()).findFirst().orElseThrow();
        assertThrows(IntegrationRead.Failure.class,
                () -> IntegrationRead.invoke(fixture, synthetic.getName(), Object.class));
    }
}
