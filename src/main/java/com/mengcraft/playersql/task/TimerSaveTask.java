package com.mengcraft.playersql.task;

import java.util.UUID;

import org.bukkit.Server;
import org.bukkit.entity.Player;

import com.mengcraft.playersql.PlayerManager;
import com.mengcraft.playersql.SyncManager;

public class TimerSaveTask implements Runnable {

    private final Server server;
    private final UUID uuid;

    public TimerSaveTask(Server server, UUID uuid) {
        this.server = server;
        this.uuid = uuid;
    }

    @Override
    public void run() {
        Player p = server.getPlayer(uuid);
        if (p != null && p.isOnline()) {
            SyncManager.DEFAULT.save(p, false);
        } else {
            int id = PlayerManager.DEFAULT.getSaveTaskIdMap().remove(uuid);
            server.getScheduler().cancelTask(id);
        }
    }

}
