package com.eldritchartifice.eye;

import com.mna.api.ManaAndArtificeMod;
import com.mna.api.events.SpellCastEvent;
import com.mna.api.events.CantripCastEvent;
import com.mna.effects.EffectInit;
import com.mna.network.ServerMessageDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

/** All session mutations occur on the logical server thread. */
public final class EyeSessions {
    private static final String MARKER = "eldritcheye_session";
    private static final String POSSESSED_ID = "posessed_entity_id"; // MnA's intentional spelling
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();
    private static final int DEFAULT_SECONDS = 600;


    static final class Session {
        final ServerPlayer player;
        final UnseenEye eye;
        final Vec3 origin;
        final int duration;
        int age;
        final boolean staff;
        final float yaw, pitch, headYaw, bodyYaw;
        final EyeView view;
        int portalCooldown;
        Session(ServerPlayer player, UnseenEye eye, int duration, boolean staff) {
            this.player = player; this.eye = eye; this.origin = player.position(); this.duration = duration;
            this.yaw = player.getYRot(); this.pitch = player.getXRot();
            this.headYaw = player.yHeadRot; this.bodyYaw = player.yBodyRot; this.staff = staff; this.view = new EyeView(player, eye);
        }
    }

    static Session get(ServerPlayer player) { return SESSIONS.get(player.getUUID()); }

