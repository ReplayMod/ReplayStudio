/*
 * Copyright (c) 2021
 *
 * This file is part of ReplayStudio.
 *
 * ReplayStudio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ReplayStudio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ReplayStudio.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.replaymod.replaystudio.viaversion;

import com.replaymod.replaystudio.lib.viaversion.api.Via;
import com.replaymod.replaystudio.lib.viaversion.api.ViaManager;
import com.replaymod.replaystudio.lib.viaversion.api.command.ViaVersionCommand;
import com.replaymod.replaystudio.lib.viaversion.api.configuration.ConfigurationProvider;
import com.replaymod.replaystudio.lib.viaversion.api.connection.ConnectionManager;
import com.replaymod.replaystudio.lib.viaversion.api.debug.DebugHandler;
import com.replaymod.replaystudio.lib.viaversion.api.platform.ViaInjector;
import com.replaymod.replaystudio.lib.viaversion.api.platform.ViaPlatform;
import com.replaymod.replaystudio.lib.viaversion.api.platform.ViaPlatformLoader;
import com.replaymod.replaystudio.lib.viaversion.api.platform.providers.ViaProviders;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.ProtocolManager;
import com.replaymod.replaystudio.lib.viaversion.api.scheduler.Scheduler;
import com.replaymod.replaystudio.lib.viaversion.debug.DebugHandlerImpl;
import com.replaymod.replaystudio.lib.viaversion.protocol.ProtocolManagerImpl;

import java.util.HashSet;
import java.util.Set;

public class CustomViaManager implements ViaManager {
    public static synchronized void initialize() {
        // Exists only to trigger the static initializer
    }

    static {
        CustomViaManager manager = new CustomViaManager();
        Via.init(manager);
        manager.protocolManager.registerProtocols();
        manager.protocolManager.refreshVersions();
        manager.initialized = true;
    }

    private final ProtocolManagerImpl protocolManager = new ProtocolManagerImpl();
    private final ConnectionManager connectionManager = new CustomConnectionManager();
    private final ViaProviders providers = new ViaProviders();
    private final ViaPlatform<?> platform = new CustomViaPlatform();
    private final ViaInjector injector = new CustomViaInjector();
    private final Set<String> subPlatforms = new HashSet<>();
    private final DebugHandler debugHandler = new DebugHandlerImpl();
    private boolean initialized;

    private CustomViaManager() {
    }

    @Override
    public ProtocolManager getProtocolManager() {
        return protocolManager;
    }

    @Override
    public ViaPlatform<?> getPlatform() {
        return platform;
    }

    @Override
    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    @Override
    public ViaProviders getProviders() {
        return providers;
    }

    @Override
    public ViaInjector getInjector() {
        return injector;
    }

    @Override
    public ViaVersionCommand getCommandHandler() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ViaPlatformLoader getLoader() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Scheduler getScheduler() {
        return null;
    }

    @Override
    public ConfigurationProvider getConfigurationProvider() {
        return null;
    }

    @Override
    public DebugHandler debugHandler() {
        return debugHandler;
    }

    @Override
    public Set<String> getSubPlatforms() {
        return subPlatforms;
    }

    @Override
    public void addEnableListener(Runnable runnable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }
}