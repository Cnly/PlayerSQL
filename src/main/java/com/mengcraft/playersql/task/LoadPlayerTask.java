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

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mengcraft.playersql.PlayerZQL;
import com.mengcraft.playersql.TaskManager;
import com.mengcraft.jdbc.ConnectionHandler;
import com.mengcraft.playersql.RetryHandler;
import com.mengcraft.playersql.events.DataLoadedEvent;
import com.mengcraft.playersql.events.NewPlayerLoginEvent;
import com.mengcraft.playersql.events.PlayerSynchronizedEvent;
import com.mengcraft.playersql.util.FixedExp;
import com.mengcraft.playersql.util.ItemUtil;

public class LoadPlayerTask implements Runnable {
    
    private static final Gson GSON = new Gson();
    
    private final PlayerZQL plugin;
    
	private final UUID uuid;

	private final RetryHandler retry;
	private final TaskManager tm = TaskManager.getManager();

	@Override
	public void run() {
		try {
		    
            while(true)
            {
                Connection connection = ConnectionHandler.getConnection("playersql");
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
                    
                    result.close();
                    sql.close();
                    connection.close();
                    
                    Bukkit.getScheduler().runTask(this.plugin, new Runnable(){
                        @Override
                        public void run()
                        {
                            NewPlayerLoginEvent nple = new NewPlayerLoginEvent(uuid);
                            Bukkit.getPluginManager().callEvent(nple);
                        }
                    });
                    
                    break;
                }
                else if(result.getInt(2) < 1)
                {
                    // Data unlocked. lock and read it.
                    PreparedStatement lock = connection
                            .prepareStatement("UPDATE `PlayerData` SET `Online` = 1 WHERE `Player` = ?;");
                    lock.setString(1, this.uuid.toString());
                    lock.executeUpdate();
                    lock.close();
                    this.sync(this.uuid,
                            new JsonParser().parse(result.getString(1))
                                    .getAsJsonArray());
                    
                    result.close();
                    sql.close();
                    connection.close();
                    
                    break;
                }
                else if(!this.retry.check(this.uuid))
                {
                    // Data locked but reach max retry number.
                    
                    // Force syncing
                    this.sync(this.uuid,
                            new JsonParser().parse(result.getString(1))
                                    .getAsJsonArray());
                    
                    plugin.getLogger().warning(String.format("data of player(UUID) %s is being forced to load due to max retries", this.uuid));
                    
                    result.close();
                    sql.close();
                    connection.close();
                    
                    break;
                }
            }
		    
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}

    @SuppressWarnings({"serial", "deprecation"})
    private void sync(final UUID uuid, JsonArray value)
    {
        final double health = value.get(0).getAsDouble();
        final int foodLevel = value.get(1).getAsInt();
        final int exp = value.get(2).getAsInt();
        final ItemStack[] invContents = arrayToStacks(value.get(3).getAsJsonArray());
        final ItemStack[] armorContents = arrayToStacks(value.get(4).getAsJsonArray());
        final ItemStack[] chestContents = arrayToStacks(value.get(5).getAsJsonArray());
        final Collection<PotionEffect> effects = arrayToEffects(value.get(6).getAsJsonArray());
        
        final JsonObject customData = value.size() >= 8 ? value.get(7).getAsJsonObject() : new JsonObject();
        
        // TODO maybe need caching configure
        
        LoadPlayerTask.this.plugin.setCustomData(uuid, GSON.<HashMap<String, JsonElement>>fromJson(customData, new TypeToken<HashMap<String, JsonElement>>(){}.getType()));
        Bukkit.getScheduler().runTask(this.plugin, new Runnable()
        {
            @Override
            public void run()
            {
                DataLoadedEvent dle = new DataLoadedEvent(uuid);
                Bukkit.getPluginManager().callEvent(dle);
            }
        });
        
        Runnable syncTask = new Runnable()
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
                
                PlayerSynchronizedEvent pse = new PlayerSynchronizedEvent(player);
                
                Bukkit.getPluginManager().callEvent(pse);
                
            }
        };
        
        // Run the task directly in case of reload
        if(null == Bukkit.getPlayer(uuid))
        {
            tm.putSyncTask(uuid, syncTask);
        }
        else
        {
            syncTask.run();
        }
        
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

	public LoadPlayerTask(UUID uuid, PlayerZQL plugin) {
	    this.plugin = plugin;
		this.uuid = uuid;
		this.retry = RetryHandler.getHandler();
	}

}
