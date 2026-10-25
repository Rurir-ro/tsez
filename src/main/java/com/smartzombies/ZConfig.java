package com.smartzombies;

import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;

/** All tunable settings. The file appears in config/smartzombies-common.toml after the first launch. */
public final class ZConfig {
    public static final ForgeConfigSpec SPEC;

    // --- senses ---
    public static final ForgeConfigSpec.IntValue SENSE_INTERVAL;
    public static final ForgeConfigSpec.DoubleValue ACTIVE_RANGE;
    public static final ForgeConfigSpec.DoubleValue SIGHT_RANGE;
    public static final ForgeConfigSpec.DoubleValue SIGHT_FOV;
    public static final ForgeConfigSpec.DoubleValue CLOSE_SENSE;
    public static final ForgeConfigSpec.DoubleValue SNEAK_MULT;
    public static final ForgeConfigSpec.DoubleValue DARK_MULT;
    public static final ForgeConfigSpec.BooleanValue IGNORE_CREATIVE;

    // --- hearing ---
    public static final ForgeConfigSpec.DoubleValue HEARING_MULT;
    public static final ForgeConfigSpec.DoubleValue NOISE_WALK;
    public static final ForgeConfigSpec.DoubleValue NOISE_SPRINT;
    public static final ForgeConfigSpec.DoubleValue NOISE_BREAK;
    public static final ForgeConfigSpec.DoubleValue NOISE_PLACE;
    public static final ForgeConfigSpec.DoubleValue NOISE_EXPLOSION;
    public static final ForgeConfigSpec.DoubleValue NOISE_ARROW;
    public static final ForgeConfigSpec.DoubleValue NOISE_PROJECTILE;
    public static final ForgeConfigSpec.DoubleValue HEAR_NOTICE;
    public static final ForgeConfigSpec.IntValue MAX_LISTENERS;

    // --- memory and calling ---
    public static final ForgeConfigSpec.IntValue MEMORY_TICKS;
    public static final ForgeConfigSpec.IntValue INVESTIGATE_TICKS;
    public static final ForgeConfigSpec.DoubleValue INVESTIGATE_SPEED;
    public static final ForgeConfigSpec.BooleanValue CALL_ENABLED;
    public static final ForgeConfigSpec.DoubleValue CALL_RADIUS;
    public static final ForgeConfigSpec.IntValue CALL_COOLDOWN;

    // --- wandering and habits ---
    public static final ForgeConfigSpec.BooleanValue WANDER_ENABLED;
    public static final ForgeConfigSpec.IntValue WANDER_MIN;
    public static final ForgeConfigSpec.IntValue WANDER_MAX;
    public static final ForgeConfigSpec.BooleanValue HABITS_ENABLED;
    public static final ForgeConfigSpec.IntValue HABIT_RADIUS;

    // --- breaking blocks ---
    public static final ForgeConfigSpec.BooleanValue BREAK_ENABLED;
    public static final ForgeConfigSpec.BooleanValue BREAK_ANY;
    public static final ForgeConfigSpec.DoubleValue BREAK_MAX_HARDNESS;
    public static final ForgeConfigSpec.DoubleValue BREAK_SECONDS;
    public static final ForgeConfigSpec.DoubleValue BREAK_NOISE;

    // --- which mobs ---
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXCLUDED;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Senses: how zombies see").push("senses");
        SENSE_INTERVAL = b.comment("How often (in ticks) a zombie thinks. Bigger = faster game, dumber zombies. 20 ticks = 1 second.")
                .defineInRange("senseIntervalTicks", 10, 5, 40);
        ACTIVE_RANGE = b.comment("Zombies farther than this from every player use vanilla behaviour (saves performance).")
                .defineInRange("activeRange", 80.0, 16.0, 160.0);
        SIGHT_RANGE = b.comment("Max sight distance in blocks (in good light, player standing).")
                .defineInRange("sightRange", 24.0, 8.0, 64.0);
        SIGHT_FOV = b.comment("Field of view in degrees. 360 = sees everywhere around.")
                .defineInRange("sightFovDegrees", 140.0, 60.0, 360.0);
        CLOSE_SENSE = b.comment("Zombies always notice a player this close, even from behind (needs line of sight).")
                .defineInRange("closeSenseRange", 3.0, 0.0, 8.0);
        SNEAK_MULT = b.comment("Sight distance multiplier when the player sneaks.")
                .defineInRange("sneakSightMultiplier", 0.4, 0.05, 1.0);
        DARK_MULT = b.comment("Sight distance multiplier in complete darkness (light 0). Scales up to 1.0 in full light.")
                .defineInRange("darkSightMultiplier", 0.45, 0.1, 1.0);
        IGNORE_CREATIVE = b.comment("Zombies ignore creative-mode players (like vanilla).")
                .define("ignoreCreative", true);
        b.pop();

