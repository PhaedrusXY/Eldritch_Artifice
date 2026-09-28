package com.eldritchartifice;

import java.lang.reflect.Method;

/**
 * Runtime logging without a compile-time SLF4J dependency in the bootstrap build.
 */
final class RuntimeLog {
    private static volatile Object logger;
    private static volatile Class<?> loggerClass;

    private RuntimeLog() {
    }

    static void info(String message) {
        if (!invoke("info", message, null)) {
            System.out.println("[Eldritch Artifice] " + message);
        }
    }

    static void warn(String message) {
        if (!invoke("warn", message, null)) {
            System.err.println("[Eldritch Artifice] WARN: " + message);
        }
    }

    static void error(String message, Throwable throwable) {
        if (!invoke("error", message, throwable)) {
            System.err.println("[Eldritch Artifice] ERROR: " + message);
            if (throwable != null) {
                throwable.printStackTrace(System.err);
            }
        }
    }

    private static boolean invoke(String level, String message, Throwable throwable) {
        try {
            Object target = logger();
            Method method;
            if (throwable == null) {
                method = loggerClass.getMethod(level, String.class);
                method.invoke(target, "[Eldritch Artifice] " + message);
            } else {
                method = loggerClass.getMethod(level, String.class, Throwable.class);
                method.invoke(target, "[Eldritch Artifice] " + message, throwable);
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return false;
        }
    }

    private static Object logger() throws ReflectiveOperationException {
        if (logger != null) {
            return logger;
        }

        Class<?> factoryClass = Class.forName("org.slf4j.LoggerFactory");
        loggerClass = Class.forName("org.slf4j.Logger");
        Method getLogger = factoryClass.getMethod("getLogger", String.class);
        logger = getLogger.invoke(null, "eldritchartifice");
        return logger;
    }
}
