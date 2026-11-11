package de.ermackov.wetdreams;

import org.bukkit.plugin.java.JavaPlugin;

public final class WetDreamsPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        boolean skipToMorning = getConfig().getBoolean("skip-to-morning", false);
        getServer().getPluginManager().registerEvents(new RainSleepListener(skipToMorning), this);
        getLogger().info(skipToMorning
                ? "Sleeping in rain skips to the next morning"
                : "Sleeping in rain only skips the rain");
        saveResource("mx-guide.yml", false);
        saveResource("mx-guide_de_DE.yml", false);
    }
}
