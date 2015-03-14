package com.mengcraft.playersql;

import java.sql.Connection;
import java.sql.Statement;

import org.bukkit.plugin.java.JavaPlugin;

import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebeaninternal.server.lib.sql.DataSourceManager;
import com.avaje.ebeaninternal.server.lib.sql.DataSourcePool;
import com.mengcraft.playersql.task.TimerSaveTask;

public class PlayerSQL extends JavaPlugin {

	private final DataSourceManager source = DataManager.getDefault().getHandle();

	@Override
	public void onEnable() {
	    new TaskManager(this);
		DataSourceConfig config = new DataSourceConfig();
		config.setDriver("com.mysql.jdbc.Driver");
		config.setUrl(getConfig().getString("plugin.database"));
		config.setUsername(getConfig().getString("plugin.username"));
		config.setPassword(getConfig().getString("plugin.password"));
		try {
			DataSourcePool pool = this.source.getDataSource("default", config);
			Connection connection = pool.getConnection();
			String table = "`Id` int NOT NULL AUTO_INCREMENT, `Player` text NULL, `Data` text NULL, `Online` int NULL, `Last` bigint NULL, PRIMARY KEY(`Id`)";
			String sql = "CREATE TABLE IF NOT EXISTS PlayerData(" + table + ");";
			Statement action = connection.createStatement();
			action.executeUpdate(sql);
			action.close();
			connection.close();
			registerEvents();
		} catch (Exception e) {
			getLogger().warning("Unable to connect to database.");
			e.printStackTrace();
			getLogger().warning("Shutting down server...");
			setEnabled(false);
			getServer().shutdown();
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

	private void registerEvents() {
		getServer().getPluginManager().registerEvents(new PlayerEvents(), this);
		getServer().getScheduler().runTaskTimer(this, new TimerSaveTask(this), 1200, 1200);
	}

	public PlayerSQL() {
		saveDefaultConfig();
	}

}
