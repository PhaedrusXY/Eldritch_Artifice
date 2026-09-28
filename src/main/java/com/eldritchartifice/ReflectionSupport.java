package com.eldritchartifice;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * Signature-based reflection helpers that avoid relying on remapped vanilla method names.
 */
final class ReflectionSupport {
    private record Key(Class<?> owner, String name, Class<?> result, java.util.List<Class<?>> parameters, boolean statik) {}
    private static final java.util.concurrent.ConcurrentHashMap<Key,Method> METHODS = new java.util.concurrent.ConcurrentHashMap<>();
    private ReflectionSupport() {
    }

    static Method findMethod(
            Class<?> owner,
            Class<?> returnType,
            Class<?>[] parameterTypes,
            boolean requireStatic) {
        Key key = new Key(owner, "", returnType, java.util.List.copyOf(Arrays.asList(parameterTypes)), requireStatic);
        return METHODS.computeIfAbsent(key, ignored -> resolveMethod(owner, returnType, parameterTypes, requireStatic));
    }
    private static Method resolveMethod(Class<?> owner, Class<?> returnType, Class<?>[] parameterTypes, boolean requireStatic) {
        Method fallback = null;
        String preferred = preferredName(returnType, parameterTypes);

        for (Method method : InspectableType.methods(owner)) {
            if (method.getReturnType() != returnType
                    || Modifier.isStatic(method.getModifiers()) != requireStatic
                    || !Arrays.equals(method.getParameterTypes(), parameterTypes)
                    || !Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            if (!preferred.isEmpty() && method.getName().equals(preferred)) {
                return method;
            }
            if (fallback == null) {
                fallback = method;
            }
        }

        if (fallback != null) {
            return fallback;
        }

        throw new IllegalStateException(
                "No matching method on " + owner.getName()
                        + " -> " + returnType.getName()
                        + Arrays.toString(parameterTypes));
    }

    static Method findNamedOrSignature(
            Class<?> owner,
            String preferredName,
            Class<?> returnType,
            Class<?>... parameterTypes) {
        Key key = new Key(owner, preferredName, returnType, java.util.List.copyOf(Arrays.asList(parameterTypes)), false);
        return METHODS.computeIfAbsent(key, ignored -> resolveNamed(owner, preferredName, returnType, parameterTypes));
    }
    private static Method resolveNamed(Class<?> owner, String preferredName, Class<?> returnType, Class<?>[] parameterTypes) {
        try {
            Method named = InspectableType.of(owner).getMethod(preferredName, parameterTypes);
            if (named.getReturnType() == returnType) {
                return named;
            }
        } catch (NoSuchMethodException ignored) {
        }
        return resolveMethod(owner, returnType, parameterTypes, false);
    }

    static Object invoke(
            Object target,
            String preferredName,
            Class<?> returnType,
            Class<?>[] parameterTypes,
            Object... args) {
        Method method = findNamedOrSignature(target.getClass(), preferredName, returnType, parameterTypes);
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Invocation failed: " + method, cause);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Invocation failed: " + method, exception);
        }
    }

    static Object invokeNoArgsByReturnType(Object target, String preferredName, Class<?> returnType) {
        return invoke(target, preferredName, returnType, new Class<?>[0]);
    }

    private static String preferredName(Class<?> returnType, Class<?>[] parameters) {
        if (returnType == int.class && Arrays.equals(parameters, new Class<?>[]{String.class})) {
            return "getInt";
        }
        if (returnType == long.class && Arrays.equals(parameters, new Class<?>[]{String.class})) {
            return "getLong";
        }
        if (returnType == String.class && Arrays.equals(parameters, new Class<?>[]{String.class})) {
            return "getString";
        }
        if (returnType == void.class && Arrays.equals(parameters, new Class<?>[]{String.class, int.class})) {
            return "putInt";
        }
        if (returnType == void.class && Arrays.equals(parameters, new Class<?>[]{String.class, long.class})) {
            return "putLong";
        }
        if (returnType == void.class && Arrays.equals(parameters, new Class<?>[]{String.class, String.class})) {
            return "putString";
        }
        return "";
    }
}
