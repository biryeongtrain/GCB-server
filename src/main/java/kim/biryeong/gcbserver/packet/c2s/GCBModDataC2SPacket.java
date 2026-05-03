package kim.biryeong.gcbserver.packet.c2s;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.Arrays;

public record GCBModDataC2SPacket(String modId) implements GCBC2SPacket {
    public static final String ID = "MODS";
    @Override
    public String id() {
        return ID;
    }

    @Override
    public void apply(ServerPlayNetworking.Context context) {
        this.getPlayer(context).gcb$addPlayerModId(modId);
    }

    @Override
    public String encode() {
        return ID + ":" + modId;
    }

    public static GCBModDataC2SPacket decode(String[] data) {
        if (data.length != 2) {
            throw new IllegalArgumentException("Invalid data format: " + Arrays.toString(data));
        }
        return new GCBModDataC2SPacket(data[1]);
    }
}
