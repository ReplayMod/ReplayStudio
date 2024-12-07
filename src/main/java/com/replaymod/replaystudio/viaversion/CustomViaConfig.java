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

import com.replaymod.replaystudio.lib.viaversion.api.configuration.ViaVersionConfig;
import com.replaymod.replaystudio.lib.viaversion.api.minecraft.WorldIdentifiers;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.BlockedProtocolVersions;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;
import com.replaymod.replaystudio.lib.viaversion.libs.fastutil.ints.IntSet;
import com.replaymod.replaystudio.lib.viaversion.libs.gson.JsonElement;
import com.replaymod.replaystudio.lib.viaversion.protocol.BlockedProtocolVersionsImpl;

import java.util.Collections;
import java.util.Map;

// Configured as per recommendations at https://docs.viaversion.com/display/VIAVERSION/Configuration
public class CustomViaConfig implements ViaVersionConfig {
    @Override
    public boolean isCheckForUpdates() {
        return false;
    }

    @Override
    public void setCheckForUpdates(boolean b) {
    }

    @Override
    public boolean isPreventCollision() {
        return false;
    }

    @Override
    public boolean isNewEffectIndicator() {
        return false;
    }

    @Override
    public boolean isShowNewDeathMessages() {
        return false;
    }

    @Override
    public boolean isSuppressMetadataErrors() {
        return true;
    }

    @Override
    public boolean isShieldBlocking() {
        return false;
    }

    @Override
    public boolean isNoDelayShieldBlocking() {
        return false;
    }

    @Override
    public boolean isShowShieldWhenSwordInHand() {
        return false;
    }

    @Override
    public boolean isHologramPatch() {
        return true;
    }

    @Override
    public boolean isPistonAnimationPatch() {
        return false;
    }

    @Override
    public boolean isBossbarPatch() {
        return true;
    }

    @Override
    public boolean isBossbarAntiflicker() {
        return false;
    }

    @Override
    public double getHologramYOffset() {
        return -0.96;
    }

    @Override
    public boolean isAutoTeam() {
        return false;
    }

    @Override
    public int getMaxPPS() {
        return -1;
    }

    @Override
    public String getMaxPPSKickMessage() {
        return null;
    }

    @Override
    public int getTrackingPeriod() {
        return -1;
    }

    @Override
    public int getWarningPPS() {
        return -1;
    }

    @Override
    public int getMaxWarnings() {
        return -1;
    }

    @Override
    public String getMaxWarningsKickMessage() {
        return null;
    }

    @Override
    public boolean isSendSupportedVersions() {
        return false;
    }

    @Override
    public boolean isSimulatePlayerTick() {
        return false;
    }

    @Override
    public boolean isItemCache() {
        return false;
    }

    @Override
    public boolean isNMSPlayerTicking() {
        return false;
    }

    @Override
    public boolean isReplacePistons() {
        return false;
    }

    @Override
    public int getPistonReplacementId() {
        return -1;
    }

    @Override
    public boolean isChunkBorderFix() {
        return true;
    }

    @Override
    public boolean is1_12NBTArrayFix() {
        return true;
    }

    @Override
    public boolean is1_13TeamColourFix() {
        return true;
    }

    @Override
    public boolean shouldRegisterUserConnectionOnJoin() {
        return false;
    }

    @Override
    public boolean is1_12QuickMoveActionFix() {
        return false;
    }

    @Override
    public BlockedProtocolVersions blockedProtocolVersions() {
        return new BlockedProtocolVersionsImpl(Collections.emptySet(), ProtocolVersion.unknown, ProtocolVersion.unknown);
    }

    @Override
    public String getBlockedDisconnectMsg() {
        return null;
    }

    @Override
    public String getReloadDisconnectMsg() {
        return null;
    }

    @Override
    public boolean isSuppressConversionWarnings() {
        return false;
    }

    @Override
    public boolean isDisable1_13AutoComplete() {
        return false;
    }

    @Override
    public boolean isServersideBlockConnections() {
        return true;
    }

    @Override
    public String getBlockConnectionMethod() {
        return "packet";
    }

    @Override
    public boolean isReduceBlockStorageMemory() {
        return false;
    }

    @Override
    public boolean isStemWhenBlockAbove() {
        return true;
    }

    @Override
    public boolean isVineClimbFix() {
        return false;
    }

    @Override
    public boolean isSnowCollisionFix() {
        return false;
    }

    @Override
    public boolean isInfestedBlocksFix() {
        return false;
    }

    @Override
    public int get1_13TabCompleteDelay() {
        return 0;
    }

    @Override
    public boolean isTruncate1_14Books() {
        return false;
    }

    @Override
    public boolean isLeftHandedHandling() {
        return true;
    }

    @Override
    public boolean is1_9HitboxFix() {
        return true;
    }

    @Override
    public boolean is1_14HitboxFix() {
        return true;
    }

    @Override
    public boolean isNonFullBlockLightFix() {
        return false;
    }

    @Override
    public boolean is1_14HealthNaNFix() {
        return true;
    }

    @Override
    public boolean is1_15InstantRespawn() {
        return false;
    }

    @Override
    public boolean isIgnoreLong1_16ChannelNames() {
        return false;
    }

    @Override
    public boolean isForcedUse1_17ResourcePack() {
        return false;
    }

    @Override
    public JsonElement get1_17ResourcePackPrompt() {
        return null;
    }

    @Override
    public WorldIdentifiers get1_16WorldNamesMap() {
        return new WorldIdentifiers(WorldIdentifiers.OVERWORLD_DEFAULT);
    }

    @Override
    public boolean cache1_17Light() {
        return true;
    }

    @Override
    public boolean isArmorToggleFix() {
        return false;
    }

    @Override
    public boolean translateOcelotToCat() {
        return false;
    }

    @Override
    public boolean enforceSecureChat() {
        return false;
    }

    @Override
    public boolean handleInvalidItemCount() {
        return true;
    }

    @Override
    public boolean cancelBlockSounds() {
        return true;
    }

    @Override
    public boolean hideScoreboardNumbers() {
        return false;
    }

    @Override
    public boolean fix1_21PlacementRotation() {
        return false;
    }

    @Override
    public boolean swordBlockingViaConsumable() {
        return true;
    }

    @Override
    public void reload() {
    }

    @Override
    public void save() {
    }

    @Override
    public void set(String s, Object o) {
    }

    @Override
    public Map<String, Object> getValues() {
        return Collections.emptyMap();
    }
}
