package kim.biryeong.gcbserver.packet.c2s;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** GCB 4.2 announces optional capabilities immediately after its version. */
public record GCBFeatureC2SPacket(String feature) implements GCBC2SPacket {
    public static final String ID = "FEATURE";
    public static GCBFeatureC2SPacket decode(String[] data) {
        if (data.length != 2 || !data[1].matches("[A-Z][A-Z0-9_]{0,63}"))
            throw new IllegalArgumentException("Invalid feature payload");
        return new GCBFeatureC2SPacket(data[1]);
    }
    @Override public String id() { return ID; }
    @Override public void apply(ServerPlayNetworking.Context context) {
        getPlayer(context).gcb$addFeature(feature);
    }
    @Override public String encode() { return ID + ":" + feature; }
}
