package com.mengcraft.playersql.task;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Queue;

import org.bukkit.entity.Player;

import com.mengcraft.playersql.PlayerManager;
import com.mengcraft.playersql.PlayerZQL;
import com.mengcraft.playersql.SwitchRequest;

public class SwitchServerTask implements Runnable {

    private final Queue<SwitchRequest> queue;
    private final PlayerZQL main;

    private final String channel = "BungeeCord";
    private final String connect = "Connect";

    public SwitchServerTask(PlayerZQL main) {
        this.main = main;
        this.queue = SwitchRequest.MANAGER.getQueue();
    }

    @Override
    public void run() {
        while (queue.size() != 0) {
            execute(queue.poll());
        }
    }

    private void execute(SwitchRequest poll) {
        Player player = main.getPlayer(poll.getPlayer());
        if (player != null) {
            player.sendPluginMessage(main, channel, message(poll));
            main.getServer().getScheduler().runTaskLater(main, () -> {
                PlayerManager.DEFAULT.setState(poll.getPlayer(), null);
            }, 10);
        }
    }

    private byte[] message(SwitchRequest poll) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (DataOutputStream stream = new DataOutputStream(out)) {
            stream.writeUTF(connect);
            stream.writeUTF(poll.getTarget());
        } catch (IOException e) {
            main.getLogger().warning(e.getMessage());
        }
        return out.toByteArray();
    }

    public void register() {
        main.getServer()
                .getMessenger()
                .registerOutgoingPluginChannel(main, "BungeeCord");
        main.getServer()
                .getScheduler()
                .runTaskTimer(main, this, 0, 0);
    }

}
