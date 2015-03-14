package com.mengcraft.playersql.task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.avaje.ebeaninternal.server.lib.sql.DataSourceManager;
import com.avaje.ebeaninternal.server.lib.sql.DataSourcePool;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mengcraft.playersql.DataManager;
import com.mengcraft.playersql.PlayerSQL;
import com.mengcraft.playersql.RetryHandler;
import com.mengcraft.playersql.events.PlayerLoadedEvent;
import com.mengcraft.playersql.util.FixedExp;
import com.mengcraft.playersql.util.ItemUtil;

public class LoadPlayerTask implements Runnable {
    
    private static final Gson GSON = new Gson();
    
    private final PlayerSQL plugin;
    
	private final UUID uuid;

	private final DataSourcePool pool;

	private final RetryHandler retry;

	@Override
	public void run() {
		try {
		    
            do
            {
                
                Connection connection = this.pool.getConnection();
                PreparedStatement sql = connection
                        .prepareStatement("SELECT `Data`, `Online` FROM `PlayerData` WHERE `Player` = ?;");
                sql.setString(1, this.uuid.toString());
                ResultSet result = sql.executeQuery();
                if(!result.next())
                {
                    // Create record for new player.
                    PreparedStatement insert = connection
                            .prepareStatement("INSERT INTO `PlayerData`(`Player`, `Online`) VALUES(?, 1);");
                    insert.setString(1, this.uuid.toString());
                    insert.executeUpdate();
                    insert.close();
                    connection.commit();
                }
                else if(result.getInt(2) < 1)
                {
                    // Data unlocked. lock and read it.
                    PreparedStatement lock = connection
                            .prepareStatement("UPDATE `PlayerData` SET `Online` = 1 WHERE `Player` = ?;");
                    lock.setString(1, this.uuid.toString());
                    lock.executeUpdate();
                    connection.commit();
                    lock.close();
                    this.sync(this.uuid,
                            new JsonParser().parse(result.getString(1))
                                    .getAsJsonArray());
                }
                result.close();
                sql.close();
                connection.close();
                
            }
            while(this.retry.check(this.uuid));
		    
            // Data locked but reach max retry number.
		    throw new RuntimeException(String.format("Unable to fetch data for player(UUID) %s!", this.uuid));
		    
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

    private void sync(final UUID uuid, JsonArray value)
    {
        
        final double health = value.get(0).getAsDouble();
        final int foodLevel = value.get(1).getAsInt();
        final int exp = value.get(2).getAsInt();
        final ItemStack[] invContents = arrayToStacks(value.get(3).getAsJsonArray());
        final ItemStack[] armorContents = arrayToStacks(value.get(4).getAsJsonArray());
        final ItemStack[] chestContents = arrayToStacks(value.get(5).getAsJsonArray());
        final Collection<PotionEffect> effects = arrayToEffects(value.get(6).getAsJsonArray());
        final JsonObject customData = value.get(7).getAsJsonObject();
        
        // TODO maybe need caching configure
        
        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                Player player = Bukkit.getPlayer(uuid);
                if(null == player)
                    return;
                
                if(plugin.getConfig().getBoolean("sync.health"))
                {
                    player.setHealth(health);
                }
                if(plugin.getConfig().getBoolean("sync.food"))
                {
                    player.setFoodLevel(foodLevel);
                }
                if(plugin.getConfig().getBoolean("sync.exp"))
                {
                    FixedExp.setExp(player, exp);
                }
                if(plugin.getConfig().getBoolean("sync.inventory"))
                {
                    player.getInventory().setContents(invContents);
                    player.getInventory().setArmorContents(armorContents);
                }
                if(plugin.getConfig().getBoolean("sync.chest"))
                {
                    player.getEnderChest().setContents(chestContents);
                }
                if(plugin.getConfig().getBoolean("sync.potion"))
                {
                    for(PotionEffect effect : player.getActivePotionEffects())
                    {
                        player.removePotionEffect(effect.getType());
                    }
                    player.addPotionEffects(effects);
                }
                
                @SuppressWarnings("serial")
                PlayerLoadedEvent ple = new PlayerLoadedEvent(player, GSON.<HashMap<String, JsonElement>>fromJson(customData, new TypeToken<HashMap<String, JsonElement>>(){}.getType()));
                
                Bukkit.getPluginManager().callEvent(ple);
                
            }
        }.runTask(this.plugin);
        
    }
    
    private Collection<PotionEffect> arrayToEffects(JsonArray effects)
    {
        List<PotionEffect> effectList = new ArrayList<PotionEffect>();
        for(JsonElement element : effects)
        {
            JsonArray array = element.getAsJsonArray();
            String i = array.get(0).getAsString();
            int j = array.get(1).getAsInt();
            effectList.add(new PotionEffect(PotionEffectType.getByName(i), j,
                    array.get(2).getAsInt(), array.get(3).getAsBoolean()));
        }
        return effectList;
    }
    
    private ItemStack[] arrayToStacks(JsonArray array)
    {
        List<ItemStack> stackList = new ArrayList<ItemStack>();
        for(JsonElement element : array)
        {
            if(element.isJsonNull())
            {
                stackList.add(new ItemStack(Material.AIR));
            }
            else
            {
                stackList.add(ItemUtil.getUtil().getItemStack(
                        element.getAsString()));
            }
        }
        return stackList.toArray(new ItemStack[array.size()]);
    }

	public LoadPlayerTask(UUID uuid, PlayerSQL plugin) {
	    this.plugin = plugin;
		this.uuid = uuid;

		DataSourceManager manager = DataManager.getDefault().getHandle();
		this.pool = manager.getDataSource("default");

		this.retry = RetryHandler.getHandler();
	}

}
