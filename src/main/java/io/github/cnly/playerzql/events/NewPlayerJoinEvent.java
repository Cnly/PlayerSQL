package io.github.cnly.playerzql.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * An event called when a new player, there wasn't data for whom in the
 * database, has joined the server.
 *
 */
public class NewPlayerJoinEvent extends Event
{
    
    private static final HandlerList handlerList = new HandlerList();
    
    private final Player player;
    
    public NewPlayerJoinEvent(Player player)
    {
        super();
        this.player = player;
    }

    public Player getPlayer()
    {
        return player;
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
