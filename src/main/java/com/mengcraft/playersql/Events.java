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

import com.mengcraft.playersql.SyncManager.State;

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
        State state = playerManager.getState(uuid);
        if (state != null && state != State.CONN_DONE) {
            event.setResult(Result.KICK_OTHER);
            event.setKickMessage(PlayerManager.MESSAGE_KICK);
        } else if (event.getResult() == Result.ALLOWED) {
            playerManager.setState(uuid, State.CONN_DONE);
        }
    }

    @EventHandler
    public void handle(final PlayerJoinEvent event) {
        event.getPlayer().sendMessage(Configs.MSG_LOADING);
        playerManager.setState(event.getPlayer().getUniqueId(), State.JOIN_WAIT);
        main.scheduler().runTaskLater(main, 
                ()-> syncManager.load(event.getPlayer()), 
                Configs.LOAD_DELAY);
    }

    @EventHandler
    public void handle(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (playerManager.getState(uuid) == null) {
            syncManager.save(player, true);
        }
    }

    @EventHandler
    public void handle(EntityDamageEvent event) {
        if(!(event.getEntity() instanceof Player)) return;
        UUID uuid = event.getEntity().getUniqueId();
        if (playerManager.getState(uuid) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void handle(PlayerDropItemEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (playerManager.getState(uuid) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void handle(PlayerInteractEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (playerManager.getState(uuid) != null) {
            event.setCancelled(true);
        }
    }

}
