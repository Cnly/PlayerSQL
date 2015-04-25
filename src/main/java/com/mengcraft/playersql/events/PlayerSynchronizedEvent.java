package com.mengcraft.playersql.events;

import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.google.gson.JsonElement;

public class PlayerSynchronizedEvent extends Event
{
    
    private static final HandlerList handlerList = new HandlerList();
    
    private final Player player;
    private HashMap<String, JsonElement> data;
    
    public PlayerSynchronizedEvent(Player player, HashMap<String, JsonElement> data)
    {
        this.player = player;
        this.data = data;
    }
    
    public Player getPlayer()
    {
        return this.player;
    }

    public JsonElement getData(String key)
    {
        return this.data.get(key);
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
