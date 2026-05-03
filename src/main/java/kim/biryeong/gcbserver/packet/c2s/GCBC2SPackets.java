package kim.biryeong.gcbserver.packet.c2s;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class GCBC2SPackets {
    private static final Map<String, Function<String[], ? extends GCBC2SPacket>> constructors = new HashMap<>();

    public static GCBC2SPacket create(String rawData) {
        String[] split = rawData.split(":");
        if (split.length == 1) { // 이거 진짜에요? 버전만 주는거
            return new GCBVersionC2SPacket(rawData);
        }
        return constructors.get(split[0]).apply(split);
    }

    private static void register(String id, Function<String[], ? extends GCBC2SPacket> constructor) {
        constructors.put(id, constructor);
    }

    static {
        register(GCBModDataC2SPacket.ID, GCBModDataC2SPacket::decode);
        register(GCBKeyInputC2SPacket.ID, GCBKeyInputC2SPacket::decode);
        register(GCBCameraTypeC2SPacket.ID, GCBCameraTypeC2SPacket::decode);
    }
}