    static boolean active(Entity entity) { return SESSIONS.containsKey(entity.getUUID()); }

    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("unseeneye")
                .then(Commands.literal("start").requires(source -> source.hasPermission(2))
                        .executes(ctx -> start(ctx.getSource().getPlayerOrException(), DEFAULT_SECONDS))
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(30, 3600))
                                .executes(ctx -> start(ctx.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(ctx, "seconds")))
                                .then(Commands.argument("augment", IntegerArgumentType.integer(0, 2))
                                        .executes(ctx -> {
                                            var p = ctx.getSource().getPlayerOrException();
                                            int augment = IntegerArgumentType.getInteger(ctx, "augment");
                                            return start(p, IntegerArgumentType.getInteger(ctx, "seconds"),
                                                    augment > 1, StaffBridge.equipped(p), true);
                                        }))))
                .then(Commands.literal("stop").executes(ctx -> {
                    finish(ctx.getSource().getPlayerOrException(), "cancelled"); return 1;
                }))
                .then(Commands.literal("status").executes(ctx -> {
                    var player = ctx.getSource().getPlayerOrException();
                    Session s = SESSIONS.get(player.getUUID());
                    player.sendSystemMessage(Component.literal(s == null ? "No Unseen Eye active."
                            : "Unseen Eye: " + Math.max(0, (s.duration - s.age) / 20)
                            + "s remaining; eye health " + s.eye.getHealth() + "; upkeep 1 mana/s; no distance tether."));
                    return 1;
                })));
    }

    private static int start(ServerPlayer player, int seconds) {
        return start(player, seconds, false, StaffBridge.equipped(player), true);
    }

    static int start(ServerPlayer player, int seconds, boolean phase, boolean staff, boolean charge) {
        if (!EyeComponent.allowed(player)) {
            player.sendSystemMessage(Component.literal("The Unseen Eye requires Tier 3 and Eldritch allegiance."));
            return 0;
        }
        if (active(player) || player.hasEffect(EffectInit.POSSESSION.get())
                || player.hasEffect(EffectInit.MIND_VISION.get()) || !player.isAlive()
                || player.isSpectator() || player.isPassenger() || player.isSleeping()
                || !player.onGround()) {
            player.sendSystemMessage(Component.literal("Stand on solid ground, awake and unmounted, without another remote-view effect."));
            return 0;
        }
        UnseenEye eye = UnseenEyeModule.EYE.get().create(player.serverLevel());
        if (eye == null) return 0;
        Vec3 at = player.getEyePosition();
        eye.moveTo(at.x, at.y, at.z, player.getYRot(), player.getXRot());
        if (!player.level().noCollision(eye, eye.getBoundingBox())) {
            player.sendSystemMessage(Component.literal("There is not enough clear space for the eye."));
            return 0;
        }
        if (charge && !resourcePayment(player, 20, false)) {
            player.sendSystemMessage(Component.literal("The eye requires 20 mana to open and 1 mana per second to sustain."));
            return 0;
        }
        if (!player.serverLevel().addFreshEntity(eye)) return 0;
        if (charge && !pay(player, 20)) { eye.discard(); return 0; }
        player.stopUsingItem();
        player.closeContainer();
        Session session = new Session(player, eye, seconds * 20, staff);
        eye.setPhase(phase);
        SESSIONS.put(player.getUUID(), session);
        player.getPersistentData().putBoolean(MARKER, true);
        // Amp 0 disables native double-tap dismissal. Our client forwards movement explicitly; the body stays put.
        player.getPersistentData().putInt(POSSESSED_ID, eye.getId());
        player.addEffect(new MobEffectInstance(EffectInit.POSSESSION.get(), seconds * 20, 0, false, false));
        eye.addEffect(new MobEffectInstance(EffectInit.POSSESSION.get(), seconds * 20, 0, false, false));
        player.sendSystemMessage(Component.literal("The Unseen Eye opens. Fly with movement/Space/Sneak. Use the M&A radial key (Z) for Return or Open portal. Double-tap exit is disabled."));
        EyeNetwork.state(player, eye.getId(), staff);
        return 1;
    }

    private static boolean pay(ServerPlayer player, float amount) {
        return resourcePayment(player, amount, true);
    }

    private static boolean resourcePayment(ServerPlayer player, float amount, boolean consume) {
        var capability = ManaAndArtificeMod.getMagicCapability();
        if (capability == null) return false;
        var magic = player.getCapability(capability).orElse(null);
        if (magic == null || magic.getCastingResource() == null) return false;
        var resource = magic.getCastingResource();
        if (!resource.hasEnough(player, amount)) return false;
        // Scouting upkeep must not silently spend the faction's Warp-generating reserve.
        if ("eldritchartifice:eldritch_mana".equals(String.valueOf(resource.getRegistryName()))) {
            try {
                float ordinary = ((Number) resource.getClass().getMethod("getOrdinaryRemaining").invoke(resource)).floatValue();
                if (!Float.isFinite(ordinary) || ordinary < amount) return false;
            } catch (ReflectiveOperationException incompatibleVersion) { return false; }
        }
        if (consume) {
            resource.consume(player, amount);
            magic.forceSync();
        }
        return true;
    }

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (Session s : SESSIONS.values().toArray(Session[]::new)) {
            ServerPlayer p = s.player;
            String reason = null;
            if (!p.isAlive() || !s.eye.isAlive() || s.eye.isRemoved()) reason = "connection broken";
            else if (p.level() != s.eye.level()) reason = "dimension changed";
            else if (!p.hasEffect(EffectInit.POSSESSION.get()) || !s.eye.hasEffect(EffectInit.POSSESSION.get())) reason = "possession ended";
            else if (p.getPersistentData().getInt(POSSESSED_ID) != s.eye.getId()) reason = "possession replaced";
            else if (p.position().distanceToSqr(s.origin) > 1) reason = "body moved";
            else if (++s.age >= s.duration) reason = "duration expired";
            else if (s.age % 20 == 0 && !pay(p, 1)) reason = "mana exhausted";
            if (reason != null) { finish(p, reason); continue; }
            // Allow the entity spawn/tracking packets to arrive before switching the camera.
            if (s.age == 5 || s.age == 10) {
                ServerMessageDispatcher.sendPlayerPosessionMessage(p, s.eye);
            }
            s.view.tick();
            EyePortals.tick(s);
            if (p.containerMenu != p.inventoryMenu) p.closeContainer();
        }
    }

    @SubscribeEvent public static void bodyPose(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        Session s = get(player);
        if (s == null) return;
        // Eye look arrives in its own input packet. Keep the body still for other players.
        player.setYRot(s.yaw); player.yRotO = s.yaw;
        player.setXRot(s.pitch); player.xRotO = s.pitch;
        player.yHeadRot = player.yHeadRotO = s.headYaw;
        player.yBodyRot = player.yBodyRotO = s.bodyYaw;
    }

    private static void finish(ServerPlayer player, String reason) {
        Session s = SESSIONS.remove(player.getUUID());
        if (s == null) return;
        player.closeContainer();
        s.view.close();
        EyeNetwork.state(player, -1, false);
        // Remove native possession before deleting its target. Do not teleport the real body.
        boolean ownsPossession = player.getPersistentData().getInt(POSSESSED_ID) == s.eye.getId();
        if (ownsPossession) player.removeEffect(EffectInit.POSSESSION.get());
        s.eye.removeEffect(EffectInit.POSSESSION.get());
        if (ownsPossession) player.getPersistentData().remove(POSSESSED_ID);
        player.getPersistentData().remove(MARKER);
        if (ownsPossession || !player.hasEffect(EffectInit.POSSESSION.get()))
            ServerMessageDispatcher.sendPlayerPosessionMessage(player, player);
        s.eye.discard();
        player.sendSystemMessage(Component.literal("The Unseen Eye closes: " + reason + "."));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST) public static void damaged(LivingDamageEvent event) {
        if (event.getAmount() <= 0) return;
        if (event.getEntity() instanceof ServerPlayer player) finish(player, "body damaged");
        // The event is never cancelled: damage to the real body still applies normally.
        if (event.getEntity() instanceof UnseenEye eye) closeEye(eye, "eye damaged");
    }
    private static void closeEye(UnseenEye eye, String reason) {
        for (Session s : SESSIONS.values().toArray(Session[]::new))
            if (s.eye == eye) finish(s.player, reason);
    }
    @SubscribeEvent public static void died(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) finish(player, "body died");
        if (event.getEntity() instanceof UnseenEye eye) closeEye(eye, "eye destroyed");
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) finish(player, "logout");
    }
    @SubscribeEvent public static void dimension(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof UnseenEye) event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer player) finish(player, "dimension travel");
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && p.getPersistentData().getBoolean(MARKER)) {
            p.removeEffect(EffectInit.POSSESSION.get());
            p.getPersistentData().remove(POSSESSED_ID);
            p.getPersistentData().remove(MARKER);
            ServerMessageDispatcher.sendPlayerPosessionMessage(p, p);
        }
    }
    @SubscribeEvent public static void stopping(ServerStoppingEvent event) {
        for (Session s : SESSIONS.values().toArray(Session[]::new)) finish(s.player, "server stopping");
    }
    @SubscribeEvent public static void target(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof UnseenEye) event.setCanceled(true);
    }
    @SubscribeEvent public static void interact(PlayerInteractEvent event) {
        if (active(event.getEntity()) && event.isCancelable()) event.setCanceled(true);
    }
    @SubscribeEvent public static void attack(AttackEntityEvent event) {
        if (active(event.getEntity())) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void outgoing(LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof UnseenEye
                || event.getSource().getDirectEntity() instanceof UnseenEye) event.setCanceled(true);
    }
    @SubscribeEvent public static void breakBlock(BlockEvent.BreakEvent event) {
        if (active(event.getPlayer())) event.setCanceled(true);
    }
    @SubscribeEvent public static void pickup(EntityItemPickupEvent event) {
        if (active(event.getEntity())) event.setCanceled(true);
    }
    @SubscribeEvent public static void toss(ItemTossEvent event) {
        if (active(event.getPlayer())) {
            // Cancelling toss alone deletes the already-removed stack; return it explicitly.
            event.setCanceled(true);
            var player = event.getPlayer();
            var stack = event.getEntity().getItem();
            if (!player.getInventory().add(stack) && !stack.isEmpty()) {
                // If another mod filled the freed slot, preserve the remainder at the body.
                // Direct entity addition avoids recursively raising another toss event.
                event.getEntity().setPos(player.position());
                player.level().addFreshEntity(event.getEntity());
            }
        }
    }
    @SubscribeEvent public static void spell(SpellCastEvent event) {
        if (event.getSource() == null) return;
        LivingEntity caster = event.getSource().getCaster();
        if (caster instanceof ServerPlayer player && event.getSpell().containsPart(EyeComponent.ID)) {
            if (event.getSpell().isChanneled() || !EyeComponent.allowed(player)) {
                event.setCanceled(true); return;
            }
            if (event.getContext() != null) event.getContext().getMeta().putBoolean("eldritcheye_staff_at_cast", StaffBridge.equipped(player));
        }
        if (caster != null && (active(caster) || caster instanceof UnseenEye) && event.isCancelable())
            event.setCanceled(true);
    }
    @SubscribeEvent public static void projectile(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof com.mna.entities.sorcery.targeting.SpellProjectile projectile
                && projectile.getShooter() instanceof ServerPlayer player
                && !event.loadedFromDisk()
                && !projectile.getPersistentData().contains("eldritcheye_staff_at_cast"))
            projectile.getPersistentData().putBoolean("eldritcheye_staff_at_cast", StaffBridge.equipped(player));
    }
    static void action(ServerPlayer player, int action) {
        Session s = get(player);
        if (s == null) return;
        if (action == 0) { finish(player, "returned"); return; }
        if (action != 1 || !s.staff) return;
        if (!resourcePayment(player, 150, false)) {
            player.sendSystemMessage(Component.literal("The Open Way requires 150 ordinary mana.")); return;
        }
        String failure = StaffBridge.open(player, s.eye);
        if (failure != null) {
            player.sendSystemMessage(Component.literal("The portal cannot open: " + failure + ".")); return;
        }
        pay(player, 150);
        finish(player, "the Open Way remains open for 30 seconds");
    }
    @SubscribeEvent public static void cantrip(CantripCastEvent event) {
        if (event.getCrafter() != null && active(event.getCrafter())) event.setCanceled(true);
    }
}
