package com.mengcraft.playersql;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerQuitEvent;

public class Events implements Listener {

    private final SyncManager syncManager = SyncManager.DEFAULT;
    private final PlayerManager playerManager = PlayerManager.DEFAULT;
    private final PlayerZQL main;

    public Events(PlayerZQL main) {
        this.main = main;
    }

    @EventHandler
    public void handle(PlayerLoginEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (playerManager.isLocked(uuid)) {
            event.setResult(Result.KICK_OTHER);
            event.setKickMessage(PlayerManager.MESSAGE_KICK);
        } else if (event.getResult() == Result.ALLOWED) {
            playerManager.lock(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void handle(final PlayerJoinEvent event) {
        event.getPlayer().sendMessage(Configs.MSG_LOADING);
        Runnable task = new Runnable() {
            @Override
            public void run() {
                syncManager.load(event.getPlayer());
            }
        };
        main.scheduler().runTaskLater(main, task, 30);
    }

    @EventHandler
    public void handle(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!playerManager.isLocked(uuid)) {
            syncManager.save(player, true);
        }
        playerManager.unlock(uuid);
    }

    @EventHandler
    public void handle(EntityDamageEvent event) {
        UUID uuid = event.getEntity().getUniqueId();
        if (playerManager.isLocked(uuid)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void handle(PlayerDropItemEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (playerManager.isLocked(uuid)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void handle(PlayerInteractEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (playerManager.isLocked(uuid)) {
            event.setCancelled(true);
        }
    }

}
