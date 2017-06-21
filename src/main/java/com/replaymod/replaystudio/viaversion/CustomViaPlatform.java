/*
 * This file is part of ReplayStudio, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016 johni0702 <https://github.com/johni0702>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.replaymod.replaystudio.viaversion;

import us.myles.ViaVersion.api.ViaAPI;
import us.myles.ViaVersion.api.ViaVersionConfig;
import us.myles.ViaVersion.api.command.ViaCommandSender;
import us.myles.ViaVersion.api.configuration.ConfigurationProvider;
import us.myles.ViaVersion.api.platform.TaskId;
import us.myles.ViaVersion.api.platform.ViaPlatform;
import us.myles.viaversion.libs.gson.JsonObject;

import java.util.UUID;
import java.util.logging.Logger;

public class CustomViaPlatform implements ViaPlatform {
	private CustomViaConfig config = new CustomViaConfig();

	@Override
	public Logger getLogger() {
		return null;
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
		return null;
	}

	@Override
	public TaskId runSync(Runnable runnable) {
		return null;
	}

	@Override
	public TaskId runRepeatingSync(Runnable runnable, Long aLong) {
		return null;
	}

	@Override
	public void cancelTask(TaskId taskId) {}

	@Override
	public ViaCommandSender[] getOnlinePlayers() {
		return new ViaCommandSender[0];
	}

	@Override
	public void sendMessage(UUID uuid, String s) {}

	@Override
	public boolean kickPlayer(UUID uuid, String s) {
		return false;
	}

	@Override
	public boolean isPluginEnabled() {
		return true;
	}

	@Override
	public ViaAPI getApi() {
		return null;
	}

	@Override
	public ViaVersionConfig getConf() {
		return config;
	}

	@Override
	public ConfigurationProvider getConfigurationProvider() {
		return null;
	}

	@Override
	public void onReload() {}

	@Override
	public JsonObject getDump() {
		return null;
	}

	@Override
	public boolean isOldClientsAllowed() {
		return false;
	}
}