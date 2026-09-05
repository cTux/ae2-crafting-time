package com.ctux.ae2craftingtime.core;

import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

public final class IntegrationRead {
    public static final class Failure extends RuntimeException {
        public Failure(String member, Throwable cause) { super(member, cause); }
    }
    private static final Map<Class<?>, Class<?>> BOXED = Map.of(boolean.class, Boolean.class, byte.class, Byte.class,
            short.class, Short.class, int.class, Integer.class, long.class, Long.class, float.class, Float.class,
            double.class, Double.class, char.class, Character.class);

    public static <T> T field(Object receiver, String name, Class<T> resultType) {
        if (receiver == null) return null;
        return field(receiver, receiver.getClass(), name, resultType);
    }

    public static <T> T field(Object receiver, Class<?> owner, String name, Class<T> resultType) {
        try {
            java.lang.reflect.Field field;
            try {
                field = owner.getField(name);
            } catch (NoSuchFieldException missingPublic) {
                field = owner.getDeclaredField(name);
            }
            field.setAccessible(true);
            return resultType.cast(field.get(receiver));
        } catch (ReflectiveOperationException | SecurityException | InaccessibleObjectException
                | IllegalArgumentException | ClassCastException failure) {
            throw new Failure(owner.getName() + "." + name, failure);
        }
    }

    public static <T> T invoke(Object receiver, String name, Class<T> resultType, Object... arguments) {
        if (receiver == null) return null;
        var member = receiver.getClass().getName() + "." + name;
        try {
            Method selected = null;
            var signatures = new HashSet<String>();
            var methods = new ArrayList<>(Arrays.asList(receiver.getClass().getMethods()));
            for (var type = receiver.getClass(); type != null; type = type.getSuperclass()) {
                methods.addAll(Arrays.asList(type.getDeclaredMethods()));
            }
            for (var method : methods) {
                if (!method.getName().equals(name) || method.isBridge() || method.isSynthetic()) continue;
                if (!signatures.add(Arrays.toString(method.getParameterTypes()))) continue;
                if (!compatible(method.getParameterTypes(), arguments)) continue;
                if (selected != null) throw new IllegalArgumentException("Ambiguous compatible overload: " + member);
                selected = method;
            }
            if (selected == null) throw new NoSuchMethodException(member);
            selected.setAccessible(true);
            return resultType.cast(selected.invoke(receiver, arguments));
        } catch (InvocationTargetException failure) {
            var cause = failure.getCause();
            if (cause instanceof Error error) throw error;
            throw new Failure(member, cause);
        } catch (ReflectiveOperationException | SecurityException | InaccessibleObjectException
                | IllegalArgumentException | ClassCastException failure) {
            throw new Failure(member, failure);
        }
    }

    static boolean compatible(Class<?>[] parameters, Object[] arguments) {
        if (parameters.length != arguments.length) return false;
        for (int i = 0; i < parameters.length; i++) {
            var type = parameters[i];
            if (arguments[i] == null) {
                if (type.isPrimitive()) return false;
            } else if (!BOXED.getOrDefault(type, type).isInstance(arguments[i])) return false;
        }
        return true;
    }
    private IntegrationRead() {}
}
