package com.mengcraft.playersql.events;

import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class PlayerSavingEvent extends Event
{
    
    private static final HandlerList handlerList = new HandlerList();
    
    private final Player player;
    private final HashMap<String, JsonElement> data = new HashMap<>();
    
    public PlayerSavingEvent(Player player)
    {
        super();
        this.player = player;
    }
    
    public JsonElement setData(String key, JsonElement value)
    {
        return this.data.put(key, value);
    }
    
    public JsonElement getData(String key)
    {
        return this.data.get(key);
    }
    
    public Player getPlayer()
    {
        return player;
    }

    @SuppressWarnings("serial")
    public JsonObject getAllData()
    {
        return (JsonObject)new Gson().toJsonTree(this.data, new TypeToken<HashMap<String, JsonElement>>(){}.getType());
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
