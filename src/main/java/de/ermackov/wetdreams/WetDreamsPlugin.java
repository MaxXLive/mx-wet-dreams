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
        registerWithMxGuide();
    }

    private void registerWithMxGuide() {
        if (!getServer().getPluginManager().isPluginEnabled("MXGuide")) {
            getLogger().info("MXGuide not found - /help integration inactive.");
            return;
        }
        var reg = getServer().getServicesManager().getRegistration(de.ermackov.mxguide.api.MxGuideApi.class);
        if (reg == null) return;

        saveResource("mx-guide.yml", false);
        var cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
            new java.io.File(getDataFolder(), "mx-guide.yml"));

        var sec = cfg.getConfigurationSection("mxguide");
        if (sec == null || !sec.getBoolean("enabled", true)) return;

        reg.getProvider().registerEntry(new de.ermackov.mxguide.GuideEntry(
            sec.getString("id", getName()),
            sec.getString("name", getName()),
            sec.getString("description"),
            sec.getInt("priority", 50),
            sec.getStringList("message"),
            sec.getBoolean("silent", false)
        ));
        getLogger().info("Registered with MXGuide.");
    }
}
