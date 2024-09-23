package ferargent.spawn;

import ferargent.spawn.listeners.ProtectionListener;
import ferargent.spawn.listeners.SpawnListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class Spawn extends JavaPlugin {

    private static Spawn plugin;

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        plugin = this;
        getServer().getPluginManager().registerEvents(SpawnListener.create(this), this);
        getServer().getPluginManager().registerEvents(ProtectionListener.create(this), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static Spawn getPlugin() {
        return plugin;
    }
}
