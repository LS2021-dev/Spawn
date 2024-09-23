package ferargent.spawn.listeners;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class ProtectionListener extends BukkitRunnable implements Listener {
    private final boolean protectSpawn;
    private final int spawnRadius;
    private final World world;

    private final List<Player> protectedPlayers = new ArrayList<>();

    private ProtectionListener(Plugin plugin, boolean protectSpawn, int spawnRadius, World world) {
        this.protectSpawn = protectSpawn;
        this.spawnRadius = spawnRadius;
        this.world = world;

        this.runTaskTimer(plugin, 0, 3);
    }

    public static ProtectionListener create(Plugin plugin) {
        var config = plugin.getConfig();
        if (!config.contains("protectSpawn") || !config.contains("spawnRadius") || !config.contains("world")) {
            plugin.saveResource("config.yml", true);
            plugin.reloadConfig();
        }
        return new ProtectionListener(plugin, config.getBoolean("protectSpawn"), config.getInt("spawnRadius"), Bukkit.getWorld(Objects.requireNonNull(config.getString("world"))));
    }

    @Override
    public void run() {
        world.getPlayers().forEach(player -> {
            if (isInSpawnRadius(player) && protectSpawn && player.getGameMode() == GameMode.SURVIVAL) {
                player.setGameMode(GameMode.ADVENTURE);
                protectedPlayers.add(player);
            } else if (!protectedPlayers.contains(player) && player.getGameMode() == GameMode.ADVENTURE && isInSpawnRadius(player)) {
                protectedPlayers.add(player);
            } else if (!isInSpawnRadius(player) && protectedPlayers.contains(player) && player.getGameMode() == GameMode.ADVENTURE) {
                player.setGameMode(GameMode.SURVIVAL);
                protectedPlayers.remove(player);
            }
        });
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && isInSpawnRadius(player) && protectSpawn) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteraction(PlayerInteractEvent event) {
        if (isInSpawnRadius(event.getPlayer()) && protectSpawn && event.getPlayer().getGameMode() == GameMode.ADVENTURE) {
            event.setCancelled(true);
        }
    }

    private boolean isInSpawnRadius(Player player) {
        return player.getLocation().distance(world.getSpawnLocation()) <= spawnRadius;
    }
}
