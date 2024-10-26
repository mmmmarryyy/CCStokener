package com.randomappdev.bukkitstats;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import java.io.File;
import java.net.URL;
import java.net.URLEncoder;
import java.util.UUID;

public class CallHome {

    private static final File file = new File("plugins/stats/config.yml");

    private static final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

    public static void load(Plugin plugin) {
        if (!verifyConfig()) {
            return;
        }
        if (config.getBoolean("opt-out")) {
            return;
        }
        plugin.getServer().getScheduler().scheduleAsyncRepeatingTask(plugin, new CallTask(plugin, config.getString("hash")), 10L, 20L * 60L * 60);
        System.out.println("[" + plugin.getDescription().getName() + "] Stats are being kept for this plugin. To opt-out for any reason, check plugins/stats.");
    }

    private static Boolean verifyConfig() {
        config.addDefault("opt-out", false);
        config.addDefault("hash", UUID.randomUUID().toString());
        if (!file.exists() || config.get("hash", null) == null) {
            System.out.println("BukkitStats is initializing for the first time. To opt-out check plugins/stats");
            try {
                config.options().copyDefaults(true);
                config.save(file);
            } catch (Exception ex) {
                System.out.println("BukkitStats failed to save.");
                ex.printStackTrace();
            }
            return false;
        }
        return true;
    }
}

class CallTask implements Runnable {

    private Plugin plugin;

    private String hash;

    public CallTask(Plugin plugin, String hash) {
        this.plugin = plugin;
        this.hash = hash;
    }

    public void run() {
        try {
            postUrl();
        } catch (Exception ignored) {
            System.out.println("Could not call home.");
            ignored.printStackTrace();
        }
    }

    private void postUrl() throws Exception {
        String authors = "";
        for (String auth : plugin.getDescription().getAuthors()) {
            authors = authors + " " + auth;
        }
        authors = authors.trim();
        String url = String.format("http://bukkitstats.randomappdev.com/ping.aspx?snam=%s&sprt=%s&shsh=%s&sver=%s&spcnt=%s&pnam=%s&pmcla=%s&paut=%s&pweb=%s&pver=%s", URLEncoder.encode(plugin.getServer().getName(), "UTF-8"), plugin.getServer().getPort(), hash, URLEncoder.encode(Bukkit.getVersion(), "UTF-8"), plugin.getServer().getOnlinePlayers().length, URLEncoder.encode(plugin.getDescription().getName(), "UTF-8"), URLEncoder.encode(plugin.getDescription().getMain(), "UTF-8"), URLEncoder.encode(authors, "UTF-8"), URLEncoder.encode(plugin.getDescription().getWebsite(), "UTF-8"), URLEncoder.encode(plugin.getDescription().getVersion(), "UTF-8"));
        new URL(url).openConnection().getInputStream();
    }
}
