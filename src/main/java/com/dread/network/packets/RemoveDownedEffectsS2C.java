package com.dread.network.packets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Server-to-Client packet that signals the client to remove all downed visual effects.
 * Sent when a player is successfully revived or when they choose to respawn.
 * Clears shaders, overlays, and any other downed state rendering.
 */
public record RemoveDownedEffectsS2C() implements CustomPayload {

    public static final CustomPayload.Id<RemoveDownedEffectsS2C> ID =
        new CustomPayload.Id<>(Identifier.of("dread", "remove_downed_effects"));

    public static final PacketCodec<RegistryByteBuf, RemoveDownedEffectsS2C> CODEC =
        PacketCodec.unit(new RemoveDownedEffectsS2C());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
