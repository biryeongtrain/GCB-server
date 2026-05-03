package kim.biryeong.gcbserver.packet.c2s;

import kim.biryeong.gcbserver.player.GCBPlayer;
import kim.biryeong.gcbserver.player.Key;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.Arrays;

public record GCBKeyInputC2SPacket(Key key, boolean pushed) implements GCBC2SPacket {
    public static final String ID = "KEYINPUT";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void apply(ServerPlayNetworking.Context context) {
        GCBPlayer player = this.getPlayer(context);
        player.gcb$setKey(key, pushed);
    }

    public static GCBC2SPacket decode(String[] data) {
        if (data.length != 3) {
            throw new IllegalArgumentException("Invalid data format: " + Arrays.toString(data));
        }

        return new GCBKeyInputC2SPacket(Key.fromTranslationKey(data[1]).orElseThrow(), Boolean.parseBoolean(data[2]));
    }

    @Override
    public String encode() {
        return ID + ":" + key.translationKey() + ":" + pushed;
    }
}
