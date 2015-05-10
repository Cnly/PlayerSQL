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
	    
	    if(this.playerManager.isLocked(e.getUniqueId()))
	    {
	           e.setLoginResult(Result.KICK_OTHER);
	           e.setKickMessage(PlayerManager.MESSAGE_KICK);
	           return;
	    }
	    
	    boolean success = new LoadTask(e.getUniqueId(), main).doLoad(this);
	    if(!success)
	    {
	        e.setLoginResult(Result.KICK_OTHER);
	        e.setKickMessage(PlayerManager.MESSAGE_KICK);
	    }
	    
	}
	
	@EventHandler
	public void onPlayerJoin(final PlayerJoinEvent e)
	{
	    
	    UUID uuid = e.getPlayer().getUniqueId();
	    this.playerManager.lock(uuid);
	    playerManager.getDataMap().put(uuid, this.dataMap.get(uuid));
	    
	}
	
    @EventHandler
    public void handle(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!playerManager.isLocked(uuid)) {
            syncManager.save(player, true);
        }
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
	
	public void putData(UUID uuid, String data)
	{
	    this.dataMap.put(uuid, data);
	}
	
}
