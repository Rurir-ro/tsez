package com.smartzombies;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Works out how loud a gun shot is, without depending on the gun mods:
 * it reads the ids of the gun, the ammo and the bullet, and looks for a suppressor in the gun's data.
 * Everything is guarded: if something unexpected happens the default noise is used.
 */
public final class ShotClassifier {

    public static final class Result {
        public final double radius;
        public final boolean suppressed;
        public final String text;
        public final String rule;

        Result(double radius, boolean suppressed, String text, String rule) {
            this.radius = radius;
            this.suppressed = suppressed;
            this.text = text;
            this.rule = rule;
        }
    }

    private static List<? extends String> cachedRaw;
    private static String[] ruleWords = new String[0];
    private static double[] ruleRadii = new double[0];

    private ShotClassifier() {
    }

    public static Result classify(Entity bullet, Entity owner) {
        StringBuilder ids = new StringBuilder();
        String[] suppressorWords = suppressorWords();
        boolean suppressed = false;

        try {
            ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKey(bullet.getType());
            if (type != null) {
                ids.append(type);
            }
            CompoundTag bulletTag = new CompoundTag();
            bullet.saveWithoutId(bulletTag);
            collectIds(bulletTag, ids);
            if (scan(bulletTag, null, suppressorWords, 0)) {
                suppressed = true;
            }
        } catch (Throwable ignored) {
            // some entities cannot be saved at this moment: fine, we just know less
        }

        try {
            if (owner instanceof LivingEntity shooter) {
                ItemStack stack = shooter.getMainHandItem();
                if (!stack.isEmpty()) {
                    ResourceLocation itemKey = ForgeRegistries.ITEMS.getKey(stack.getItem());
                    if (itemKey != null) {
                        ids.append(' ').append(itemKey);
                    }
                    CompoundTag tag = stack.getTag();
                    if (tag != null) {
                        collectIds(tag, ids);
                        if (scan(tag, null, suppressorWords, 0)) {
                            suppressed = true;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
            // same here
        }

        String text = ids.toString().toLowerCase(Locale.ROOT);
        if (!suppressed && matchesAny(text, suppressorWords)) {
            suppressed = true; // e.g. a gun whose own id says "suppressed"
        }

        refreshRules();
        double radius = ZConfig.NOISE_PROJECTILE.get();
        String rule = "по умолчанию";
        for (int i = 0; i < ruleWords.length; i++) {
            if (text.contains(ruleWords[i])) {
                radius = ruleRadii[i];
                rule = ruleWords[i] + "=" + (int) ruleRadii[i];
                break;
            }
        }
        if (suppressed && radius > 0.0) {
            radius = Math.max(2.0, radius * ZConfig.SUPPRESSED_MULT.get());
        }
        return new Result(radius, suppressed, text, rule);
    }

    /** Only top level string values whose key mentions "gun" or "ammo" (GunId, AmmoId, GunDisplayId...). */
    private static void collectIds(CompoundTag tag, StringBuilder out) {
        for (String key : tag.getAllKeys()) {
            String k = key.toLowerCase(Locale.ROOT);
            if (!(k.contains("gun") || k.contains("ammo"))) {
                continue;
            }
            Tag value = tag.get(key);
            if (value instanceof StringTag s) {
                out.append(' ').append(s.getAsString());
            }
        }
    }

    private static String[] suppressorWords() {
        List<? extends String> raw = ZConfig.SUPPRESSOR_WORDS.get();
        String[] words = new String[raw.size()];
        for (int i = 0; i < words.length; i++) {
            words[i] = raw.get(i).toLowerCase(Locale.ROOT);
        }
        return words;
    }

    private static boolean matchesAny(String text, String[] words) {
        for (String w : words) {
            if (!w.isEmpty() && text.contains(w)) {
                return true;
            }
        }
        return false;
    }

    /** Looks through the whole data tree for a suppressor keyword in names or text values. */
    private static boolean scan(Tag tag, String key, String[] words, int depth) {
        if (tag == null || depth > 6) {
            return false;
        }
        boolean keyHit = key != null && matchesAny(key.toLowerCase(Locale.ROOT), words);
        if (tag instanceof CompoundTag compound) {
            if (keyHit && !compound.isEmpty()) {
                return true;
            }
            for (String k : compound.getAllKeys()) {
                if (scan(compound.get(k), k, words, depth + 1)) {
                    return true;
                }
            }
            return false;
        }
        if (tag instanceof ListTag list) {
            if (keyHit && !list.isEmpty()) {
                return true;
            }
            for (Tag element : list) {
                if (scan(element, null, words, depth + 1)) {
                    return true;
                }
            }
            return false;
        }
        if (tag instanceof StringTag s) {
            String value = s.getAsString();
            return matchesAny(value.toLowerCase(Locale.ROOT), words) || (keyHit && !value.isEmpty());
        }
        if (tag instanceof NumericTag n) {
            return keyHit && n.getAsDouble() != 0.0;
        }
        return false;
    }

    private static void refreshRules() {
        List<? extends String> raw = ZConfig.SHOT_RULES.get();
        if (raw == cachedRaw) {
            return;
        }
        cachedRaw = raw;
        List<String> words = new ArrayList<>();
        List<Double> radii = new ArrayList<>();
        for (String s : raw) {
            int i = s.lastIndexOf('=');
            if (i <= 0) {
                continue;
            }
            String word = s.substring(0, i).trim().toLowerCase(Locale.ROOT);
            if (word.isEmpty()) {
                continue;
            }
            try {
                double r = Double.parseDouble(s.substring(i + 1).trim());
                words.add(word);
                radii.add(r);
            } catch (NumberFormatException ex) {
                // a broken line in the config is skipped
            }
        }
        ruleWords = words.toArray(new String[0]);
        ruleRadii = new double[radii.size()];
        for (int i = 0; i < ruleRadii.length; i++) {
            ruleRadii[i] = radii.get(i);
        }
    }
}
