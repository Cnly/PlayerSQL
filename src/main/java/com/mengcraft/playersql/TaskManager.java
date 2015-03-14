package com.mengcraft.playersql;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R2.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.mengcraft.playersql.events.PlayerSavingEvent;
import com.mengcraft.playersql.task.LoadPlayerTask;
import com.mengcraft.playersql.task.SavePlayerTask;
import com.mengcraft.playersql.util.FixedExp;
import com.mengcraft.playersql.util.ItemUtil;

public class TaskManager {
    
    private static final Gson GSON = new Gson();
    
	private static TaskManager manager;
	private final ExecutorService pool = Executors.newCachedThreadPool();
	private final ItemUtil util = ItemUtil.getUtil();
	private final PlayerSQL plugin;
	
	public void runLoadTask(UUID uuid) {
		this.pool.execute(new LoadPlayerTask(uuid, this.plugin));
	}

	public void runSaveTask(Player player) {
		this.pool.execute(new SavePlayerTask(player.getUniqueId(), getData(player)));
	}

	public void runSaveAll(Plugin plugin, int quit) {
		Map<UUID, String> map = new HashMap<>();
		for (Player player : plugin.getServer().getOnlinePlayers()) {
			map.put(player.getUniqueId(), getData(player));
		}
		if (map.size() > 0) {
			this.pool.execute(new SavePlayerTask(map, quit));
		}
	}

	public String getData(Player player) {
	    
	    JsonArray rootArray = new JsonArray();
	    rootArray.add(GSON.toJsonTree(player.getHealth()));
	    rootArray.add(GSON.toJsonTree(player.getFoodLevel()));
	    rootArray.add(GSON.toJsonTree(FixedExp.getExp(player)));
	    rootArray.add(getItemArray(player.getInventory().getContents()));
	    rootArray.add(getItemArray(player.getInventory().getArmorContents()));
	    rootArray.add(getItemArray(player.getEnderChest().getContents()));
	    rootArray.add(getEffectArray(player.getActivePotionEffects()));
	    
	    PlayerSavingEvent pse = new PlayerSavingEvent(player);
	    Bukkit.getPluginManager().callEvent(pse);
	    rootArray.add(pse.getAllData());
	    
	    return rootArray.toString();
	}
	
    private JsonArray getItemArray(ItemStack[] stacks)
    {
        JsonArray result = new JsonArray();
        
        for(ItemStack item : stacks)
        {
            
            if(null == item || Material.AIR == item.getType())
                result.add(null);
            else
            {
                CraftItemStack craftItem = CraftItemStack.asCraftCopy(item);
                result.add(GSON.toJsonTree(this.util.getString(craftItem)));
            }
        }

	    return result;
	}
    
    private JsonArray getEffectArray(Collection<PotionEffect> effects)
    {
        
        JsonArray result = new JsonArray();
        
        for(PotionEffect pe : effects)
        {
            
            JsonArray singlePotionArray = new JsonArray();
            singlePotionArray.add(GSON.toJsonTree(pe.getType().getName()));
            singlePotionArray.add(GSON.toJsonTree(pe.getDuration()));
            singlePotionArray.add(GSON.toJsonTree(pe.getAmplifier()));
            
            result.add(singlePotionArray);
            
        }
        
        return result;
    }

	public static TaskManager getManager() {
		return manager;
	}

    public TaskManager(PlayerSQL plugin)
    {
        this.plugin = plugin;
        manager = this;
    }
	
}
