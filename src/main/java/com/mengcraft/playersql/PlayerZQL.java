package com.mengcraft.playersql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mengcraft.playersql.commands.SendCommand;
import com.mengcraft.playersql.jdbc.ConnectionFactory;
import com.mengcraft.playersql.jdbc.ConnectionHandler;
import com.mengcraft.playersql.jdbc.ConnectionManager;
import com.mengcraft.playersql.lib.ExpUtil;
import com.mengcraft.playersql.lib.ExpUtilHandler;
import com.mengcraft.playersql.lib.ItemUtil;
import com.mengcraft.playersql.lib.ItemUtilHandler;
import com.mengcraft.playersql.lib.Metrics;
import com.mengcraft.playersql.SyncManager.State;
import com.mengcraft.playersql.task.LoadTask;
import com.mengcraft.playersql.task.SwitchServerTask;
import com.mengcraft.playersql.task.TimerCheckTask;

public class PlayerZQL extends JavaPlugin {
    
    private static PlayerZQL instance;
    
    public ItemUtil util;
    public ExpUtil exp;
    public SyncManager syncManager;
    
    private boolean enable;
    
    private final HashMap<UUID, HashMap<String, JsonElement>> customData = new HashMap<>();
    
    @Override
    public void onEnable() {
        
        instance = this;
        
        try {
            util = new ItemUtilHandler(this).handle();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        exp = new ExpUtilHandler(this).handle();
        syncManager = new SyncManager();
        
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
                    + "`Player` char(36) NULL, "
                    + "`Data` text NULL, "
                    + "`Online` int NULL, "
                    + "`Last` bigint NULL, "
                    + "PRIMARY KEY(`Id`), "
                    + "INDEX `player_index` (`Player`));";
            Statement action = connection.createStatement();
            action.executeUpdate(sql);
            action.close();
            handler.release(connection);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        new SendCommand().register(this);
        new TimerCheckTask(this).register();
        new SwitchServerTask(this).register();
        
        if(Configs.BUNGEE)
        {
            new PlayerListener(this).register();
        }
        else
        {
            new NonBungeeModePlayerListener(this).register();
        }
        
        PlayerManager pm = PlayerManager.DEFAULT;
        for(Player p : getServer().getOnlinePlayers())
        {
            UUID uuid = p.getUniqueId();
            pm.setState(uuid, State.JOIN_WAIT);
            new LoadTask(uuid, this).run();
        }
        
        try {
            new Metrics(this).start();
        } catch (IOException e) {
            getLogger().warning(e.toString());
        }
        
        enable = true;
        
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        if(enable)
        {
            PlayerManager pm = PlayerManager.DEFAULT;
            List<Player> list = new ArrayList<>();
            for (Player p : getServer().getOnlinePlayers()) {
                UUID uuid = p.getUniqueId();
                if (pm.getState(uuid) == null) {
                    list.add(p);
                }
            }
            if (list.size() > 0) {
                syncManager.blockingSave(list, true);
            }
            ConnectionManager.DEFAULT.shutdown();
        }
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

    public void info(String string) {
        getLogger().info(string);
    }

    public void warn(String string) {
        getLogger().warning(string);
    }
    
    public Player getPlayer(UUID uuid) {
        return getServer().getPlayer(uuid);
    }

}
