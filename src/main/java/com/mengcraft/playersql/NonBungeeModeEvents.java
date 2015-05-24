package com.mengcraft.playersql;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.mengcraft.playersql.SyncManager.State;
import com.mengcraft.playersql.task.LoadTask;

public class NonBungeeModeEvents implements Listener {
    
    private final PlayerZQL main;
    private final SyncManager syncManager = SyncManager.DEFAULT;
    private final PlayerManager playerManager = PlayerManager.DEFAULT;
    
    private ConcurrentHashMap<UUID, String> dataMap = new ConcurrentHashMap<>();

    public NonBungeeModeEvents(PlayerZQL main) {
        this.main = main;
    }
    
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPreLogin(AsyncPlayerPreLoginEvent e)
	{
	    
	    if(AsyncPlayerPreLoginEvent.Result.ALLOWED != e.getLoginResult())
	        return;
	    
	    UUID uuid = e.getUniqueId();
        State state = playerManager.getState(uuid);
        if (state != null && state != State.CONN_DONE)
        {
	           e.setLoginResult(Result.KICK_OTHER);
	           e.setKickMessage(PlayerManager.MESSAGE_KICK);
	           return;
	    }
        
        playerManager.setState(uuid, State.CONN_DONE);
	    
	    boolean success = new LoadTask(e.getUniqueId(), main).doLoad(this);
	    if(!success)
	    {
	        e.setLoginResult(Result.KICK_OTHER);
	        e.setKickMessage(PlayerManager.MESSAGE_KICK);
	        playerManager.setState(uuid, null);
	    }
	    
	}
	
	@EventHandler
	public void onPlayerJoin(final PlayerJoinEvent e)
	{
	    UUID uuid = e.getPlayer().getUniqueId();
	    this.playerManager.setState(uuid, State.JOIN_DONE);
	    playerManager.getDataMap().put(uuid, this.dataMap.get(uuid));
	    
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
	
	public void putData(UUID uuid, String data)
	{
	    this.dataMap.put(uuid, data);
	}
	
}
