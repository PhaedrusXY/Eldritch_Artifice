package com.eldritchartifice;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Consumer;

/**
 * Runtime adapter for Forge's mod event bus.
 */
final class ModEventBridge {
    private final Object eventBus;
    private final Method typedListenerMethod;
    private final Object normalPriority;

    ModEventBridge() {
        try {
            Class<?> contextClass =
                    Class.forName("net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext");
            Object context = contextClass.getMethod("get").invoke(null);
            this.eventBus = context.getClass().getMethod("getModEventBus").invoke(context);

            Class<?> priorityClass = Class.forName("net.minecraftforge.eventbus.api.EventPriority");
            this.normalPriority = enumConstant(priorityClass, "NORMAL");
            this.typedListenerMethod =
                    findTypedListenerMethod(eventBus.getClass(), priorityClass);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to access the Forge mod event bus.", exception);
        }
    }

    void register(String eventClassName, Consumer<Object> listener) {
        try {
            Class<?> eventClass = Class.forName(eventClassName);
            Class<?>[] parameters = typedListenerMethod.getParameterTypes();

            if (parameters.length == 4) {
                typedListenerMethod.invoke(eventBus, normalPriority, false, eventClass, listener);
            } else if (parameters.length == 3) {
                typedListenerMethod.invoke(eventBus, normalPriority, eventClass, listener);
            } else {
                typedListenerMethod.invoke(eventBus, eventClass, listener);
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Unable to register mod-bus listener for " + eventClassName,
                    exception);
        }
    }

    private static Method findTypedListenerMethod(Class<?> busClass, Class<?> priorityClass) {
        Method threeParameter = null;
        Method twoParameter = null;

        for (Method method : busClass.getMethods()) {
            if (!method.getName().equals("addListener") || !Modifier.isPublic(method.getModifiers())) {
                continue;
            }

            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length == 4
                    && parameters[0] == priorityClass
                    && parameters[1] == boolean.class
                    && parameters[2] == Class.class
                    && Consumer.class.isAssignableFrom(parameters[3])) {
                return method;
            }

            if (parameters.length == 3
                    && parameters[0] == priorityClass
                    && parameters[1] == Class.class
                    && Consumer.class.isAssignableFrom(parameters[2])) {
                threeParameter = method;
            }

            if (parameters.length == 2
                    && parameters[0] == Class.class
                    && Consumer.class.isAssignableFrom(parameters[1])) {
                twoParameter = method;
            }
        }

        if (threeParameter != null) {
            return threeParameter;
        }
        if (twoParameter != null) {
            return twoParameter;
        }
        throw new IllegalStateException("Forge IEventBus typed addListener overload was not found.");
    }

    private static Object enumConstant(Class<?> enumClass, String name) {
        for (Object value : enumClass.getEnumConstants()) {
            if (((Enum<?>) value).name().equals(name)) {
                return value;
            }
        }
        throw new IllegalStateException(
                "Enum constant " + name + " was not found in " + enumClass.getName());
    }
}
