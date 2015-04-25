package com.mengcraft.playersql;

import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mengcraft.jdbc.ConnectionFactory;
import com.mengcraft.jdbc.ConnectionHandler;
import com.mengcraft.playersql.task.LoadPlayerTask;
import com.mengcraft.playersql.task.TimerSaveTask;

public class PlayerZQL extends JavaPlugin {
    
    private static PlayerZQL instance;
    
    private final HashMap<UUID, HashMap<String, JsonElement>> customData = new HashMap<>();

	@Override
	public void onEnable() {
	    
	    instance = this;
	    
	    new TaskManager(this);
		saveDefaultConfig();
		
		ConnectionFactory factory = new ConnectionFactory(
				getConfig().getString("plugin.database"),
				getConfig().getString("plugin.username"),
				getConfig().getString("plugin.password"));
		ConnectionHandler handler = new ConnectionHandler(factory, "playersql");
		
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
			getServer().getPluginManager().registerEvents(new PlayerEvents(), this);
			getServer().getScheduler().runTaskTimer(this, new TimerSaveTask(this), 6000, 6000);

		} catch (Exception e) {
			getLogger().warning("Unable to connect to database.");
			e.printStackTrace();
			getLogger().warning("Shutting down server...");
			setEnabled(false);
			getServer().shutdown();
		}
		
		for(Player p : Bukkit.getOnlinePlayers())
		{
		    new LoadPlayerTask(p.getUniqueId(), PlayerZQL.getInstance()).run();
		}
		
	}

	@Override
	public void onDisable() {
		try {
			TaskManager.getManager().runSaveAll(this, 0);
		} catch (Exception e) {
			getLogger().warning("Unable to connect to database.");
		    e.printStackTrace();
			getLogger().warning("Can not link to database.");
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
        return (JsonObject)new Gson().toJsonTree(this.customData.get(uuid), new TypeToken<HashMap<String, JsonElement>>(){}.getType());
    }

    public static PlayerZQL getInstance()
    {
        return instance;
    }

}
