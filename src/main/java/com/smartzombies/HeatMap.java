package com.smartzombies;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/**
 * "Habits": a coarse map of where players spend time. Cells are 8x8 blocks.
 * Idle zombies sometimes walk toward the hottest cell near them.
 */
public final class HeatMap {
    private static final int SHIFT = 3;
    private static final int MAX_CELLS = 3000;
    private static final Map<ResourceLocation, Map<Long, Float>> MAPS = new HashMap<>();

    private HeatMap() {
    }

    public static void record(ServerLevel level, double x, double z) {
        Map<Long, Float> m = MAPS.computeIfAbsent(level.dimension().location(), k -> new HashMap<>());
        long key = ChunkPos.asLong(((int) Math.floor(x)) >> SHIFT, ((int) Math.floor(z)) >> SHIFT);
        float v = m.merge(key, 1.0f, Float::sum);
        if (v > 50.0f) {
            m.put(key, 50.0f);
        }
        if (m.size() > MAX_CELLS) {
            m.values().removeIf(h -> h < 3.0f);
            if (m.size() > MAX_CELLS) {
                m.clear();
            }
        }
    }

    /** Called about once a minute: old habits fade away. */
    public static void decay(ServerLevel level) {
        Map<Long, Float> m = MAPS.get(level.dimension().location());
        if (m == null) {
            return;
        }
        Iterator<Map.Entry<Long, Float>> it = m.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, Float> e = it.next();
            float nv = e.getValue() * 0.9f;
            if (nv < 0.5f) {
                it.remove();
            } else {
                e.setValue(nv);
            }
        }
    }

    /** Center of the most "popular" cell around the given point, or null if nothing interesting. */
    public static Vec3 hottest(ServerLevel level, Vec3 from, int radiusBlocks) {
        Map<Long, Float> m = MAPS.get(level.dimension().location());
        if (m == null || m.isEmpty()) {
            return null;
        }
        int cr = Math.max(1, radiusBlocks >> SHIFT);
        int cx0 = ((int) Math.floor(from.x)) >> SHIFT;
        int cz0 = ((int) Math.floor(from.z)) >> SHIFT;
        double bestScore = 0.0;
        int bx = 0;
        int bz = 0;
        boolean found = false;
        for (int dx = -cr; dx <= cr; dx++) {
            for (int dz = -cr; dz <= cr; dz++) {
                Float h = m.get(ChunkPos.asLong(cx0 + dx, cz0 + dz));
                if (h == null || h < 2.0f) {
                    continue;
                }
                double distCells = Math.sqrt((double) (dx * dx + dz * dz));
                if (distCells < 1.5) {
                    continue; // already there
                }
                double score = h / (1.0 + distCells * 0.25);
                if (score > bestScore) {
                    bestScore = score;
                    bx = cx0 + dx;
                    bz = cz0 + dz;
                    found = true;
                }
            }
        }
        if (!found) {
            return null;
        }
        return new Vec3((bx << SHIFT) + 4.0, from.y, (bz << SHIFT) + 4.0);
    }
}
