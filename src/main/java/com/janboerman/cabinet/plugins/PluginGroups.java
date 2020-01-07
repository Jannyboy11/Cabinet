package com.janboerman.cabinet.plugins;

import com.janboerman.cabinet.api.Groups;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.Objects;

public abstract class PluginGroups implements Groups {

    private Plugin plugin;
    private final String name;
    protected final ProxyServer proxyServer;

    public PluginGroups(String name, ProxyServer proxyServer) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.proxyServer = Objects.requireNonNull(proxyServer, "proxy cannot be null");
    }

    protected Plugin getPlugin() {
        return plugin;
    }

    public abstract void initialise();

    @Override
    public void onDisable() {
    }

    @Override
    public boolean tryInitialise() {
        plugin = proxyServer.getPluginManager().getPlugin(name);
        if (plugin != null) {
            initialise();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getName() {
        return name + "Groups";
    }

}
