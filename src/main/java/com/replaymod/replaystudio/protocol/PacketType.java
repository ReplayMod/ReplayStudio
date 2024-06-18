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
package com.replaymod.replaystudio.protocol;

import com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.State;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;

// Note: Intentionally non-exhaustive (except 1.7.6 PLAY). Only contains what our filters need.
public enum PacketType {
	UnknownLogin(ProtocolVersion.v1_7_6, -1, State.LOGIN), // cause you can't switch over null
	UnknownConfiguration(ProtocolVersion.v1_20_2, -1, State.CONFIGURATION), // cause you can't switch over null
	UnknownPlay(ProtocolVersion.v1_7_6, -1), // cause you can't switch over null
	LoginSuccess(ProtocolVersion.v1_7_6, 0x02, State.LOGIN),
	ConfigCustomPayload(ProtocolVersion.v1_20_2, 0x00, State.CONFIGURATION),
	ConfigDisconnect(ProtocolVersion.v1_20_2, 0x01, State.CONFIGURATION),
	ConfigFinish(ProtocolVersion.v1_20_2, 0x02, State.CONFIGURATION),
	ConfigKeepAlive(ProtocolVersion.v1_20_2, 0x03, State.CONFIGURATION),
	ConfigPing(ProtocolVersion.v1_20_2, 0x04, State.CONFIGURATION),
	ConfigRegistries(ProtocolVersion.v1_20_2, 0x05, State.CONFIGURATION),
	ConfigFeatures(ProtocolVersion.v1_20_2, 0x07, State.CONFIGURATION),
	ConfigTags(ProtocolVersion.v1_20_2, 0x08, State.CONFIGURATION),
	ConfigSelectKnownPacks(ProtocolVersion.v1_20_5, 0x0e, State.CONFIGURATION),
    KeepAlive(ProtocolVersion.v1_7_6, 0x00),
	JoinGame(ProtocolVersion.v1_7_6, 0x01),
	Chat(ProtocolVersion.v1_7_6, 0x02),
	UpdateTime(ProtocolVersion.v1_7_6, 0x03),
	EntityEquipment(ProtocolVersion.v1_7_6, 0x04),
	SpawnPosition(ProtocolVersion.v1_7_6, 0x05),
	UpdateHealth(ProtocolVersion.v1_7_6, 0x06),
	Respawn(ProtocolVersion.v1_7_6, 0x07),
	PlayerPositionRotation(ProtocolVersion.v1_7_6, 0x08),
	ChangeHeldItem(ProtocolVersion.v1_7_6, 0x09),
	PlayerUseBed(ProtocolVersion.v1_7_6, 0x0a), // removed in 1.14
	EntityAnimation(ProtocolVersion.v1_7_6, 0x0b),
	SpawnPlayer(ProtocolVersion.v1_7_6, 0x0c), // removed in 1.20.2 in favor of SpawnObject
	EntityCollectItem(ProtocolVersion.v1_7_6, 0x0d),
	SpawnObject(ProtocolVersion.v1_7_6, 0x0e),
	SpawnMob(ProtocolVersion.v1_7_6, 0x0f), // removed in 1.19 in favor of SpawnObject
	SpawnPainting(ProtocolVersion.v1_7_6, 0x10),
	SpawnExpOrb(ProtocolVersion.v1_7_6, 0x11),
	EntityVelocity(ProtocolVersion.v1_7_6, 0x12),
	DestroyEntities(ProtocolVersion.v1_7_6, 0x13), // removed in 1.17, re-added in 1.17.1
	EntityMovement(ProtocolVersion.v1_7_6, 0x14), // removed in 1.17
	EntityPosition(ProtocolVersion.v1_7_6, 0x15),
	EntityRotation(ProtocolVersion.v1_7_6, 0x16),
	EntityPositionRotation(ProtocolVersion.v1_7_6, 0x17),
	EntityTeleport(ProtocolVersion.v1_7_6, 0x18),
	EntityHeadLook(ProtocolVersion.v1_7_6, 0x19),
	EntityStatus(ProtocolVersion.v1_7_6, 0x1a),
	EntityAttach(ProtocolVersion.v1_7_6, 0x1b),
	EntityMetadata(ProtocolVersion.v1_7_6, 0x1c),
	EntityEffect(ProtocolVersion.v1_7_6, 0x1d),
	EntityRemoveEffect(ProtocolVersion.v1_7_6, 0x1e),
	SetExperience(ProtocolVersion.v1_7_6, 0x1f),
	EntityProperties(ProtocolVersion.v1_7_6, 0x20),
	ChunkData(ProtocolVersion.v1_7_6, 0x21),
	MultiBlockChange(ProtocolVersion.v1_7_6, 0x22),
	BlockChange(ProtocolVersion.v1_7_6, 0x23),
	BlockValue(ProtocolVersion.v1_7_6, 0x24),
	BlockBreakAnim(ProtocolVersion.v1_7_6, 0x25),
	BulkChunkData(ProtocolVersion.v1_7_6, 0x26),
	Explosion(ProtocolVersion.v1_7_6, 0x27),
	PlayEffect(ProtocolVersion.v1_7_6, 0x28),
	PlaySound(ProtocolVersion.v1_7_6, 0x29),
	SpawnParticle(ProtocolVersion.v1_7_6, 0x2a),
	NotifyClient(ProtocolVersion.v1_7_6, 0x2b),
	SpawnGlobalEntity(ProtocolVersion.v1_7_6, 0x2c),
	OpenWindow(ProtocolVersion.v1_7_6, 0x2d),
	CloseWindow(ProtocolVersion.v1_7_6, 0x2e),
	SetSlot(ProtocolVersion.v1_7_6, 0x2f),
	WindowItems(ProtocolVersion.v1_7_6, 0x30),
	WindowProperty(ProtocolVersion.v1_7_6, 0x31),
	ConfirmTransaction(ProtocolVersion.v1_7_6, 0x32),
	UpdateSign(ProtocolVersion.v1_7_6, 0x33),
	MapData(ProtocolVersion.v1_7_6, 0x34),
	UpdateTileEntity(ProtocolVersion.v1_7_6, 0x35),
	OpenTileEntityEditor(ProtocolVersion.v1_7_6, 0x36),
	Statistics(ProtocolVersion.v1_7_6, 0x37),
	PlayerListEntry(ProtocolVersion.v1_7_6, 0x38),
	PlayerAbilities(ProtocolVersion.v1_7_6, 0x39),
	TabComplete(ProtocolVersion.v1_7_6, 0x3a),
	ScoreboardObjective(ProtocolVersion.v1_7_6, 0x3b),
	UpdateScore(ProtocolVersion.v1_7_6, 0x3c),
	DisplayScoreboard(ProtocolVersion.v1_7_6, 0x3d),
	Team(ProtocolVersion.v1_7_6, 0x3e),
	PluginMessage(ProtocolVersion.v1_7_6, 0x3f),
	Disconnect(ProtocolVersion.v1_7_6, 0x40),

