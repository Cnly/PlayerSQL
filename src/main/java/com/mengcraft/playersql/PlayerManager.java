package com.mengcraft.playersql;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mengcraft.playersql.SyncManager.State;
import com.mengcraft.playersql.task.UnlockTask;

public class PlayerManager {

    public static final PlayerManager DEFAULT = new PlayerManager();
    /**
     * Used as a flag to signify a new player.
     */
    public static final String FLAG_EMPTY = new String();
    /**
     * Used as a flag that signifies a player's unlock on the database side,
     * and {@link UnlockTask} should remove the player locally with
     * {@link PlayerManager#unlock(UUID)}.</br>
     * This should only appear in some very infrequent cases.
     */
    public static final String FLAG_EXCEPTION = new String();
    public static final String MESSAGE_KICK;

    static {
        MESSAGE_KICK = "Your data is locked; login later.";
    }

    private final Map<UUID, State>  playerStateMap = new ConcurrentHashMap<>();
    private final Map<UUID, String> dataMap = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> timerSaveTaskIds = new HashMap<>();
    
    public void setState(UUID uuid, State s)
    {
        if(null != s)
        {
            this.playerStateMap.put(uuid, s);
        }
        else
        {
            this.playerStateMap.remove(uuid);
        }
    }
    
    public State getState(UUID uuid)
    {
        return this.playerStateMap.get(uuid);
    }

    public Map<UUID, String> getDataMap() {
        return dataMap;
    }

    /**
     * Gets an unmodifiable set of dataMap's entries
     * @return an unmodifiable set of dataMap's entries
     */
    public Set<Entry<UUID, String>> getDataEntries() {
        return Collections.unmodifiableSet(dataMap.entrySet());
    }

    public Map<UUID, Integer> getSaveTaskIdMap() {
        return timerSaveTaskIds;
    }

}
