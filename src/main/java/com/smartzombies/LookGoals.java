package com.smartzombies;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;

/**
 * Vanilla zombies have two "look" behaviours: they stare at any player within 8 blocks
 * and they randomly turn their heads. Both make a field of view meaningless
 * (the zombie turns to you, then "sees" you). This removes them.
 *
 * The goal lists are protected fields, so they are found by TYPE with reflection
 * (no dependence on obfuscated names). If anything fails, the mod just keeps working without this feature.
 */
public final class LookGoals {
    private static Field[] selectorFields;
    private static boolean failed;

    private LookGoals() {
    }

    /** Returns how many goals were removed. */
    public static int strip(Mob mob) {
        if (failed) {
            return 0;
        }
        try {
            if (selectorFields == null) {
                List<Field> found = new ArrayList<>();
                for (Field f : Mob.class.getDeclaredFields()) {
                    if (f.getType() == GoalSelector.class) {
                        f.setAccessible(true);
                        found.add(f);
                    }
                }
                selectorFields = found.toArray(new Field[0]);
            }
            int removed = 0;
            for (Field f : selectorFields) {
                Object o = f.get(mob);
                if (!(o instanceof GoalSelector selector)) {
                    continue;
                }
                List<Goal> toRemove = new ArrayList<>();
                for (WrappedGoal w : selector.getAvailableGoals()) {
                    Goal g = w.getGoal();
                    if (g instanceof LookAtPlayerGoal || g instanceof RandomLookAroundGoal) {
                        toRemove.add(g);
                    }
                }
                for (Goal g : toRemove) {
                    selector.removeGoal(g);
                    removed++;
                }
            }
            return removed;
        } catch (Throwable t) {
            failed = true;
            SmartZombies.LOGGER.warn("Could not remove vanilla look goals: {}", t.toString());
            return 0;
        }
    }
}
