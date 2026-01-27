package com.dread.network.packets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Server-to-Client packet that synchronizes a player's downed state to the client.
 * Used to update the UI with remaining revival time and enable/disable downed rendering.
 *
 * isMercyMode: true = SINGLEPLAYER (MERCY mode), false = MULTIPLAYER (NO MERCY mode)
 */
public record DownedStateUpdateS2C(boolean isDowned, int remainingSeconds, boolean isMercyMode) implements CustomPayload {

    public static final CustomPayload.Id<DownedStateUpdateS2C> ID =
        new CustomPayload.Id<>(Identifier.of("dread", "downed_state_update"));

    public static final PacketCodec<RegistryByteBuf, DownedStateUpdateS2C> CODEC =
        PacketCodec.tuple(
            PacketCodecs.BOOL, DownedStateUpdateS2C::isDowned,
            PacketCodecs.VAR_INT, DownedStateUpdateS2C::remainingSeconds,
            PacketCodecs.BOOL, DownedStateUpdateS2C::isMercyMode,
            DownedStateUpdateS2C::new
        );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
