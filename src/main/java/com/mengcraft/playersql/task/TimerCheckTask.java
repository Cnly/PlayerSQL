package com.mengcraft.playersql.task;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import com.mengcraft.playersql.Configs;
import com.mengcraft.playersql.PlayerManager;
import com.mengcraft.playersql.PlayerZQL;
import com.mengcraft.playersql.SyncManager;
import com.mengcraft.playersql.events.NewPlayerJoinEvent;

public class TimerCheckTask implements Runnable {
    
    private static final boolean DEBUG = Configs.DEBUG;
    
    private final PlayerManager playerManager;
    private final SyncManager syncManager;
    private final PlayerZQL main;
    private final Server server;
    private final BukkitScheduler scheduler;

    public TimerCheckTask(PlayerZQL main) {
        this.playerManager = PlayerManager.DEFAULT;
        this.syncManager = SyncManager.DEFAULT;
        this.main = main;
        this.server = main.getServer();
        this.scheduler = server.getScheduler();
    }
    
    /**
     * A task run repeatedly in order to perform actions on players in the main thread.
     */
    @Override
    public void run() {
        Set<Entry<UUID, String>> dataEntries = playerManager.getDataEntries();
        for (Entry<UUID, String> e : dataEntries) {
            UUID uuid = e.getKey();
            String data = e.getValue();
            if (data == PlayerManager.FLAG_EMPTY) {
                setupNewPlayer(uuid);
            } else if (data == PlayerManager.FLAG_EXCEPTION) {
                // This is an infrequent case where a player is 'disappeared' after joining the server.
                playerManager.unlock(uuid);
            } else {
                setupPlayer(uuid, data);
            }
        }
    }

    private void setupNewPlayer(UUID uuid) {
        playerManager.unlock(uuid);
        scheduleTask(uuid);
        NewPlayerJoinEvent npje = new NewPlayerJoinEvent(Bukkit.getPlayer(uuid));
        Bukkit.getPluginManager().callEvent(npje);
        if (DEBUG) {
            main.info("#5 New player: " + uuid);
        }
    }

    private void setupPlayer(UUID uuid, String data) {
        Player p = server.getPlayer(uuid);
        syncManager.load(p, data);
        scheduleTask(uuid);
        if (DEBUG) {
            main.info("#1 Loaded data for " + uuid);
        }
    }

    private void scheduleTask(UUID uuid) {
        Map<UUID, Integer> task = playerManager.getSaveTaskIdMap();
        if (task.get(uuid) != null) {
            server.getScheduler().cancelTask(task.remove(uuid));
            if (DEBUG) {
                main.warn("#3 Cancelled existing timer task for " + uuid);
            }
        }
        Runnable runnable = new TimerSaveTask(server, uuid);
        int id = scheduleTask(runnable, 3600, 3600);
        playerManager.getSaveTaskIdMap().put(uuid, id);
        if (DEBUG) {
            main.info("#4 Ran a timer task for " + uuid);
        }
    }

    private int scheduleTask(Runnable runnable, int i, int j) {
        return scheduler.runTaskTimer(main, runnable, i, j).getTaskId();
    }

}