        b.comment("Hearing: noises that attract zombies. Values are radius in blocks.").push("hearing");
        HEARING_MULT = b.comment("Multiplier for all noise radii. 0 = deaf zombies.")
                .defineInRange("hearingMultiplier", 1.0, 0.0, 4.0);
        NOISE_WALK = b.defineInRange("noiseWalking", 7.0, 0.0, 64.0);
        NOISE_SPRINT = b.defineInRange("noiseSprinting", 16.0, 0.0, 64.0);
        NOISE_BREAK = b.defineInRange("noiseBlockBreak", 12.0, 0.0, 64.0);
        NOISE_PLACE = b.defineInRange("noiseBlockPlace", 6.0, 0.0, 64.0);
        NOISE_EXPLOSION = b.defineInRange("noiseExplosion", 48.0, 0.0, 128.0);
        NOISE_ARROW = b.comment("Vanilla arrows/bolts being shot.")
                .defineInRange("noiseVanillaArrow", 8.0, 0.0, 64.0);
        NOISE_PROJECTILE = b.comment("Projectiles from other mods (this is how gun shots are detected). 0 = off.")
                .defineInRange("noiseModdedProjectile", 40.0, 0.0, 128.0);
        HEAR_NOTICE = b.comment("If a player makes noise within this distance of a zombie, the zombie targets that player right away.")
                .defineInRange("hearNoticeRange", 5.0, 0.0, 16.0);
        MAX_LISTENERS = b.comment("At most this many zombies react to one noise (performance guard).")
                .defineInRange("maxListenersPerNoise", 48, 1, 256);
        b.pop();

        b.comment("Memory and calling for help").push("memory");
        MEMORY_TICKS = b.comment("How long a zombie keeps hunting a player it can no longer perceive (ticks).")
                .defineInRange("memoryTicks", 200, 20, 2400);
        INVESTIGATE_TICKS = b.comment("Max time a zombie spends walking to a heard noise / last known position (ticks).")
                .defineInRange("investigateTicks", 900, 100, 6000);
        INVESTIGATE_SPEED = b.comment("Walking speed multiplier while investigating.")
                .defineInRange("investigateSpeed", 0.9, 0.3, 2.0);
        CALL_ENABLED = b.comment("A zombie that spots a player calls nearby zombies.")
                .define("callEnabled", true);
        CALL_RADIUS = b.defineInRange("callRadius", 24.0, 0.0, 64.0);
        CALL_COOLDOWN = b.comment("Ticks between two calls from the same zombie.")
                .defineInRange("callCooldownTicks", 100, 20, 1200);
        b.pop();

        b.comment("Wandering: idle zombies drift toward players and toward places players visit often").push("wander");
        WANDER_ENABLED = b.define("wanderEnabled", true);
        WANDER_MIN = b.comment("Min ticks between wander trips of one zombie.")
                .defineInRange("wanderMinTicks", 1200, 200, 24000);
        WANDER_MAX = b.comment("Max ticks between wander trips of one zombie.")
                .defineInRange("wanderMaxTicks", 3600, 200, 48000);
        HABITS_ENABLED = b.comment("Zombies remember where players spend time (bases, farms, routes).")
                .define("habitsEnabled", true);
        HABIT_RADIUS = b.comment("How far (blocks) a zombie looks for such places.")
                .defineInRange("habitRadius", 64, 16, 128);
        b.pop();

        b.comment("Breaking blocks").push("breaking");
        BREAK_ENABLED = b.define("breakEnabled", true);
        BREAK_ANY = b.comment("false = only weak blocks (doors, trapdoors, gates, planks, wooden fences, glass, wool, leaves). true = any block up to maxHardness.")
                .define("breakAnyBlock", false);
        BREAK_MAX_HARDNESS = b.comment("Blocks harder than this are never broken. Dirt 0.5, glass 0.3, planks/logs 2, wooden door 3, stone 1.5, iron door 5.")
                .defineInRange("maxHardness", 3.0, 0.0, 50.0);
        BREAK_SECONDS = b.comment("Seconds ONE zombie needs per point of hardness. Several zombies at the same block add up.")
                .defineInRange("secondsPerHardness", 4.0, 0.5, 30.0);
        BREAK_NOISE = b.comment("Noise radius made while breaking a block (calls more zombies to the wall).")
                .defineInRange("breakNoiseRadius", 12.0, 0.0, 64.0);
        b.pop();

        b.comment("Which mobs are affected").push("mobs");
        EXCLUDED = b.comment("Entity ids that keep vanilla behaviour. Any other mob that extends the vanilla Zombie is affected.")
                .defineListAllowEmpty("excludedEntities",
                        List.of("minecraft:zombified_piglin", "minecraft:drowned"),
                        o -> o instanceof String);
        b.pop();

        SPEC = b.build();
    }

    private ZConfig() {
    }
}
