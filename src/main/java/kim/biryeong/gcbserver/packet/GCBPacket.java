package kim.biryeong.gcbserver.packet;

import kim.biryeong.gcbserver.GcbServer;
import kim.biryeong.gcbserver.packet.c2s.GCBC2SPackets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public interface GCBPacket extends CustomPacketPayload {
    StreamCodec<RegistryFriendlyByteBuf, GCBPacket> CODEC = CustomPacketPayload.codec(GCBPacket::encode, GCBPacket::decode);
    Type<GCBPacket> TYPE = CustomPacketPayload.createType(GcbServer.GCB_IDENTIFIER);
    @Override
    default @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    static void encode(GCBPacket packet, RegistryFriendlyByteBuf buf) {
        buf.writeUtf(packet.encode());
    }
    static GCBPacket decode(RegistryFriendlyByteBuf buf) {
        String data = buf.readUtf();
        return GCBC2SPackets.create(data);
    }
    String encode();
}
