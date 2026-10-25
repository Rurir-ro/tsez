package com.smartzombies;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(SmartZombies.MODID)
public class SmartZombies {
    public static final String MODID = "smartzombies";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SmartZombies() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ZConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(new ZombieEvents());
        LOGGER.info("Smart Zombies loaded");
    }
}
