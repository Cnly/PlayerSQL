package com.mengcraft.playersql;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerEvents implements Listener {

	private final TaskManager manager;
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPreLogin(AsyncPlayerPreLoginEvent e)
	{
	    
	    if(AsyncPlayerPreLoginEvent.Result.ALLOWED != e.getLoginResult())
	        return;
	    
	    manager.runLoadTask(e.getUniqueId());
	    
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
	    this.manager.runSaveTask(event.getPlayer());
	}

	public PlayerEvents() {
		this.manager = TaskManager.getManager();
	}

}
