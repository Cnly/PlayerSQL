package com.mengcraft.playersql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.craftbukkit.v1_8_R2.inventory.CraftItemStack;
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
import com.mengcraft.playersql.events.DataLoadedEvent;
import com.mengcraft.playersql.events.PlayerSavingEvent;
import com.mengcraft.playersql.events.PlayerSynchronizedEvent;
import com.mengcraft.playersql.task.LoadTask;
import com.mengcraft.playersql.task.SaveTask;
import com.mengcraft.playersql.task.UnlockTask;
import com.mengcraft.playersql.util.ExpsUtil;
import com.mengcraft.playersql.util.ItemUtil;
import com.mengcraft.util.ArrayBuilder;

public class SyncManager {
    
    public static final SyncManager DEFAULT = new SyncManager();
    
    private static final Gson GSON = new Gson();
    
    private final PlayerZQL main = PlayerZQL.getInstance();
    private final ExecutorService service;
    private final JsonParser parser = new JsonParser();
    private final PlayerManager playerManager = PlayerManager.DEFAULT;
    private final Server server = Bukkit.getServer();

    private SyncManager() {
        this.service = new ThreadPoolExecutor(2, Integer.MAX_VALUE,
                60000,
                TimeUnit.MILLISECONDS,
                new SynchronousQueue<Runnable>()
                );
    }

    public void save(Player player, boolean unlock) {
        if (player == null) {
            throw new NullPointerException("#11 Can not save a null player.");
        }
        String data = getData(player);
        UUID uuid = player.getUniqueId();
        service.execute(new SaveTask(uuid, data, unlock));
    }

    public void save(List<Player> list, boolean unlock) {
        Map<UUID, String> map = new LinkedHashMap<>();
        for (Player p : list) {
            map.put(p.getUniqueId(), getData(p));
        }
        service.execute(new SaveTask(map, unlock));
    }
    
    /**
     * Used by the main thread to save all players on disable.
     */
    public void blockingSave(List<Player> list, boolean unlock) {
        save(list, unlock);
        service.shutdown();
        try
        {
            service.awaitTermination(1, TimeUnit.MINUTES);
        }
        catch(InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    public void load(Player player) {
        if (player == null) {
            throw new NullPointerException("Player can't be null!");
        } else if (!player.isOnline()) {
            // Player has gone
            playerManager.setState(player.getUniqueId(), null);
            return;
        } else {
            service.execute(new LoadTask(player.getUniqueId(), main));
        }
    }

    public void sync(UUID uuid, String value) {
        Player player = server.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            JsonArray array = parser.parse(value).getAsJsonArray();
            load(player, array);
            playerManager.setState(uuid, null);
            playerManager.getDataMap().remove(uuid);
        } else {
            /*
             * Player is null or offline here but the player's data on the
             * database has been locked. Perform an unlock task. This is an
             * infrequent case.
             */
            service.execute(new UnlockTask(uuid));
        }
    }

    public String getData(Player player) {
        JsonArray rootArray = new JsonArray();
        rootArray.add(GSON.toJsonTree(player.getHealth()));
        rootArray.add(GSON.toJsonTree(player.getFoodLevel()));
        rootArray.add(GSON.toJsonTree(ExpsUtil.UTIL.getExp(player)));
        rootArray.add(getItemArray(player.getInventory().getContents()));
        rootArray.add(getItemArray(player.getInventory().getArmorContents()));
        rootArray.add(getItemArray(player.getEnderChest().getContents()));
        rootArray.add(getEffectArray(player.getActivePotionEffects()));
        rootArray.add(GSON.toJsonTree(player.getInventory().getHeldItemSlot()));
        
        PlayerSavingEvent pse = new PlayerSavingEvent(player);
        Bukkit.getPluginManager().callEvent(pse);
        rootArray.add(pse.getAllData());
        
        return rootArray.toString();
    }

    @SuppressWarnings({"deprecation", "serial"})
    private void load(Player p, JsonArray array) {
        if (Configs.SYN_HEAL) {
            double j = array.get(0).getAsDouble();
            double d = j <= p.getMaxHealth() ?
                    j != 0 ? j : p.getHealth() :
                    p.getMaxHealth();
            p.setHealth(d);
        }
        if (Configs.SYN_FOOD) {
            p.setFoodLevel(array.get(1).getAsInt());
        }
        if (Configs.SYN_EXPS) {
            ExpsUtil.UTIL.setExp(p, array.get(2).getAsInt());
        }
        if (Configs.SYN_INVT) {
            ItemStack[] stacks = arrayToStacks(array.get(3).getAsJsonArray());
            ItemStack[] armors = arrayToStacks(array.get(4).getAsJsonArray());
            int hold = array.size() > 7 ?
                    array.get(7).getAsInt() :
                    4;
            p.getInventory().setContents(stacks);
            p.getInventory().setArmorContents(armors);
            p.getInventory().setHeldItemSlot(hold);
        }
        if (Configs.SYN_CEST) {
            ItemStack[] stacks = arrayToStacks(array.get(5).getAsJsonArray());
            p.getEnderChest().setContents(stacks);
        }
        if (Configs.SYN_EFCT) {
            for (PotionEffect effect : p.getActivePotionEffects()) {
                p.removePotionEffect(effect.getType());
            }
            JsonArray input = array.get(6).getAsJsonArray();
            Collection<PotionEffect> effects = arrayToEffects(input);
            p.addPotionEffects(effects);
        }
        
        final JsonObject customData = array.size() >= 9 ? array.get(8).getAsJsonObject() : new JsonObject();
        this.main.setCustomData(p.getUniqueId(), GSON.<HashMap<String, JsonElement>>fromJson(customData, new TypeToken<HashMap<String, JsonElement>>(){}.getType()));
        
        DataLoadedEvent dle = new DataLoadedEvent(p.getUniqueId());
        Bukkit.getPluginManager().callEvent(dle);
        PlayerSynchronizedEvent pse = new PlayerSynchronizedEvent(p);
        Bukkit.getPluginManager().callEvent(pse);
        
    }

    private Collection<PotionEffect> arrayToEffects(JsonArray effects) {
        List<PotionEffect> out = new ArrayList<PotionEffect>();
        for (JsonElement element : effects) {
            JsonArray array = element.getAsJsonArray();
            String i = array.get(0).getAsString();
            int j = array.get(1).getAsInt();
            PotionEffect effect = new PotionEffect(PotionEffectType.
                    getByName(i), j, array.get(2).getAsInt(),
                    array.get(3).getAsBoolean());
            out.add(effect);
        }
        return out;
    }

    private ItemStack[] arrayToStacks(JsonArray array) {
        ArrayBuilder<ItemStack> builder = new ArrayBuilder<>();
        for (JsonElement element : array) {
            if (element.isJsonNull()) {
                builder.append(new ItemStack(Material.AIR));
            } else {
                String data = element.getAsString();
                builder.append(ItemUtil.UTIL.getItemStack(data));
            }
        }
        return builder.build(ItemStack.class);
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
            singlePotionArray.add(GSON.toJsonTree(pe.isAmbient()));
            
            result.add(singlePotionArray);
            
        }
        
        return result;
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
                result.add(GSON.toJsonTree(ItemUtil.UTIL.getString(craftItem)));
            }
        }

        return result;
    }

    public enum State {
        CONN_DONE,
        JOIN_WAIT,
        JOIN_DONE,
    }

}
