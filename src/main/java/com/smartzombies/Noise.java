package com.smartzombies;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A noise is an event, not something zombies poll for: cheap and easy to extend. */
public final class Noise {
    private Noise() {
    }

    /**
     * @param radius base radius in blocks (multiplied by the hearing setting)
     * @param source the player who made the noise, or null
     * @param except a zombie that must not react (the one making the noise), or null
     */
    public static void emit(ServerLevel level, Vec3 pos, double radius, Player source, Zombie except) {
        double r = radius * ZConfig.HEARING_MULT.get();
        if (r < 1.0) {
            return;
        }
        AABB box = new AABB(pos.x - r, pos.y - r * 0.6, pos.z - r, pos.x + r, pos.y + r * 0.6, pos.z + r);
        List<Zombie> zombies = level.getEntitiesOfClass(Zombie.class, box);
        if (zombies.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        double r2 = r * r;
        int budget = ZConfig.MAX_LISTENERS.get();
        for (Zombie z : zombies) {
            if (z == except) {
                continue;
            }
            if (z.distanceToSqr(pos) > r2) {
                continue;
            }
            ZombieBrain b = ZombieBrain.get(z);
            if (!b.isEnabled()) {
                continue;
            }
            b.hear(z, level, pos, source, now);
            budget--;
            if (budget <= 0) {
                break;
            }
        }
    }
}
