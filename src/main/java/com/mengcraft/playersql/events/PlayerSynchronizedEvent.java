package com.mengcraft.playersql.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.google.gson.JsonElement;
import com.mengcraft.playersql.PlayerZQL;

/**
 * An event called when PlayerZQL has just finished updating an in-game
 * player with the data from the database.<br/>
 * If the player is new to the server, which means there was no data in the
 * database, this event will not fire.
 *
 */
public class PlayerSynchronizedEvent extends Event
{
    
    private static final HandlerList handlerList = new HandlerList();
    
    private final PlayerZQL main = PlayerZQL.getInstance();
    private final Player player;
    
    public PlayerSynchronizedEvent(Player player)
    {
        this.player = player;
    }
    
    public Player getPlayer()
    {
        return this.player;
    }
    
    /**
     * Gets the custom data associated with the given key
     * @param key The key
     * @return Matching data
     */
    public JsonElement getData(String key)
    {
        return this.main.getCustomData(this.player.getUniqueId(), key);
    }
    
    @Override
    public HandlerList getHandlers()
    {
        return handlerList;
    }
    
    public static HandlerList getHandlerList()
    {
        return handlerList;
    }
    
}
