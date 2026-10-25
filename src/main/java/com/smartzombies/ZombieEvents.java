package com.smartzombies;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.registries.ForgeRegistries;

/** Connects the game's events to the zombie brains. */
public class ZombieEvents {
    private static final UUID NO_OWNER = new UUID(0L, 0L);

    private final Map<UUID, Vec3> lastPlayerPos = new HashMap<>();
    private final Map<UUID, Long> lastShotNoise = new HashMap<>();

    // ---------------------------------------------------------------- zombie thinking

    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent e) {
        LivingEntity le = e.getEntity();
        if (!(le instanceof Zombie z)) {
            return;
        }
        if (!(z.level() instanceof ServerLevel level)) {
            return;
        }
        ZombieBrain b = ZombieBrain.get(z);
        if (b.isEnabled()) {
            b.tick(z, level);
        }
    }

    /** Vanilla zombies "see" through a 35 block radius with no field of view. Here they only target who they perceived. */
    @SubscribeEvent
    public void onChangeTarget(LivingChangeTargetEvent e) {
        if (!(e.getEntity() instanceof Zombie z)) {
            return;
        }
        if (z.level().isClientSide()) {
            return;
        }
        if (!(e.getNewTarget() instanceof Player p)) {
            return;
        }
        ZombieBrain b = ZombieBrain.get(z);
        if (!b.isEnabled()) {
            return;
        }
        if (!b.canTarget(p, z.level().getGameTime())) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onHurt(LivingHurtEvent e) {
        if (!(e.getEntity() instanceof Zombie z)) {
            return;
        }
        if (!(z.level() instanceof ServerLevel level)) {
            return;
        }
        Entity src = e.getSource().getEntity();
        if (!(src instanceof Player p)) {
            return;
        }
        ZombieBrain b = ZombieBrain.get(z);
        if (b.isEnabled()) {
            b.onAttacked(z, level, p, level.getGameTime());
        }
    }

    // ---------------------------------------------------------------- noises

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.side != LogicalSide.SERVER) {
            return;
        }
        if (!(e.player instanceof ServerPlayer p)) {
            return;
        }
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        if ((now + p.getId()) % 10L != 0L) {
            return;
        }
        Vec3 cur = p.position();
        Vec3 prev = lastPlayerPos.put(p.getUUID(), cur);
        if (p.isSpectator() || (ZConfig.IGNORE_CREATIVE.get() && p.isCreative())) {
            return;
        }
        if (ZConfig.HABITS_ENABLED.get() && (now + p.getId()) % 40L == 0L) {
            HeatMap.record(level, cur.x, cur.z);
        }
        if (prev == null || p.isPassenger() || p.isFallFlying() || p.isCrouching()) {
            return;
        }
        double dx = cur.x - prev.x;
        double dz = cur.z - prev.z;
        double moved = Math.sqrt(dx * dx + dz * dz);
        if (moved < 0.4 || moved > 12.0) {
            return; // standing still, or just teleported
        }
        double radius = p.isSprinting() ? ZConfig.NOISE_SPRINT.get() : ZConfig.NOISE_WALK.get();
        Noise.emit(level, cur, radius, p, null);
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent e) {
        if (e.getLevel() instanceof ServerLevel level) {
            Noise.emit(level, Vec3.atCenterOf(e.getPos()), ZConfig.NOISE_BREAK.get(), e.getPlayer(), null);
        }
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent e) {
        if (e.getLevel() instanceof ServerLevel level) {
            Player src = e.getEntity() instanceof Player pl ? pl : null;
            Noise.emit(level, Vec3.atCenterOf(e.getPos()), ZConfig.NOISE_PLACE.get(), src, null);
        }
    }

    @SubscribeEvent
    public void onExplosion(ExplosionEvent.Start e) {
        if (e.getLevel() instanceof ServerLevel level) {
            Noise.emit(level, e.getExplosion().getPosition(), ZConfig.NOISE_EXPLOSION.get(), null, null);
        }
    }

    /** Shots: arrows and any projectile from another mod (gun bullets) make noise where they appear. */
    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent e) {
        if (!(e.getLevel() instanceof ServerLevel level) || e.loadedFromDisk()) {
            return;
        }
        Entity ent = e.getEntity();
        if (!(ent instanceof Projectile proj)) {
            return;
        }
        Entity owner = proj.getOwner();
        if (owner != null && !(owner instanceof Player)) {
            return; // mobs shooting do not count
        }
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(ent.getType());
        double radius;
        if (key != null && "minecraft".equals(key.getNamespace())) {
            radius = (ent instanceof AbstractArrow) ? ZConfig.NOISE_ARROW.get() : 0.0;
        } else {
            radius = ZConfig.NOISE_PROJECTILE.get();
        }
        if (radius <= 0.0) {
            return;
        }
        // full-auto guns fire many bullets per second: one noise per shooter per 8 ticks is enough
        long now = level.getGameTime();
        UUID id = owner != null ? owner.getUUID() : NO_OWNER;
        Long last = lastShotNoise.get(id);
        if (last != null && now - last < 8L) {
            return;
        }
        lastShotNoise.put(id, now);
        Player src = owner instanceof Player pl ? pl : null;
        Noise.emit(level, ent.position(), radius, src, null);
    }

    // ---------------------------------------------------------------- housekeeping

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.side != LogicalSide.SERVER) {
            return;
        }
        if (!(e.level instanceof ServerLevel level)) {
            return;
        }
        long t = level.getGameTime();
        if (t % 100L == 0L) {
            BlockDamage.cleanup(level, t);
        }
        if (t % 1200L == 0L) {
            HeatMap.decay(level);
        }
    }
}
