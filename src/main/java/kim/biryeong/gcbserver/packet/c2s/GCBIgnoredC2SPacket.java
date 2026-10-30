package kim.biryeong.gcbserver.packet.c2s;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Unknown extensions and malformed messages must not mutate player input or presence. */
public record GCBIgnoredC2SPacket(String raw) implements GCBC2SPacket {
    @Override public String id() { return "IGNORED"; }
    @Override public boolean accepted() { return false; }
    @Override public void apply(ServerPlayNetworking.Context context) { }
    @Override public String encode() { return raw; }
}
