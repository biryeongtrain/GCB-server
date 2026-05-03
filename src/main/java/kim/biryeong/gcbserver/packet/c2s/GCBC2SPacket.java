package kim.biryeong.gcbserver.packet.c2s;

import kim.biryeong.gcbserver.packet.GCBPacket;
import kim.biryeong.gcbserver.player.GCBPlayer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public interface GCBC2SPacket extends GCBPacket {
    String id();
    void apply(ServerPlayNetworking.Context context);
    default GCBPlayer getPlayer(ServerPlayNetworking.Context context) {
        return (GCBPlayer) context.player();
    }
}
