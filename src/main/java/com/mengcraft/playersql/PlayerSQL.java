package com.mengcraft.playersql;

import java.sql.Connection;
import java.sql.Statement;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.mengcraft.jdbc.ConnectionFactory;
import com.mengcraft.jdbc.ConnectionHandler;
import com.mengcraft.playersql.task.LoadPlayerTask;
import com.mengcraft.playersql.task.TimerSaveTask;

public class PlayerSQL extends JavaPlugin {
    
    private static PlayerSQL instance;

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
		    new LoadPlayerTask(p.getUniqueId(), PlayerSQL.getInstance()).run();
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

    public static PlayerSQL getInstance()
    {
        return instance;
    }

}
