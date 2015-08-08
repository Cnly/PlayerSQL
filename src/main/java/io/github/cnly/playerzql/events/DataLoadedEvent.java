package io.github.cnly.playerzql.events;

import java.util.UUID;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.google.gson.JsonElement;
import com.mengcraft.playersql.PlayerZQL;

/**
 * An event called when PlayerZQL has just retrieved a player's data from the
 * database.<br/>
 * If the player is new to the server, which means there was no data in the
 * database, this event will not fire.
 *
 */
public class DataLoadedEvent extends Event
{
    
    private static final HandlerList handlerList = new HandlerList();
    
    private final PlayerZQL main = PlayerZQL.getInstance();
    private final UUID uuid;
    
    public DataLoadedEvent(UUID uuid)
    {
        this.uuid = uuid;
    }
    
    public UUID getUuid()
    {
        return this.uuid;
    }
    
    /**
     * Gets the custom data associated with the given key
     * @param key The key
     * @return Matching data
     */
    public JsonElement getData(String key)
    {
        return this.main.getCustomData(uuid, key);
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
