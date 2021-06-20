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

import com.google.gson.JsonObject;
import com.replaymod.replaystudio.lib.viaversion.api.ViaAPI;
import com.replaymod.replaystudio.lib.viaversion.api.ViaVersionConfig;
import com.replaymod.replaystudio.lib.viaversion.api.command.ViaCommandSender;
import com.replaymod.replaystudio.lib.viaversion.api.configuration.ConfigurationProvider;
import com.replaymod.replaystudio.lib.viaversion.api.platform.TaskId;
import com.replaymod.replaystudio.lib.viaversion.api.platform.ViaConnectionManager;
import com.replaymod.replaystudio.lib.viaversion.api.platform.ViaPlatform;

import java.io.File;
import java.util.UUID;
import java.util.logging.Logger;

public class CustomViaPlatform implements ViaPlatform {
    private CustomViaConfig config = new CustomViaConfig();

    @Override
    public Logger getLogger() {
        return Logger.getLogger(CustomViaPlatform.class.getName());
    }

    @Override
    public String getPlatformName() {
        return "ReplayStudio";
    }

    @Override
    public String getPlatformVersion() {
        return null;
    }

    @Override
    public String getPluginVersion() {
        return "1.0";
    }

    @Override
    public TaskId runAsync(Runnable runnable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TaskId runSync(Runnable runnable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TaskId runSync(Runnable runnable, Long aLong) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TaskId runRepeatingSync(Runnable runnable, Long aLong) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void cancelTask(TaskId taskId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ViaCommandSender[] getOnlinePlayers() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendMessage(UUID uuid, String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean kickPlayer(UUID uuid, String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPluginEnabled() {
        return true;
    }

    @Override
    public ViaAPI getApi() {
        return CustomViaAPI.INSTANCE.get();
    }

    @Override
    public ViaVersionConfig getConf() {
        return config;
    }

    @Override
    public ConfigurationProvider getConfigurationProvider() {
        throw new UnsupportedOperationException();
    }

    @Override
    public File getDataFolder() {
        return null;
    }

    @Override
    public void onReload() {}

    @Override
    public JsonObject getDump() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isOldClientsAllowed() {
        return false;
    }

    @Override
    public ViaConnectionManager getConnectionManager() {
        return null;
    }
}