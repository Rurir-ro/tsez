package com.smartzombies;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The "mind" of one zombie: what it saw, heard and remembers, and where it is going.
 * One small object per zombie, created lazily. Does not keep the zombie alive in memory.
 */
public final class ZombieBrain {
    private static final Map<Zombie, ZombieBrain> BRAINS = new WeakHashMap<>();

    private boolean enabled = true;

    // memory of the last player perceived
    private UUID perceivedPlayer;
    private long perceivedTick = -1_000_000L;
    private Vec3 lastKnownPos;
    private boolean tracking;

    // current "go there" task (noise, last known position, wandering)
    private Vec3 investigatePos;
    private long investigateUntil;
    private long repathAt;
    private int searchesLeft;

    private long callCooldownUntil;
    private long nextWander;

    // stuck detection for block breaking
    private Vec3 lastPos;
    private int stuckCount;

    private ZombieBrain() {
    }

    public static ZombieBrain get(Zombie z) {
        ZombieBrain b = BRAINS.get(z);
        if (b == null) {
            b = new ZombieBrain();
            b.enabled = !isExcluded(z);
            BRAINS.put(z, b);
        }
        return b;
    }

    private static boolean isExcluded(Zombie z) {
        if (z instanceof ZombifiedPiglin) {
            return true;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(z.getType());
        if (id == null) {
            return false;
        }
        String s = id.toString();
        for (Object o : ZConfig.EXCLUDED.get()) {
            if (s.equals(o)) {
                return true;
            }
        }
        return false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    private static boolean isValidTarget(Player p) {
        if (!p.isAlive() || p.isSpectator()) {
            return false;
        }
        return !(ZConfig.IGNORE_CREATIVE.get() && p.isCreative());
    }

    /** Used by the target-change filter: vanilla may only target players this zombie actually perceived. */
    public boolean canTarget(Player p, long now) {
        return perceivedPlayer != null
                && perceivedPlayer.equals(p.getUUID())
                && now - perceivedTick <= ZConfig.MEMORY_TICKS.get();
    }

    // ------------------------------------------------------------------ main loop

    public void tick(Zombie z, ServerLevel level) {
        if (z.isNoAi() || !z.isAlive()) {
            return;
        }
        long now = level.getGameTime();
        int interval = ZConfig.SENSE_INTERVAL.get();
        if ((now + z.getId()) % interval != 0L) {
            return;
        }
        // far away from everybody: leave the zombie to vanilla, costs nothing
        if (level.getNearestPlayer(z, ZConfig.ACTIVE_RANGE.get()) == null) {
            return;
        }
        perceive(z, level, now);
        maintain(z, now);
        if (z.getTarget() == null) {
            idle(z, level, now);
        }
        breakStep(z, level, now, interval);
    }

    // ------------------------------------------------------------------ sight

    private void perceive(Zombie z, ServerLevel level, long now) {
        double baseRange = ZConfig.SIGHT_RANGE.get();
        double cosHalfFov = Math.cos(Math.toRadians(ZConfig.SIGHT_FOV.get() / 2.0));
        double close = ZConfig.CLOSE_SENSE.get();
        double dark = ZConfig.DARK_MULT.get();
        Vec3 eye = z.getEyePosition();
        Vec3 look = z.getViewVector(1.0F);

        Player best = null;
        double bestDist = Double.MAX_VALUE;
        for (Player p : level.players()) {
            if (!isValidTarget(p)) {
                continue;
            }
            double range = baseRange;
            if (p.isCrouching()) {
                range *= ZConfig.SNEAK_MULT.get();
            }
            if (p.isInvisible()) {
                range *= 0.3;
            }
            int light = level.getMaxLocalRawBrightness(p.blockPosition());
            range *= dark + (1.0 - dark) * (light / 15.0);

            // cheapest check first: distance
            double d2 = z.distanceToSqr(p);
            if (d2 > range * range) {
                continue;
            }
            double dist = Math.sqrt(d2);
            // then the angle
            if (dist > close) {
                Vec3 to = p.getEyePosition().subtract(eye).normalize();
                if (look.dot(to) < cosHalfFov) {
                    continue;
                }
            }
            // the expensive ray check last
            if (!z.hasLineOfSight(p)) {
                continue;
            }
            if (dist < bestDist) {
                bestDist = dist;
                best = p;
            }
        }
        if (best != null) {
            notice(z, level, best, now, true);
        }
    }

    /** The zombie became aware of a player: remember, hunt and (optionally) shout. */
    public void notice(Zombie z, ServerLevel level, Player p, long now, boolean callOthers) {
        perceivedPlayer = p.getUUID();
        perceivedTick = now;
        lastKnownPos = p.position();
        investigatePos = null;
        tracking = true;
        if (z.getTarget() != p) {
            z.setTarget(p);
        }
        if (callOthers && ZConfig.CALL_ENABLED.get() && now >= callCooldownUntil) {
            call(z, level, p, now);
        }
    }

    public void onAttacked(Zombie z, ServerLevel level, Player attacker, long now) {
        if (isValidTarget(attacker)) {
            notice(z, level, attacker, now, true);
        }
    }

    private void call(Zombie z, ServerLevel level, Player p, long now) {
        callCooldownUntil = now + ZConfig.CALL_COOLDOWN.get();
        double r = ZConfig.CALL_RADIUS.get();
        if (r < 1.0) {
            return;
        }
        List<Zombie> near = level.getEntitiesOfClass(Zombie.class, z.getBoundingBox().inflate(r, r * 0.5, r));
        int budget = ZConfig.MAX_LISTENERS.get();
        for (Zombie o : near) {
            if (o == z || o.getTarget() != null) {
                continue;
            }
            ZombieBrain ob = get(o);
            if (!ob.enabled) {
                continue;
            }
            ob.startInvestigate(p.position(), now, 2);
            budget--;
            if (budget <= 0) {
                break;
            }
        }
    }

    // ------------------------------------------------------------------ hearing

    public void hear(Zombie z, ServerLevel level, Vec3 pos, Player source, long now) {
        if (z.getTarget() != null) {
            return; // already busy hunting
        }
        if (source != null && isValidTarget(source)) {
            double r = ZConfig.HEAR_NOTICE.get();
            if (z.distanceToSqr(source) <= r * r) {
                notice(z, level, source, now, false);
                return;
            }
        }
        startInvestigate(pos, now, 2);
    }

    // ------------------------------------------------------------------ memory

    private void maintain(Zombie z, long now) {
        LivingEntity t = z.getTarget();
        if (t instanceof Player) {
            tracking = true;
            if (now - perceivedTick > ZConfig.MEMORY_TICKS.get()) {
                z.setTarget(null);
                tracking = false;
                if (lastKnownPos != null) {
                    startInvestigate(lastKnownPos, now, 2);
                }
            }
        } else if (t == null && tracking) {
            // target was dropped (by vanilla or by us): go and look where we saw it last
            tracking = false;
            if (lastKnownPos != null) {
                startInvestigate(lastKnownPos, now, 2);
            }
        }
    }

    private void startInvestigate(Vec3 pos, long now, int searches) {
        investigatePos = pos;
        investigateUntil = now + ZConfig.INVESTIGATE_TICKS.get();
        searchesLeft = searches;
        repathAt = 0L;
    }

    // ------------------------------------------------------------------ idle behaviour

    private void idle(Zombie z, ServerLevel level, long now) {
        if (investigatePos != null) {
            if (now > investigateUntil) {
                investigatePos = null;
                return;
            }
            double dx = investigatePos.x - z.getX();
            double dz = investigatePos.z - z.getZ();
            double dy = investigatePos.y - z.getY();
            if (dx * dx + dz * dz < 9.0 && Math.abs(dy) < 4.0) {
                arrived(z, now);
                return;
            }
            if (now >= repathAt || z.getNavigation().isDone()) {
                moveToward(z, investigatePos, ZConfig.INVESTIGATE_SPEED.get());
                repathAt = now + 40L;
            }
            return;
        }

        if (!ZConfig.WANDER_ENABLED.get()) {
            return;
        }
        if (nextWander == 0L) {
            nextWander = now + randomWanderDelay(z);
            return;
        }
        if (now < nextWander) {
            return;
        }
        nextWander = now + randomWanderDelay(z);
        Vec3 dest = pickWanderDestination(z, level);
        if (dest != null) {
            startInvestigate(dest, now, 1);
        }
    }

    /** Reached the spot: look around a bit by walking to a couple of nearby points, then give up. */
    private void arrived(Zombie z, long now) {
        if (searchesLeft > 0) {
            searchesLeft--;
            Vec3 p = DefaultRandomPos.getPos(z, 8, 4);
            if (p != null) {
                investigatePos = p;
                investigateUntil = now + 200L;
                repathAt = 0L;
                return;
            }
        }
        investigatePos = null;
    }

    private static int randomWanderDelay(Zombie z) {
        int min = ZConfig.WANDER_MIN.get();
        int max = Math.max(min + 1, ZConfig.WANDER_MAX.get());
        return min + z.getRandom().nextInt(max - min);
    }

    private static Vec3 pickWanderDestination(Zombie z, ServerLevel level) {
        if (ZConfig.HABITS_ENABLED.get() && z.getRandom().nextInt(10) < 6) {
            Vec3 hot = HeatMap.hottest(level, z.position(), ZConfig.HABIT_RADIUS.get());
            if (hot != null) {
                return hot;
            }
        }
        Player p = level.getNearestPlayer(z.getX(), z.getY(), z.getZ(), ZConfig.ACTIVE_RANGE.get(),
                ZConfig.IGNORE_CREATIVE.get());
        if (p == null) {
            return null;
        }
        // not exactly at the player: a rough direction, like a smell
        return new Vec3(
                p.getX() + z.getRandom().nextInt(25) - 12,
                p.getY(),
                p.getZ() + z.getRandom().nextInt(25) - 12);
    }

    private static void moveToward(Zombie z, Vec3 dest, double speed) {
        PathNavigation nav = z.getNavigation();
        double dx = dest.x - z.getX();
        double dz = dest.z - z.getZ();
        // paths are limited in length, so far goals are approached in hops
        if (dx * dx + dz * dz > 24.0 * 24.0) {
            Vec3 hop = DefaultRandomPos.getPosTowards(z, 16, 7, dest, Math.PI / 2.0);
            if (hop != null) {
                nav.moveTo(hop.x, hop.y, hop.z, speed);
                return;
            }
        }
        nav.moveTo(dest.x, dest.y, dest.z, speed);
    }

    // ------------------------------------------------------------------ breaking blocks

    private void breakStep(Zombie z, ServerLevel level, long now, int interval) {
        if (!ZConfig.BREAK_ENABLED.get()) {
            return;
        }
        Vec3 goal = null;
        LivingEntity t = z.getTarget();
        if (t != null) {
            goal = t.position();
        } else if (investigatePos != null) {
            goal = investigatePos;
        }
        Vec3 pos = z.position();
        double moved2 = lastPos == null ? 1.0 : pos.distanceToSqr(lastPos);
        lastPos = pos;
        if (goal == null) {
            stuckCount = 0;
            return;
        }
        double dx = goal.x - pos.x;
        double dz = goal.z - pos.z;
        double horiz2 = dx * dx + dz * dz;
        if (horiz2 < 6.25 || horiz2 > 24.0 * 24.0 || moved2 > 0.09) {
            stuckCount = 0; // close enough to fight, too far to care, or still walking
            return;
        }
        stuckCount++;
        if (stuckCount < 2) {
            return; // standing still for about 2 thinking steps = really stuck
        }
        Direction dir = Direction.getNearest(dx, 0.0, dz);
        BlockPos front = z.blockPosition().relative(dir);
        BlockPos[] candidates = new BlockPos[] {front, front.above()};
        for (BlockPos c : candidates) {
            BlockState s = level.getBlockState(c);
            if (s.getCollisionShape(level, c).isEmpty()) {
                continue;
            }
            if (!canBreak(level, c, s)) {
                continue;
            }
            float hardness = Math.max(0.2f, s.getDestroySpeed(level, c));
            float amount = interval / (20.0f * hardness * ZConfig.BREAK_SECONDS.get().floatValue());
            boolean done = BlockDamage.hit(level, c, amount, now);
            z.getLookControl().setLookAt(Vec3.atCenterOf(c));
            z.swing(InteractionHand.MAIN_HAND);
            level.playSound(null, c, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE,
                    0.7f, 0.8f + z.getRandom().nextFloat() * 0.4f);
            Noise.emit(level, Vec3.atCenterOf(c), ZConfig.BREAK_NOISE.get(), null, z);
            if (done) {
                level.destroyBlock(c, false);
                stuckCount = 0;
            }
            return;
        }
    }

    private static boolean canBreak(ServerLevel level, BlockPos pos, BlockState s) {
        if (s.isAir()) {
            return false;
        }
        float h = s.getDestroySpeed(level, pos);
        if (h < 0.0f) {
            return false; // bedrock and the like
        }
        if (h > ZConfig.BREAK_MAX_HARDNESS.get()) {
            return false;
        }
        return ZConfig.BREAK_ANY.get() || isWeakBlock(s);
    }

    private static boolean isWeakBlock(BlockState s) {
        if (s.is(BlockTags.DOORS) || s.is(BlockTags.TRAPDOORS) || s.is(BlockTags.FENCE_GATES)
                || s.is(BlockTags.PLANKS) || s.is(BlockTags.WOODEN_FENCES) || s.is(BlockTags.WOODEN_SLABS)
                || s.is(BlockTags.WOOL) || s.is(BlockTags.LEAVES)) {
            return true;
        }
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(s.getBlock());
        return id != null && id.getPath().contains("glass");
    }
}
