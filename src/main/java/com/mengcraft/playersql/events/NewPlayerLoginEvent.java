package com.mengcraft.playersql.events;

import java.util.UUID;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * An event called when a new player, there wasn't data for whom in the
 * database, has logged in to the server and just been added to the database by
 * PlayerZQL.
 *
 */
public class NewPlayerLoginEvent extends Event
{
    
    private static final HandlerList handlerList = new HandlerList();
    
    private final UUID uuid;
    
    public NewPlayerLoginEvent(UUID uuid)
    {
        super();
        this.uuid = uuid;
    }

    public UUID getUuid()
    {
        return uuid;
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
