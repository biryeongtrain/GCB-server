package kim.biryeong.gcbserver.packet.s2c;

import kim.biryeong.gcbserver.packet.GCBPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public interface GCBS2CPacket extends GCBPacket {
    default void send(ServerPlayer player) {
        ServerPlayNetworking.send(player, this);
    }
}
