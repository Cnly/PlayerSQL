package io.github.cnly.playerzql.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mengcraft.playersql.PlayerZQL;

public class PlayerSavingEvent extends Event
{
    
    private static final HandlerList handlerList = new HandlerList();
    
    private final PlayerZQL main = PlayerZQL.getInstance();
    private final Player player;
    
    public PlayerSavingEvent(Player player)
    {
        super();
        this.player = player;
    }
    
    /**
     * Sets custom data
     * @param key The key
     * @param value The value
     * @return previous associated data if present
     */
    public JsonElement setData(String key, JsonElement value)
    {
        return this.main.setCustomData(player.getUniqueId(), key, value);
    }
    
    /**
     * Gets the custom data associated with the given key
     * @param key The key
     * @return Matching data
     */
    public JsonElement getData(String key)
    {
        return this.main.getCustomData(player.getUniqueId(), key);
    }
    
    public Player getPlayer()
    {
        return player;
    }

    @SuppressWarnings("deprecation")
    public JsonObject getAllData()
    {
        return main.getAllCustomData(player.getUniqueId());
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
