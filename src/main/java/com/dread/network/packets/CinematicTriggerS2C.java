package com.dread.network.packets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Server-to-Client packet that triggers the death cinematic on the client.
 * Sent when a player dies to Dread, initiating the camera animation and shader effects.
 */
public record CinematicTriggerS2C(int dreadEntityId, BlockPos deathPos) implements CustomPayload {

    public static final CustomPayload.Id<CinematicTriggerS2C> ID =
        new CustomPayload.Id<>(Identifier.of("dread", "cinematic_trigger"));

    public static final PacketCodec<RegistryByteBuf, CinematicTriggerS2C> CODEC =
        PacketCodec.tuple(
            PacketCodecs.VAR_INT, CinematicTriggerS2C::dreadEntityId,
            BlockPos.PACKET_CODEC, CinematicTriggerS2C::deathPos,
            CinematicTriggerS2C::new
        );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