	Difficulty(ProtocolVersion.v1_8, 0x41),
	Combat(ProtocolVersion.v1_8, 0x42), // removed in 1.17
	SwitchCamera(ProtocolVersion.v1_8, 0x43),
	WorldBorder(ProtocolVersion.v1_8, 0x44),
	EntityNBTUpdate(ProtocolVersion.v1_8, 0x49), // removed in 1.9

	UnloadChunk(ProtocolVersion.v1_9, 0x1d),
	SetPassengers(ProtocolVersion.v1_9, 0x40),

    OpenHorseWindow(ProtocolVersion.v1_14, 0x1f),
	UpdateLight(ProtocolVersion.v1_14, 0x24),
	TradeList(ProtocolVersion.v1_14, 0x27),
	UpdateViewPosition(ProtocolVersion.v1_14, 0x40),
	UpdateViewDistance(ProtocolVersion.v1_14, 0x41),
    EntitySoundEffect(ProtocolVersion.v1_14, 0x50),
    Tags(ProtocolVersion.v1_14, 0x5b),
	PlayerActionAck(ProtocolVersion.v1_14, 0x5c),

    CombatEnd(ProtocolVersion.v1_17, 0x33),
    CombatEnter(ProtocolVersion.v1_17, 0x34),
    CombatEntityDead(ProtocolVersion.v1_17, 0x35),
	DestroyEntity(ProtocolVersion.v1_17, 0x3a), // removed in 1.17.1, that didn't last long

	UpdateSimulationDistance(ProtocolVersion.v1_18, 0x57),

	Features(ProtocolVersion.v1_19_3, 0x67), // removed in 1.20.2 in favor of ConfigFeatures
	PlayerListEntryRemove(ProtocolVersion.v1_19_3, 0x35),

	Bundle(ProtocolVersion.v1_19_4, 0x00),

	Reconfigure(ProtocolVersion.v1_20_2, 0x65),
	;

    private final State state;
    private final ProtocolVersion initialVersion;
    private final int initialId;

	PacketType(ProtocolVersion initialVersion, int initialId) {
		this(initialVersion, initialId, State.PLAY);
    }

	PacketType(ProtocolVersion initialVersion, int initialId, State state) {
		this.state = state;
        this.initialVersion = initialVersion;
        this.initialId = initialId;
    }

	public State getState() {
		return state;
	}

	public ProtocolVersion getInitialVersion() {
        return initialVersion;
    }

    public int getInitialId() {
        return initialId;
    }

    public boolean isUnknown() {
		return initialId == -1;
	}
}
