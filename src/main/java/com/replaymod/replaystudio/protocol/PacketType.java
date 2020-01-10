/*
 * This file is part of ReplayStudio, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2020 johni0702 <https://github.com/johni0702>
 * Copyright (c) ReplayStudio contributors (see git)
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
package com.replaymod.replaystudio.protocol;

import com.replaymod.replaystudio.us.myles.ViaVersion.api.protocol.ProtocolVersion;
import com.replaymod.replaystudio.us.myles.ViaVersion.packets.State;

// Note: Intentionally non-exhaustive (except 1.7.6 PLAY). Only contains what our filters need.
public enum PacketType {
	UnknownLogin(ProtocolVersion.v1_7_6, -1, State.LOGIN), // cause you can't switch over null
	UnknownPlay(ProtocolVersion.v1_7_6, -1), // cause you can't switch over null
	LoginSuccess(ProtocolVersion.v1_7_6, 0x02, State.LOGIN),
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
	SpawnPlayer(ProtocolVersion.v1_7_6, 0x0c),
	EntityCollectItem(ProtocolVersion.v1_7_6, 0x0d),
	SpawnObject(ProtocolVersion.v1_7_6, 0x0e),
	SpawnMob(ProtocolVersion.v1_7_6, 0x0f),
	SpawnPainting(ProtocolVersion.v1_7_6, 0x10),
	SpawnExpOrb(ProtocolVersion.v1_7_6, 0x11),
	EntityVelocity(ProtocolVersion.v1_7_6, 0x12),
	DestroyEntities(ProtocolVersion.v1_7_6, 0x13),
	EntityMovement(ProtocolVersion.v1_7_6, 0x14),
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
	Combat(ProtocolVersion.v1_8, 0x42),
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
	PlayerActionAck(ProtocolVersion.v1_14, 0x5c),
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
