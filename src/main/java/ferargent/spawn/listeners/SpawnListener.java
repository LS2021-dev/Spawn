package ferargent.spawn.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SpawnListener extends BukkitRunnable implements Listener {
    private final boolean boostEnabled;
    private final boolean fallDamage;
    private final int spawnRadius;
    private final int boostStrength;
    private final String boostMessage;
    private final World world;

    private final List<Player> flying = new ArrayList<>();
    private final List<Player> boosted = new ArrayList<>();

    private SpawnListener(Plugin plugin, boolean boostEnabled, boolean fallDamage, int spawnRadius, int boostStrength, String boostMessage, @Nullable World world) {
        this.boostEnabled = boostEnabled;
        this.fallDamage = fallDamage;
        this.spawnRadius = spawnRadius;
        this.boostStrength = boostStrength;
        this.boostMessage = boostMessage;
        this.world = world;


        this.runTaskTimer(plugin, 0, 3);
    }

    public static SpawnListener create(Plugin plugin) {
        var config = plugin.getConfig();
        if (!config.contains("boostEnabled") || !config.contains("fallDamage") || !config.contains("spawnRadius") || !config.contains("boostStrength") || !config.contains("world") || !config.contains("boostMessage")) {
            plugin.saveResource("config.yml", true);
            plugin.reloadConfig();
        }
        return new SpawnListener(plugin, config.getBoolean("boostEnabled"), config.getBoolean("fallDamage"), config.getInt("spawnRadius"), config.getInt("boostStrength"), config.getString("boostMessage"), Bukkit.getWorld(Objects.requireNonNull(config.getString("world"))));
    }

    @Override
    public void run() {
        world.getPlayers().forEach(player -> {
            if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) return;
            player.setAllowFlight(isInSpawnRadius(player));

            if (flying.contains(player) && player.isOnGround()) {
                player.setAllowFlight(false);
                player.setGliding(false);
                boosted.remove(player);
                flying.remove(player);
            }
        });
    }

    @EventHandler
    public void onDoubleJump(PlayerToggleFlightEvent event) {
        if (event.getPlayer().getGameMode() != GameMode.SURVIVAL && event.getPlayer().getGameMode() != GameMode.ADVENTURE)
            return;
        Player player = event.getPlayer();
        if (!isInSpawnRadius(player)) return;
        event.setCancelled(true);
        player.setGliding(true);
        flying.add(player);

        if (!boostEnabled) return;

        String[] messageParts = boostMessage.split("%key%");

        player.sendActionBar(Component.text(messageParts[0]).append(Component.keybind("key.swapOffhand").color(NamedTextColor.GOLD)).append(Component.text(messageParts[1])));
    }

    @EventHandler
    public void onToggleGlide(EntityToggleGlideEvent event) {
        if (event.getEntityType() == EntityType.PLAYER && flying.contains((Player) event.getEntity()))
            event.setCancelled(true);
    }

    @EventHandler
    void onDamage(EntityDamageEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) return;
        if (fallDamage && !isInSpawnRadius((Player) event.getEntity())) return;
        if (event.getEntityType() == EntityType.PLAYER && (event.getCause() == EntityDamageEvent.DamageCause.FALL || event.getCause() == EntityDamageEvent.DamageCause.FLY_INTO_WALL) && (flying.contains((Player) event.getEntity()) || isInSpawnRadius((Player) event.getEntity()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapItem(PlayerSwapHandItemsEvent event) {
        if (!boostEnabled || !flying.contains(event.getPlayer()) || boosted.contains(event.getPlayer())) return;
        event.setCancelled(true);
        boosted.add(event.getPlayer());
        event.getPlayer().setVelocity(event.getPlayer().getLocation().getDirection().multiply(boostStrength));
    }

    private boolean isInSpawnRadius(Player player) {
        if (player.getWorld() != world) return false;
        return player.getLocation().distance(world.getSpawnLocation()) <= spawnRadius;
    }
}
