package com.eldritchartifice;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.function.Predicate;

/**
 * Operator-only prototype commands.
 */
final class EldritchCommands {
    private EldritchCommands() {
    }

    static void register(Object event) {
        try {
            Object dispatcher = event.getClass().getMethod("getDispatcher").invoke(event);
            Object root = literal("eldritch");
            requireOperator(root);

            addSubcommand(root, "riftanchor", RiftArena::mark);
            addSubcommand(root, "riftopen", ShoggothService::spawn);
            addSubcommand(root, "riftaudience", player -> {
                if(!RiftAudience.show(player))RuntimeMinecraft.sendMessage(player,"[Eldritch] Too many audiences are active; try again shortly.");
            });
            addSubcommand(root, "riftstatus", ShoggothService::status);
            addSubcommand(root, "riftclear", player -> { ShoggothService.clear(); RuntimeMinecraft.sendMessage(player,"[Eldritch] Rift closed and arena restored."); });
            addSubcommand(root, "shoggoth", ShoggothService::spawn);
            addSubcommand(root, "shoggothjoin", ShoggothService::join);
            addSubcommand(root, "shoggothstatus", ShoggothService::status);
            addSubcommand(root, "shoggothleave", player -> {
                ShoggothService.leave(player);
                RuntimeMinecraft.sendMessage(player, "[Eldritch] Move beyond 32 blocks to leave the rift encounter.");
            });
            addSubcommand(root, "shoggothclear", player -> {
                ShoggothService.clear();
                RuntimeMinecraft.sendMessage(player, "[Eldritch] Shoggoth encounter cleared.");
            });
            addSubcommand(root, "status", player -> {
                WarpState state = WarpService.state(player);
                RuntimeMinecraft.sendMessage(player, "[Eldritch] " + WarpService.statusLine(state));
            });
            addContextSubcommand(root, "devsetup", (player, source) -> {
                DevSetupService.Result result = DevSetupService.run(player, dispatcher, source);
                RuntimeMinecraft.sendMessage(
                        player,
                        result.success()
                                ? "[Eldritch] Dev setup complete: " + result.summary()
                                : "[Eldritch] Dev setup incomplete: " + result.summary()
                                        + " Check latest.log.");
            });
            addSubcommand(root, "attention5", player -> {
                EldritchApi.drawAttention(player, 5, "debug_command");
                RuntimeMinecraft.sendMessage(player, "[Eldritch] Drew 5 Attention.");
            });
            addSubcommand(root, "attention25", player -> {
                EldritchApi.drawAttention(player, 25, "debug_command");
                RuntimeMinecraft.sendMessage(player, "[Eldritch] Drew 25 Attention.");
            });
            addSubcommand(root, "lingering5", player -> {
                EldritchApi.addLingeringWarp(player, 5, "debug_command");
                RuntimeMinecraft.sendMessage(player, "[Eldritch] Added 5 Lingering Warp.");
            });
            addSubcommand(root, "permanent1", player -> {
                EldritchApi.addPermanentWarp(player, 1, "debug_command");
                RuntimeMinecraft.sendMessage(player, "[Eldritch] Added 1 Permanent Warp.");
            });
            addSubcommand(root, "forceevent", WarpService::forceEvent);
            addSubcommand(root, "faction", player ->
                    RuntimeMinecraft.sendMessage(
                            player,
                            "[Eldritch] M&A " + MnaIntegration.status(player)));
            addSubcommand(root, "join", player -> {
                boolean joined = MnaIntegration.join(player);
                RuntimeMinecraft.sendMessage(
                        player,
                        joined
                                ? "[Eldritch] Joined the Eldritch M&A faction."
                                : "[Eldritch] Could not join the Eldritch M&A faction; check latest.log.");
            });
            addSubcommand(root, "leave", player -> {
                boolean left = MnaIntegration.leave(player);
                RuntimeMinecraft.sendMessage(
                        player,
                        left
                                ? "[Eldritch] Cleared M&A faction allegiance."
                                : "[Eldritch] Could not clear M&A faction allegiance; check latest.log.");
            });
            addSubcommand(root, "reserve", player ->
                    RuntimeMinecraft.sendMessage(
                            player,
                            "[Eldritch] Reserve " + MnaIntegration.reserveStatus(player)));
            addSubcommand(root, "overcasttest", player -> {
                boolean tested = MnaIntegration.debugOvercast(player);
                RuntimeMinecraft.sendMessage(
                        player,
                        tested
                                ? "[Eldritch] Forced a 5-mana reserve dip."
                                : "[Eldritch] Overcast test requires the active Eldritch casting resource.");
            });
            addSubcommand(root, "ddstatus", player ->
                    RuntimeMinecraft.sendMessage(
                            player,
                            "[Eldritch] DD " + DimensionalDoorsIntegration.status(player)));
            addSubcommand(root, "watcher", player -> {
                if (DimensionalDoorsIntegration.isLimbo(player)) {
                    RuntimeMinecraft.sendMessage(
                            player,
                            "[Eldritch] Watchers do not manifest inside Limbo.");
                    return;
                }

                boolean spawned = WatcherService.spawnFor(player, true);
                RuntimeMinecraft.sendMessage(
                        player,
                        spawned
                                ? "[Eldritch] A prototype Watcher has manifested."
                                : "[Eldritch] Watcher spawn failed; check latest.log.");
            });
            addSubcommand(root, "watcherstatus", player ->
                    RuntimeMinecraft.sendMessage(
                            player,
                            "[Eldritch] Watcher " + WatcherService.status(player)));
            addSubcommand(root, "watcherfocus", player -> {
                boolean focused = WatcherService.focusFor(player);
                RuntimeMinecraft.sendMessage(
                        player,
                        focused
                                ? "[Eldritch] Watcher attention forced near maximum."
                                : "[Eldritch] No active Watcher to focus.");
            });
            addSubcommand(root, "watcherclear", player -> {
                WatcherService.clearFor(player, "debug_command");
                RuntimeMinecraft.sendMessage(player, "[Eldritch] Prototype Watcher cleared.");
            });
            addSubcommand(root, "displacement", player -> {
                DisplacementService.applyDebug(player);
                RuntimeMinecraft.sendMessage(
                        player,
                        "[Eldritch] Prototype Displacement applied: 1 charge / 15 seconds.");
            });
            addSubcommand(root, "displacement3", player -> {
                DisplacementService.applyDebugStrong(player);
                RuntimeMinecraft.sendMessage(
                        player,
                        "[Eldritch] Prototype Displacement applied: 3 charges / 25 seconds.");
            });
            addSubcommand(root, "displacementstatus", player ->
                    RuntimeMinecraft.sendMessage(
                            player,
                            "[Eldritch] Displacement " + DisplacementService.status(player)));
            addSubcommand(root, "displacementconfig", player ->
                    RuntimeMinecraft.sendMessage(
                            player,
                            "[Eldritch] Displacement config "
                                    + MnaIntegration.displacementConfigStatus()));
            addSubcommand(root, "displacementclear", player -> {
                DisplacementService.clearFor(player, "debug_command");
                RuntimeMinecraft.sendMessage(player, "[Eldritch] Displacement cleared.");
            });
            addSubcommand(root, "timewarp", player -> {
                TimeWarpService.applyDebug(player);
                RuntimeMinecraft.sendMessage(
                        player,
                        "[Eldritch] Prototype Time Warp created: 30 seconds / radius 7 / magnitude 2.");
            });
            addSubcommand(root, "timewarpstatus", player ->
                    RuntimeMinecraft.sendMessage(
                            player,
                            "[Eldritch] Time Warp " + TimeWarpService.status(player)));
            addSubcommand(root, "timewarpclear", player -> {
                TimeWarpService.clearFor(player, "debug_command");
                RuntimeMinecraft.sendMessage(player, "[Eldritch] Time Warp cleared.");
            });
            addContextSubcommand(root, "armorset", (player, source) -> {
                DevSetupService.Result result =
                        DevSetupService.grantArmorSet(player, dispatcher, source);
                RuntimeMinecraft.sendMessage(
                        player,
                        result.success()
                                ? "[Eldritch] Tier-5 armor test set granted."
                                : "[Eldritch] Armor grant incomplete: " + result.summary());
            });
            addSubcommand(root, "armorstatus", player ->
                    RuntimeMinecraft.sendMessage(
                            player,
                            "[Eldritch] Armor " + Tier5ArmorService.status(player)));
            addContextSubcommand(root, "staff", (player, source) -> {
                DevSetupService.Result result =
                        DevSetupService.grantStaff(player, dispatcher, source);
                RuntimeMinecraft.sendMessage(
                        player,
                        result.success()
                                ? "[Eldritch] Staff of the Open Way granted."
                                : "[Eldritch] Staff grant incomplete: " + result.summary());
            });
            addSubcommand(root, "clear", player -> {
                WarpService.clear(player);
                RuntimeMinecraft.sendMessage(
                        player,
                        "[Eldritch] Warp, Pressure, and Exposure cleared.");
            });
            addSubcommand(root, "cleartransient", player -> {
                WarpService.clearTransient(player);
                RuntimeMinecraft.sendMessage(player,
                        "[Eldritch] Transient Warp and Pressure cleared; lingering and permanent marks remain.");
            });

            Method register = dispatcher.getClass().getMethod(
                    "register",
                    Class.forName("com.mojang.brigadier.builder.LiteralArgumentBuilder"));
            register.invoke(dispatcher, root);
            RuntimeLog.info("Registered /eldritch prototype commands.");
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to register /eldritch commands.", exception);
        }
    }

