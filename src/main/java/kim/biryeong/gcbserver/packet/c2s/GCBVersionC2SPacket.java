package kim.biryeong.gcbserver.packet.c2s;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public record GCBVersionC2SPacket(String version) implements GCBC2SPacket {

    @Override
    public String id() {
        return "";
    }

    @Override
    public void apply(ServerPlayNetworking.Context context) {
        this.getPlayer(context).gcb$setModVersion(version);
    }

    @Override
    public String encode() {
        return version;
    }
}
