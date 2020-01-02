package com.janboerman.cabinet.util;

import net.md_5.bungee.api.plugin.Plugin;

import java.util.concurrent.Executor;

public class Executors {

    public static Executor asyncExecutor(Plugin plugin) {
        return runnable -> plugin.getProxy().getScheduler().runAsync(plugin, runnable);
    }

}
