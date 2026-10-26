package com.smartzombies;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Debug helpers. Toggle in game with /smartzombies debug (needs operator rights / cheats). */
public final class Debug {
    public static volatile boolean enabled = false;

    private Debug() {
    }

    /** A particle above the entity's head. */
    public static void mark(ServerLevel level, Entity e, ParticleOptions type) {
        if (!enabled) {
            return;
        }
        level.sendParticles(type, e.getX(), e.getY() + e.getBbHeight() + 0.4, e.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
    }

    /** Short text over the hotbar. */
    public static void bar(Player p, String text) {
        if (enabled && p instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.literal(text), true);
        }
    }

    /** A chat line. */
    public static void chat(Player p, String text) {
        if (enabled && p instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal(text));
        }
    }
}
