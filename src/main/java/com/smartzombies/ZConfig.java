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
    public static final ForgeConfigSpec.BooleanValue REMOVE_LOOK_GOALS;

    // --- gun shots ---
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SHOT_RULES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SUPPRESSOR_WORDS;
    public static final ForgeConfigSpec.DoubleValue SUPPRESSED_MULT;

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
        NOISE_PROJECTILE = b.comment("Projectiles from other mods (this is how gun shots are detected): noise when NO rule from [shots] matches. 0 = off.")
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
        REMOVE_LOOK_GOALS = b.comment("Remove the vanilla 'stare at nearby players' and 'randomly turn the head' behaviours. Without this a zombie turns to you and then 'sees' you.")
                .define("removeVanillaLookGoals", true);
        b.pop();

        b.comment("Gun shot noise. Each rule is  keyword=radius  (blocks). Rules are checked from the top, the FIRST match wins.",
                "The keyword is searched in the lowercase ids of the gun, the ammo and the bullet (for example glock, 9mm, ak47, 556).",
                "Turn on /smartzombies debug and shoot: the chat shows exactly which text the mod read, so you can add precise rules.")
                .push("shots");
        SHOT_RULES = b.defineListAllowEmpty("rules",
                List.of(
                        // artillery, launchers
                        "mortar=90", "rpg=80", "rocket=80", "missile=80", "launcher=70", "m320=60", "cannon=95", "grenade=8",
                        // anti-materiel and sniper rifles
                        "50bmg=95", "m107=95", "barrett=95", "m82=95", "m95=95", "338=90", "408=90", "cheytac=90",
                        "m200=90", "awp=85", "sniper=80",
                        // shotguns
                        "12g=55", "12ga=55", "gauge=55", "shotgun=55", "m870=55", "spas=55", "benelli=55", "m1014=55",
                        "mossberg=55", "aa12=55", "saiga=55", "ksg=55", "db_short=55", "db_long=55",
                        // machine guns
                        "m240=72", "m249=68", "pkm=70", "m60=70", "mg42=70", "minigun=75", "negev=68", "lmg=68", "rpk=66",
                        // big rifle calibers
                        "762x54=76", "30_06=76", "3006=76", "mosin=76", "svd=76", "m700=76", "m24=76", "kar98=76",
                        // submachine guns (before pistol calibers, because they shoot those)
                        "thompson=36", "mp5=36", "mp7=36", "mp9=34", "uzi=34", "p90=36", "vector=36", "ump45=36", "ump9=36",
                        "mac10=34", "pp19=34", "bizon=34", "kriss=36", "scorpion=34", "smg=36", "57x28=36", "46x30=36",
                        // battle rifles
                        "308=72", "762x51=72", "m14=72", "m1a=72", "scar_h=72",
                        // assault rifles
                        "762x39=62", "545x39=58", "68x51=66", "58x42=58", "556=60", "223=58", "ak47=62", "akm=62",
                        "ak74=58", "ak12=58", "m4a1=60", "m16=60", "hk416=60", "scar=62", "aug=58", "famas=58",
                        "g36=58", "qbz=58", "type81=62", "type_81=62", "sks=62", "rifle=62",
                        // pistols and revolvers
                        "357=44", "44mag=46", "50ae=50", "deagle=50", "revolver=44", "magnum=46", "b93r=34", "10mm=34",
                        "40sw=34", "45acp=32", "m1911=32", "9mm=30", "glock=30", "p320=30", "usp=30", "cz75=30",
                        "beretta=30", "makarov=28", "380=26", "22lr=18", "pistol=30"),
                o -> o instanceof String s && s.contains("="));
        SUPPRESSOR_WORDS = b.comment("If any of these words appears in the gun's data (attachments), the shot counts as suppressed.")
                .defineListAllowEmpty("suppressorKeywords",
                        List.of("silenc", "suppress", "supress", "muffl"),
                        o -> o instanceof String);
        SUPPRESSED_MULT = b.comment("Noise radius multiplier for suppressed shots.")
                .defineInRange("suppressedMultiplier", 0.3, 0.0, 1.0);
        b.pop();

        SPEC = b.build();
    }

    private ZConfig() {
    }
}
