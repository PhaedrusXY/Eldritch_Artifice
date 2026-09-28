package com.eldritchartifice;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Consumer;

/**
 * Minimal runtime adapter around the Forge event bus.
 */
final class ForgeEventBridge {
    private final Object eventBus;
    private final Method typedListenerMethod;
    private final Object normalPriority;

    ForgeEventBridge() {
        try {
            Class<?> minecraftForge = Class.forName("net.minecraftforge.common.MinecraftForge");
            Field eventBusField = minecraftForge.getField("EVENT_BUS");
            this.eventBus = eventBusField.get(null);

            Class<?> eventPriorityClass = Class.forName("net.minecraftforge.eventbus.api.EventPriority");
            this.normalPriority = enumConstant(eventPriorityClass, "NORMAL");
            this.typedListenerMethod = findTypedListenerMethod(eventBus.getClass(), eventPriorityClass);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to access the Forge event bus.", exception);
        }
    }

    void register(String eventClassName, Consumer<Object> listener) {
        registerAtPriority(eventClassName, listener, normalPriority);
    }

    void registerLowest(String eventClassName, Consumer<Object> listener) {
        registerAtPriority(eventClassName, listener, enumConstant(normalPriority.getClass(), "LOWEST"));
    }

    private void registerAtPriority(String eventClassName, Consumer<Object> listener, Object priority) {
        try {
            Class<?> eventClass = Class.forName(eventClassName);
            Class<?>[] parameters = typedListenerMethod.getParameterTypes();

            if (parameters.length == 4) {
                typedListenerMethod.invoke(eventBus, priority, false, eventClass, listener);
            } else if (parameters.length == 3) {
                typedListenerMethod.invoke(eventBus, priority, eventClass, listener);
            } else {
                typedListenerMethod.invoke(eventBus, eventClass, listener);
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to register listener for " + eventClassName, exception);
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
        throw new IllegalStateException("Enum constant " + name + " was not found in " + enumClass.getName());
    }
}
