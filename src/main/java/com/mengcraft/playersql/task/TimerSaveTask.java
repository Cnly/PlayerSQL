package com.mengcraft.playersql.task;

import java.util.UUID;

import org.bukkit.Server;
import org.bukkit.entity.Player;

import com.mengcraft.playersql.PlayerManager;
import com.mengcraft.playersql.PlayerZQL;
import com.mengcraft.playersql.SyncManager;

public class TimerSaveTask implements Runnable {

    private final Server server;
    private final UUID uuid;
    private final SyncManager manager;

    public TimerSaveTask(PlayerZQL main, UUID uuid) {
        this.server = main.getServer();
        this.uuid = uuid;
        this.manager = main.syncManager;
    }

    @Override
    public void run() {
        Player p = server.getPlayer(uuid);
        if (p != null && p.isOnline()) {
            manager.save(p, false);
        } else {
            int id = PlayerManager.DEFAULT.getSaveTaskIdMap().remove(uuid);
            server.getScheduler().cancelTask(id);
        }
    }

}