    private static void addSubcommand(Object root, String name, PlayerAction action)
            throws ReflectiveOperationException {
        Object child = literal(name);
        Object command = commandProxy(name, action);

        Method executes = child.getClass().getMethod(
                "executes",
                Class.forName("com.mojang.brigadier.Command"));
        executes.invoke(child, command);

        Method then = root.getClass().getMethod(
                "then",
                Class.forName("com.mojang.brigadier.builder.ArgumentBuilder"));
        then.invoke(root, child);
    }

    private static void addContextSubcommand(
            Object root,
            String name,
            ContextPlayerAction action)
            throws ReflectiveOperationException {
        Object child = literal(name);
        Object command = contextCommandProxy(name, action);

        Method executes = child.getClass().getMethod(
                "executes",
                Class.forName("com.mojang.brigadier.Command"));
        executes.invoke(child, command);

        Method then = root.getClass().getMethod(
                "then",
                Class.forName("com.mojang.brigadier.builder.ArgumentBuilder"));
        then.invoke(root, child);
    }

    private static Object literal(String value) throws ReflectiveOperationException {
        Class<?> commandsClass = Class.forName("net.minecraft.commands.Commands");
        Class<?> literalBuilderClass =
                Class.forName("com.mojang.brigadier.builder.LiteralArgumentBuilder");

        Method fallback = null;
        for (Method method : commandsClass.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers())
                    || method.getParameterCount() != 1
                    || method.getParameterTypes()[0] != String.class
                    || method.getReturnType() != literalBuilderClass) {
                continue;
            }
            if (method.getName().equals("literal")) {
                return method.invoke(null, value);
            }
            fallback = method;
        }

        if (fallback == null) {
            throw new IllegalStateException("Minecraft Commands.literal factory was not found.");
        }
        return fallback.invoke(null, value);
    }

    private static void requireOperator(Object root) throws ReflectiveOperationException {
        Method requires = root.getClass().getMethod("requires", Predicate.class);
        Predicate<Object> predicate = EldritchCommands::hasOperatorPermission;
        requires.invoke(root, predicate);
    }

    private static boolean hasOperatorPermission(Object commandSource) {
        try {
            Method method = namedMethod(
                    commandSource.getClass(),
                    new String[]{"hasPermission", "m_6761_"},
                    boolean.class,
                    int.class);
            return method != null && (Boolean) method.invoke(commandSource, 2);
        } catch (ReflectiveOperationException exception) {
            RuntimeLog.warn("Could not evaluate command permission: " + exception);
            return false;
        }
    }

    private static Object commandProxy(String name, PlayerAction action) throws ClassNotFoundException {
        Class<?> commandClass = Class.forName("com.mojang.brigadier.Command");
        return Proxy.newProxyInstance(
                commandClass.getClassLoader(),
                new Class<?>[]{commandClass},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> "eldritchartifice.command." + name;
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == (args == null || args.length == 0 ? null : args[0]);
                            default -> defaultValue(method.getReturnType());
                        };
                    }
                    if (!method.getName().equals("run") || args == null || args.length != 1) {
                        return defaultValue(method.getReturnType());
                    }
                    Object context = args[0];
                    Object source = context.getClass().getMethod("getSource").invoke(context);
                    RuntimeLog.info("Executing /eldritch " + name + ".");
                    Object player = commandPlayer(source);
                    if (player == null) {
                        return 0;
                    }
                    try {
                        action.run(player);
                        return 1;
                    } catch (Throwable throwable) {
                        RuntimeLog.error(
                                "Command /eldritch " + name + " failed: " + throwable,
                                throwable);
                        RuntimeMinecraft.sendMessage(
                                player,
                                "[Eldritch] Command failed; see latest.log for details.");
                        return 0;
                    }
                });
    }

    private static Object contextCommandProxy(
            String name,
            ContextPlayerAction action)
            throws ClassNotFoundException {
        Class<?> commandClass = Class.forName("com.mojang.brigadier.Command");
        return Proxy.newProxyInstance(
                commandClass.getClassLoader(),
                new Class<?>[]{commandClass},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> "eldritchartifice.command." + name;
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == (args == null || args.length == 0 ? null : args[0]);
                            default -> defaultValue(method.getReturnType());
                        };
                    }
                    if (!method.getName().equals("run") || args == null || args.length != 1) {
                        return defaultValue(method.getReturnType());
                    }

                    Object context = args[0];
                    Object source = context.getClass().getMethod("getSource").invoke(context);
                    RuntimeLog.info("Executing /eldritch " + name + ".");
                    Object player = commandPlayer(source);
                    if (player == null) {
                        return 0;
                    }

                    try {
                        action.run(player, source);
                        return 1;
                    } catch (Throwable throwable) {
                        RuntimeLog.error(
                                "Command /eldritch " + name + " failed: " + throwable,
                                throwable);
                        RuntimeMinecraft.sendMessage(
                                player,
                                "[Eldritch] Command failed; see latest.log for details.");
                        return 0;
                    }
                });
    }

    private static Object commandPlayer(Object source) {
        try {
            Class<?> serverPlayerClass = Class.forName("net.minecraft.server.level.ServerPlayer");
            Method method = namedMethod(
                    source.getClass(),
                    new String[]{"getPlayerOrException", "m_81375_", "getPlayer", "m_230896_"},
                    serverPlayerClass);
            if (method == null) {
                RuntimeLog.warn("Command source did not expose a ServerPlayer accessor.");
                return null;
            }
            return method.invoke(source);
        } catch (ReflectiveOperationException exception) {
            RuntimeLog.error("Unable to resolve command player: " + exception, exception);
            return null;
        }
    }

    private static Method namedMethod(
            Class<?> owner,
            String[] names,
            Class<?> returnType,
            Class<?>... parameterTypes) {
        for (String name : names) {
            try {
                Method method = owner.getMethod(name, parameterTypes);
                if (method.getReturnType() == returnType) {
                    return method;
                }
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        return null;
    }

    @FunctionalInterface
    private interface PlayerAction {
        void run(Object player);
    }

    @FunctionalInterface
    private interface ContextPlayerAction {
        void run(Object player, Object source);
    }
}
