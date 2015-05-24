package com.mengcraft.playersql;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mengcraft.jdbc.ConnectionFactory;
import com.mengcraft.jdbc.ConnectionHandler;
import com.mengcraft.jdbc.ConnectionManager;
import com.mengcraft.playersql.SyncManager.State;
import com.mengcraft.playersql.task.LoadTask;
import com.mengcraft.playersql.task.TimerCheckTask;

public class PlayerZQL extends JavaPlugin {
    
    private static PlayerZQL instance;
    
    private final HashMap<UUID, HashMap<String, JsonElement>> customData = new HashMap<>();
    
    @Override
    public void onEnable() {
        
        instance = this;
        
        saveResource("config.yml", false);
        ConnectionFactory factory = new ConnectionFactory(
                getConfig().getString("plugin.database"),
                getConfig().getString("plugin.username"),
                getConfig().getString("plugin.password"));
        ConnectionManager manager = ConnectionManager.DEFAULT;
        ConnectionHandler handler = manager.getHandler("playersql", factory);
        try {
            Connection connection = handler.getConnection();
            String sql = "CREATE TABLE IF NOT EXISTS PlayerData("
                    + "`Id` int NOT NULL AUTO_INCREMENT, "
                    + "`Player` text NULL, "
                    + "`Data` text NULL, "
                    + "`Online` int NULL, "
                    + "`Last` bigint NULL, "
                    + "PRIMARY KEY(`Id`));";
            Statement action = connection.createStatement();
            action.executeUpdate(sql);
            action.close();
            handler.release(connection);
            scheduler().runTask(this, new MetricsTask(this));
            scheduler().runTaskTimer(this, new TimerCheckTask(this), 0, 0);
            if(Configs.BUNGEE)
            {
                register(new Events(this), this);
            }
            else
            {
                register(new NonBungeeModeEvents(this), this);
            }
        } catch (Exception e) {
            getLogger().warning("Unable to connect to database.");
            setEnabled(false);
        }
        
        PlayerManager pm = PlayerManager.DEFAULT;
        for(Player p : getServer().getOnlinePlayers())
        {
            UUID uuid = p.getUniqueId();
            pm.setState(uuid, State.JOIN_WAIT);
            new LoadTask(uuid, this).run();
        }
        
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        
        SyncManager sm = SyncManager.DEFAULT;
        PlayerManager pm = PlayerManager.DEFAULT;
        List<Player> list = new ArrayList<>();
        for (Player p : getServer().getOnlinePlayers()) {
            UUID uuid = p.getUniqueId();
            if (pm.getState(uuid) == null) {
                list.add(p);
            }
        }
        if (list.size() > 0) {
            sm.blockingSave(list, true);
        }
        ConnectionManager.DEFAULT.shutdown();
    }
    
    /**
     * This method should be only used by PlayerZQL itself.<br/>
     * Using this method may override custom data from other plugins.
     */
    @Deprecated
    public void setCustomData(UUID uuid, HashMap<String, JsonElement> data)
    {
        this.customData.put(uuid, data);
    }
    
    /**
     * Sets custom data
     * @param uuid The UUID of the player
     * @param key The key
     * @param value The value
     * @return previous associated data if present
     */
    public JsonElement setCustomData(UUID uuid, String key, JsonElement value)
    {
        
        HashMap<String, JsonElement> singlePlayerData = this.customData.get(uuid);
        if(null == singlePlayerData)
        {
            singlePlayerData = new HashMap<>();
        }
        
        singlePlayerData.put(key, value);
        
        HashMap<String, JsonElement> previousSingleData = this.customData.put(uuid, singlePlayerData);
        if(null != previousSingleData)
        {
            return previousSingleData.get(key);
        }
        else
        {
            return null;
        }
        
    }
    
    /**
     * Gets the custom data associated with the given key
     * @param uuid The UUID of the player
     * @param key The key
     * @return Matching data
     */
    public JsonElement getCustomData(UUID uuid, String key)
    {
        
        HashMap<String, JsonElement> singlePlayerData = this.customData.get(uuid);
        if(null == singlePlayerData)
        {
            return null;
        }
        
        return singlePlayerData.get(key);
    }
    
    /**
     * This method should be only used by PlayerZQL itself.<br/>
     */
    @Deprecated
    @SuppressWarnings("serial")
    public JsonObject getAllCustomData(UUID uuid)
    {
        HashMap<String, JsonElement> singlePlayerMap = this.customData.get(uuid);
        if(null == singlePlayerMap)
        {
            return new JsonObject();
        }
        else
        {
            return (JsonObject)new Gson().toJsonTree(this.customData.get(uuid), new TypeToken<HashMap<String, JsonElement>>(){}.getType());
        }
    }
    
    public static PlayerZQL getInstance()
    {
        return instance;
    }

    public BukkitScheduler scheduler() {
        return getServer().getScheduler();
    }

    private void register(Listener listener, PlayerZQL main) {
        getServer().getPluginManager().registerEvents(listener, main);
    }

    public void info(String string) {
        getLogger().info(string);
    }

    public void warn(String string) {
        getLogger().warning(string);
    }

}
