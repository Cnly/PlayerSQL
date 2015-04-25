package com.mengcraft.playersql;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.mengcraft.playersql.events.NewPlayerJoinEvent;
import com.mengcraft.playersql.task.LoadPlayerTask;

public class PlayerEvents implements Listener {
    
    private final PlayerZQL plugin = PlayerZQL.getInstance();
	private final TaskManager manager;
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPreLogin(AsyncPlayerPreLoginEvent e)
	{
	    
	    if(AsyncPlayerPreLoginEvent.Result.ALLOWED != e.getLoginResult())
	        return;
	    
	    new LoadPlayerTask(e.getUniqueId(), PlayerZQL.getInstance()).run();
	    
	}
	
	@EventHandler
	public void onPlayerJoin(final PlayerJoinEvent e)
	{
	    
	    boolean executed = this.manager.executeSyncingTask(e.getPlayer().getUniqueId());
	    if(executed)
	    {
            Bukkit.getScheduler().runTask(this.plugin, new Runnable(){
                @Override
                public void run()
                {
                    NewPlayerJoinEvent npje = new NewPlayerJoinEvent(e.getPlayer());
                    Bukkit.getPluginManager().callEvent(npje);
                }
            });
	    }
	    
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
	    this.manager.runSaveTask(event.getPlayer());
	}

	public PlayerEvents() {
		this.manager = TaskManager.getManager();
	}

}
