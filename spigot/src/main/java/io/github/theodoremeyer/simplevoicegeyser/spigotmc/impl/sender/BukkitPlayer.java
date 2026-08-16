package io.github.theodoremeyer.simplevoicegeyser.spigotmc.impl.sender;

import io.github.theodoremeyer.simplevoicegeyser.core.SvgCore;
import io.github.theodoremeyer.simplevoicegeyser.core.api.sender.SvgPlayer;
import io.github.theodoremeyer.simplevoicegeyser.spigotmc.SvgPlugin;
import io.github.theodoremeyer.simplevoicegeyser.spigotmc.impl.FoliaCompat;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BukkitPlayer extends SvgPlayer {

    private final Player player;

    public BukkitPlayer(Player player) {
        this.player = player;
    }

    @Override
    public UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Override
    public String getName() {
        return player.getName();
    }

    @Override
    public boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }

    @Override
    public void chat(String message) {
        SvgPlugin plugin = (SvgPlugin) SvgCore.getPlatform();
        FoliaCompat.runAtEntity(plugin, player, () -> player.chat(message));
    }

    @Override
    public boolean isOnline() {
        return player.isOnline();
    }

    @Override
    public Object getPlayer() {
        return player;
    }

    @Override
    public void sendMessage(String message) {
        runOnMainThread(() -> player.sendMessage(translate(message)));
    }

    private void runOnMainThread(Runnable task) {
        FoliaCompat.runAtEntity(SvgPlugin.getPlugin(SvgPlugin.class), player, task);
    }

    private String translate(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
