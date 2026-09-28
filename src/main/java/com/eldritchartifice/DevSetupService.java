package com.eldritchartifice;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * One-command development bootstrap for disposable test characters.
 */
final class DevSetupService {
    private static final String[] BEFORE_FACTION_COMMANDS = {
        "mna progression tier 5",
        "mna progression level 75",
        "mna progression complete"
    };

    private static final String[] AFTER_FACTION_COMMANDS = {
        "mna rote all",
        "mna mastery all",
        "give @s eldritchartifice:staff_of_the_open_way 1"
    };

    private static final String[] ARMOR_COMMANDS = {
        "give @s eldritchartifice:eldritch_helmet 1",
        "give @s eldritchartifice:eldritch_chestplate 1",
        "give @s eldritchartifice:eldritch_leggings 1",
        "give @s eldritchartifice:eldritch_boots 1"
    };

    private static final String[] STAFF_COMMANDS = {
        "give @s eldritchartifice:staff_of_the_open_way 1"
    };

    private DevSetupService() {
    }

    static Result run(Object player, Object dispatcher, Object commandSource) {
        if (!RuntimeMinecraft.isServerPlayer(player)) {
            return new Result(false, List.of("command source is not a ServerPlayer"));
        }
        if (dispatcher == null || commandSource == null) {
            return new Result(false, List.of("live command dispatcher/source unavailable"));
        }

        List<String> failures = new ArrayList<>();
        try {
            Method execute = dispatcher.getClass().getMethod(
                    "execute",
                    String.class,
                    Object.class);

            runCommands(dispatcher, execute, commandSource, BEFORE_FACTION_COMMANDS, failures);

            if (!MnaIntegration.join(player)) {
                failures.add("eldritch faction join");
            }

            runCommands(dispatcher, execute, commandSource, AFTER_FACTION_COMMANDS, failures);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            RuntimeLog.error("Development setup failed.", exception);
            failures.add(exception.getClass().getSimpleName() + ": " + safeMessage(exception));
        }

        boolean success = failures.isEmpty();
        RuntimeLog.info(
                "Development setup for " + RuntimeMinecraft.playerDebugName(player)
                        + " success=" + success
                        + (success ? "" : ", failures=" + failures));
        return new Result(success, List.copyOf(failures));
    }

    static Result grantArmorSet(Object player, Object dispatcher, Object commandSource) {
        if (!RuntimeMinecraft.isServerPlayer(player)) {
            return new Result(false, List.of("command source is not a ServerPlayer"));
        }
        if (dispatcher == null || commandSource == null) {
            return new Result(false, List.of("live command dispatcher/source unavailable"));
        }

        List<String> failures = new ArrayList<>();
        try {
            Method execute = dispatcher.getClass().getMethod(
                    "execute",
                    String.class,
                    Object.class);
            runCommands(dispatcher, execute, commandSource, ARMOR_COMMANDS, failures);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            RuntimeLog.error("Tier-5 armor debug grant failed.", exception);
            failures.add(exception.getClass().getSimpleName() + ": " + safeMessage(exception));
        }

        return new Result(failures.isEmpty(), List.copyOf(failures));
    }

    static Result grantStaff(Object player, Object dispatcher, Object commandSource) {
        if (!RuntimeMinecraft.isServerPlayer(player)) {
            return new Result(false, List.of("command source is not a ServerPlayer"));
        }
        if (dispatcher == null || commandSource == null) {
            return new Result(false, List.of("live command dispatcher/source unavailable"));
        }

        List<String> failures = new ArrayList<>();
        try {
            Method execute = dispatcher.getClass().getMethod(
                    "execute",
                    String.class,
                    Object.class);
            runCommands(dispatcher, execute, commandSource, STAFF_COMMANDS, failures);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            RuntimeLog.error("Staff of the Open Way debug grant failed.", exception);
            failures.add(exception.getClass().getSimpleName() + ": " + safeMessage(exception));
        }

        return new Result(failures.isEmpty(), List.copyOf(failures));
    }

    private static void runCommands(
            Object dispatcher,
            Method execute,
            Object source,
            String[] commandLines,
            List<String> failures)
            throws ReflectiveOperationException {
        for (String commandLine : commandLines) {
            try {
                Object result = execute.invoke(dispatcher, commandLine, source);
                if (result instanceof Integer integer && integer <= 0) {
                    failures.add(commandLine + " returned " + integer);
                }
            } catch (InvocationTargetException exception) {
                Throwable cause = exception.getCause();
                RuntimeLog.error(
                        "Development command failed: /" + commandLine,
                        cause == null ? exception : cause);
                failures.add(commandLine);
            }
        }
    }

    private static String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null ? "<no message>" : message;
    }

    record Result(boolean success, List<String> failures) {
        String summary() {
            if (success) {
                return "Tier 5, magic level 75, progression complete, Eldritch faction, "
                        + "all rote/mastery, and Staff of the Open Way applied.";
            }
            return "setup incomplete: " + String.join("; ", failures);
        }
    }
}
