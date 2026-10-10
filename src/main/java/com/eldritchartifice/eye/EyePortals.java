package com.eldritchartifice.eye;

import com.mna.entities.rituals.Portal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

final class EyePortals {
    static void tick(EyeSessions.Session s) {
        if (s.portalCooldown > 0) { --s.portalCooldown; return; }
        for (Portal portal : s.eye.level().getEntitiesOfClass(Portal.class, s.eye.getBoundingBox())) {
            if (portal.getAge() < 40) continue;
            BlockPos at = portal.getTeleportBlockPos();
            if (at == null || at.equals(BlockPos.ZERO) || portal.getDimension() == null) continue;
            if (!portal.getDimension().equals(s.eye.level().dimension())) {
                s.portalCooldown = 100;
                s.player.sendSystemMessage(Component.literal("The eye cannot hold its view across dimensions yet."));
                return;
            }
            s.portalCooldown = 100;
            // Destination is the portal's actual arrival point, not an anchor guessed from its location.
            s.eye.teleportTo(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5);
            s.view.tick();
            return;
        }
    }
}
