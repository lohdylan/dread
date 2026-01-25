package com.dread.network.packets;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

/**
 * Server-to-Client packet that updates revival progress for a downed player.
 * Shows a progress bar to the reviver and updates smoothly as they hold right-click.
 * Progress ranges from 0.0 (not started) to 1.0 (fully revived).
 */
public record RevivalProgressS2C(UUID downedPlayerUUID, boolean active, float progress) implements CustomPayload {

    public static final CustomPayload.Id<RevivalProgressS2C> ID =
        new CustomPayload.Id<>(Identifier.of("dread", "revival_progress"));

    // Custom UUID codec
    private static final PacketCodec<ByteBuf, UUID> UUID_CODEC = new PacketCodec<ByteBuf, UUID>() {
        @Override
        public UUID decode(ByteBuf buf) {
            long mostSigBits = buf.readLong();
            long leastSigBits = buf.readLong();
            return new UUID(mostSigBits, leastSigBits);
        }

        @Override
        public void encode(ByteBuf buf, UUID uuid) {
            buf.writeLong(uuid.getMostSignificantBits());
            buf.writeLong(uuid.getLeastSignificantBits());
        }
    };

    public static final PacketCodec<RegistryByteBuf, RevivalProgressS2C> CODEC =
        PacketCodec.tuple(
            UUID_CODEC, RevivalProgressS2C::downedPlayerUUID,
            PacketCodecs.BOOL, RevivalProgressS2C::active,
            PacketCodecs.FLOAT, RevivalProgressS2C::progress,
            RevivalProgressS2C::new
        );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
