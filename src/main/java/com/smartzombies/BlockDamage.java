package com.smartzombies;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

/**
 * Shared damage on blocks that zombies are breaking.
 * Several zombies hitting the same block add their damage together (crowd pressure).
 */
public final class BlockDamage {
    private static final class Damage {
        float progress;
        long lastTick;
    }

    private static final Map<ResourceLocation, Map<Long, Damage>> MAPS = new HashMap<>();

    private BlockDamage() {
    }

    /** Adds damage (1.0 = block is destroyed). Returns true when the block should be broken now. */
    public static boolean hit(ServerLevel level, BlockPos pos, float amount, long now) {
        Map<Long, Damage> m = MAPS.computeIfAbsent(level.dimension().location(), k -> new HashMap<>());
        long key = pos.asLong();
        Damage d = m.computeIfAbsent(key, k -> new Damage());
        d.progress += amount;
        d.lastTick = now;
        if (d.progress >= 1.0f) {
            m.remove(key);
            level.destroyBlockProgress(crackId(pos), pos, -1);
            return true;
        }
        level.destroyBlockProgress(crackId(pos), pos, Math.min(9, (int) (d.progress * 10.0f)));
        return false;
    }

    /** Forget damage on blocks that nobody hits anymore. */
    public static void cleanup(ServerLevel level, long now) {
        Map<Long, Damage> m = MAPS.get(level.dimension().location());
        if (m == null) {
            return;
        }
        Iterator<Map.Entry<Long, Damage>> it = m.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, Damage> e = it.next();
            if (now - e.getValue().lastTick > 100L) {
                BlockPos pos = BlockPos.of(e.getKey());
                level.destroyBlockProgress(crackId(pos), pos, -1);
                it.remove();
            }
        }
    }

    private static int crackId(BlockPos pos) {
        return pos.hashCode();
    }
}
