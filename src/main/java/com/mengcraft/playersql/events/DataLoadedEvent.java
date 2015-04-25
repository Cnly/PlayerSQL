package com.mengcraft.playersql.events;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.google.gson.JsonElement;

public class DataLoadedEvent extends Event
{
    
    private static final HandlerList handlerList = new HandlerList();
    
    private final UUID uuid;
    private HashMap<String, JsonElement> data;
    
    public DataLoadedEvent(UUID uuid, HashMap<String, JsonElement> data)
    {
        this.uuid = uuid;
        this.data = data;
    }
    
    public UUID getUuid()
    {
        return this.uuid;
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
