package com.mengcraft.playersql;

import java.io.IOException;

import org.mcstats.Metrics;

public class MetricsTask implements Runnable {
    
    private final PlayerZQL main;

    public MetricsTask(PlayerZQL main) {
        this.main = main;
    }

    @Override
    public void run() {
        try {
            new Metrics(main).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
