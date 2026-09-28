package com.eldritchartifice;

import net.minecraftforge.fml.common.Mod;

/**
 * Bootstrap for the Eldritch Artifice prototype.
 */
@Mod(EldritchArtifice.MOD_ID)
public final class EldritchArtifice {
    public static final String MOD_ID = "eldritchartifice";
    public static final String VERSION = "0.11.15-prototype";

    public EldritchArtifice() {
        RuntimeLog.info("Bootstrapping " + VERSION);
        MnaSpellConfigRaceGuard.install();

        ModEventBridge modEventBridge = new ModEventBridge();
        modEventBridge.register("net.minecraftforge.event.BuildCreativeModeTabContentsEvent",
                event -> safeHandle("creative item listing", () -> EldritchCreativeItems.buildContents(event)));
        modEventBridge.register(
                "net.minecraftforge.registries.RegisterEvent",
                event -> {
                    safeHandle("Wayfarer door registration", () -> WayfarerRegistry.register(event));
                    safeHandle(
                            "M&A faction/spell registration",
                            () -> MnaIntegration.onRegisterEvent(event));
                    safeHandle(
                            "Tier-5 Eldritch armor registration",
                            () -> EldritchArmorRegistry.onRegisterEvent(event));
                    safeHandle(
                            "Staff of the Open Way registration",
                            () -> EldritchStaffRegistry.onRegisterEvent(event));
                    safeHandle(
                            "Eldritch grimoire and artifact registration",
                            () -> EldritchRelicRegistry.onRegisterEvent(event));
                });
        modEventBridge.register(
                "net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent",
                event -> safeHandle(
                        "M&A casting resource registration",
                        MnaIntegration::onLoadComplete));

        ForgeEventBridge eventBridge = new ForgeEventBridge();
        eventBridge.register("net.minecraftforge.event.server.ServerStartedEvent", event -> safeHandle("wayfarer load", () -> WayfarerDoors.start(event)));
        eventBridge.register("net.minecraftforge.event.server.ServerStoppedEvent", event -> WayfarerDoors.stop());
        eventBridge.register("net.minecraftforge.event.TickEvent$ServerTickEvent", event -> WayfarerDoors.tick(event));
        eventBridge.registerLowest("net.minecraftforge.event.entity.player.PlayerInteractEvent$RightClickBlock", event -> safeHandle("wayfarer packing", () -> WayfarerDoors.interact(event)));
        eventBridge.registerLowest("net.minecraftforge.event.level.BlockEvent$BreakEvent", event -> safeHandle("wayfarer pickup", () -> WayfarerDoors.breakDoor(event)));
        eventBridge.register("net.minecraftforge.event.entity.player.PlayerEvent$PlayerLoggedOutEvent", event -> WayfarerDoors.logout(event));

        eventBridge.register("net.minecraftforge.event.server.ServerStartedEvent",
                event -> safeHandle("rift arena recovery", () -> RiftArena.started(event)));
        eventBridge.register("net.minecraftforge.event.level.BlockEvent$BreakEvent",
                event -> safeHandle("rift block protection", () -> RiftArena.protect(event)));
        eventBridge.register("net.minecraftforge.event.level.BlockEvent$EntityPlaceEvent",
                event -> safeHandle("rift placement protection", () -> RiftArena.protect(event)));
        eventBridge.register("net.minecraftforge.event.level.ExplosionEvent$Detonate",
                event -> safeHandle("rift explosion protection", () -> RiftArena.explosion(event)));
        eventBridge.register(
                "net.minecraftforge.event.TickEvent$PlayerTickEvent",
                event -> safeHandle(
                        "player tick",
                        () -> {
                            safeHandle("wayfarer passage", () -> WayfarerDoors.playerTick(event));
                            WarpService.onPlayerTick(event);
                            Tier5ArmorService.onPlayerTick(event);
                        }));
        eventBridge.register(
                "net.minecraftforge.event.entity.player.PlayerEvent$PlayerLoggedInEvent",
                event -> safeHandle("login", () -> {
                    WarpService.onLogin(event);
                    MnaIntegration.onPlayerLogin(RuntimeMinecraft.eventPlayer(event));
                }));
        eventBridge.register(
                "net.minecraftforge.event.entity.player.PlayerEvent$PlayerLoggedOutEvent",
                event -> safeHandle("logout", () -> {
                    Object player = RuntimeMinecraft.eventPlayer(event);
                    WatcherService.clearFor(player, "owner_logout");
                    DisplacementService.clearFor(player, "owner_logout");
                    TimeWarpService.clearFor(player, "owner_logout");
                    Tier5ArmorService.clearFor(player, "owner_logout");
                    WarpService.onLogout(event);
                }));
        eventBridge.register(
                "net.minecraftforge.event.entity.player.PlayerEvent$PlayerRespawnEvent",
                event -> safeHandle("respawn", () -> {
                    Object player = RuntimeMinecraft.eventPlayer(event);
                    WatcherService.clearFor(player, "owner_respawn");
                    DisplacementService.clearFor(player, "owner_respawn");
                    TimeWarpService.clearFor(player, "owner_respawn");
                    Tier5ArmorService.clearFor(player, "owner_respawn");
                    WarpService.onRespawn(event);
                }));
        eventBridge.register(
                "net.minecraftforge.event.entity.player.PlayerEvent$PlayerChangedDimensionEvent",
                event -> safeHandle("dimension change", () -> {
                    Object player = RuntimeMinecraft.eventPlayer(event);
                    WatcherService.clearFor(player, "owner_dimension_change");
                    DisplacementService.clearFor(player, "owner_dimension_change");
                    TimeWarpService.clearFor(player, "owner_dimension_change");
                    WarpService.onDimensionChanged(event);
                }));
        eventBridge.register(
                "net.minecraftforge.event.entity.player.PlayerEvent$Clone",
                event -> safeHandle("player clone", () -> {
                    Object original = RuntimeMinecraft.cloneOriginal(event);
                    WatcherService.clearFor(original, "owner_clone");
                    DisplacementService.clearFor(original, "owner_clone");
                    TimeWarpService.clearFor(original, "owner_clone");
                    Tier5ArmorService.clearFor(original, "owner_clone");
                    WarpService.onClone(event);
                }));
        eventBridge.register(
                "net.minecraftforge.event.RegisterCommandsEvent",
                event -> safeHandle("command registration", () -> EldritchCommands.register(event)));

        eventBridge.register(
                "net.minecraftforge.event.entity.player.PlayerInteractEvent$RightClickBlock",
                event -> safeHandle(
                        "Teleport marker dimension capture",
                        () -> TeleportMarkerService.onRightClickBlock(event)));

        eventBridge.register(
                "net.minecraftforge.event.entity.living.LivingAttackEvent",
                event -> safeHandle(
                        "Watcher attack isolation",
                        () -> WatcherService.onLivingAttack(event)));
        eventBridge.register(
                "net.minecraftforge.event.entity.living.LivingAttackEvent",
                event -> safeHandle(
                        "Displacement damage interception",
                        () -> DisplacementService.onLivingAttack(event)));
        eventBridge.register(
                "net.minecraftforge.event.entity.living.LivingAttackEvent",
                event -> safeHandle(
                        "Tier-5 armor damage interception",
                        () -> Tier5ArmorService.onLivingAttack(event)));
        eventBridge.register(
                "net.minecraftforge.event.entity.living.LivingDeathEvent",
                event -> safeHandle(
                        "manifestation/displacement death cleanup",
                        () -> {
                            WatcherService.onLivingDeath(event);
                            DisplacementService.onLivingDeath(event);
                            TimeWarpService.onLivingDeath(event);
                            Tier5ArmorService.clearFor(
                                    RuntimeMinecraft.eventPlayer(event),
                                    "owner_death");
                        }));
        eventBridge.register(
                "net.minecraftforge.event.entity.living.LivingDropsEvent",
                event -> safeHandle(
                        "Watcher drops isolation",
                        () -> WatcherService.onLivingDrops(event)));
        eventBridge.register(
                "net.minecraftforge.event.entity.living.LivingExperienceDropEvent",
                event -> safeHandle(
                        "Watcher experience isolation",
                        () -> WatcherService.onLivingExperienceDrop(event)));
        eventBridge.register(
                "net.minecraftforge.event.entity.EntityJoinLevelEvent",
                event -> safeHandle(
                        "Watcher stale-entity cleanup",
                        () -> WatcherService.onEntityJoinLevel(event)));
        eventBridge.register(
                "net.minecraftforge.event.entity.EntityMobGriefingEvent",
                event -> safeHandle(
                        "Watcher mob-griefing isolation",
                        () -> WatcherService.onMobGriefing(event)));
        eventBridge.register(
                "net.minecraftforge.event.entity.living.LivingEvent$LivingTickEvent",
                event -> safeHandle(
                        "Watcher/displacement living tick",
                        () -> {
                            WatcherService.onLivingTick(event);
                            DisplacementService.onLivingTick(event);
                        }));
        eventBridge.register(
                "net.minecraftforge.event.server.ServerStoppingEvent",
                event -> safeHandle(
                        "prototype server-stop cleanup",
                        () -> {
                            WatcherService.clearAll("server_stopping");
                            DisplacementService.clearAll("server_stopping");
                            TimeWarpService.clearAll("server_stopping");
                            CagedSingularityService.clear();
                            Tier5ArmorService.clearAll("server_stopping");
                        }));

        eventBridge.register("net.minecraftforge.event.TickEvent$ServerTickEvent",
                event -> safeHandle("shoggoth tick", () -> ShoggothService.tick(event)));
        eventBridge.register("net.minecraftforge.event.TickEvent$ServerTickEvent",
                event -> safeHandle("singularity tick", () -> CagedSingularityService.tick(event)));
        eventBridge.register("net.minecraftforge.event.entity.living.LivingAttackEvent",
                event -> safeHandle("shoggoth parallax", () -> ShoggothService.attack(event)));
        eventBridge.register("net.minecraftforge.event.entity.EntityJoinLevelEvent",
                event -> safeHandle("shoggoth entity tracking", () -> ShoggothService.entityJoin(event)));
        eventBridge.register("net.minecraftforge.event.entity.EntityLeaveLevelEvent",
                event -> safeHandle("shoggoth unload", () -> ShoggothService.entityLeave(event)));
        eventBridge.register("net.minecraftforge.event.entity.living.LivingDropsEvent",
                event -> safeHandle("shoggoth drops", () -> ShoggothService.drops(event)));
        eventBridge.register("net.minecraftforge.event.entity.living.LivingExperienceDropEvent",
                event -> safeHandle("shoggoth XP", () -> ShoggothService.experience(event)));
        eventBridge.register("net.minecraftforge.event.entity.EntityMobGriefingEvent",
                event -> safeHandle("shoggoth terrain protection", () -> ShoggothService.grief(event)));
        eventBridge.register("net.minecraftforge.event.entity.player.PlayerEvent$PlayerLoggedOutEvent",
                event -> safeHandle("shoggoth logout", () -> ShoggothService.leave(RuntimeMinecraft.eventPlayer(event))));
        eventBridge.register("net.minecraftforge.event.server.ServerStoppingEvent",
                event -> safeHandle("shoggoth shutdown", ShoggothService::clear));

        eventBridge.register("net.minecraftforge.event.TickEvent$ServerTickEvent",
                event -> safeHandle("ritual audience", () -> RiftAudience.tick(event)));
        eventBridge.register("net.minecraftforge.event.server.ServerStoppingEvent",
                event -> safeHandle("ritual audience shutdown", RiftAudience::clearAll));

        if (RuntimeMinecraft.isClientDistribution()) {
            eventBridge.register("net.minecraftforge.client.event.RenderLivingEvent$Pre",
                    event -> safeHandle("rift shell visibility", () -> ShoggothService.render(event)));
            eventBridge.register("com.mna.api.guidebook.RegisterGuidebooksEvent",
                    event -> safeHandle("Eldritch Codex registration", () -> EldritchCodex.register(event)));
            eventBridge.register(
                    "net.minecraftforge.client.event.RenderLivingEvent$Pre",
                    event -> safeHandle(
                            "Watcher pre-render filtering",
                            () -> WatcherService.onRenderPre(event)));
            eventBridge.register(
                    "net.minecraftforge.client.event.RenderLivingEvent$Post",
                    event -> safeHandle(
                            "Watcher post-render filtering",
                            () -> WatcherService.onRenderPost(event)));
        }

        RuntimeLog.info(
                "Prototype Warp, M&A, Watcher, Displacement, Time Warp, Teleport, Tier-5 armor, "
                        + "and Staff of the Open Way hooks registered.");
    }

    private static void safeHandle(String name, Runnable action) {
        try {
            action.run();
        } catch (Throwable throwable) {
            RuntimeLog.error("Failed during " + name + ": " + throwable, throwable);
        }
    }
}
