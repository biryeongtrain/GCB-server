package kim.biryeong.gcbserver.packet.c2s;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class GCBC2SPackets {
    private static final Map<String, Function<String[], ? extends GCBC2SPacket>> constructors = new HashMap<>();

    public static GCBC2SPacket create(String rawData) {
        String[] split = rawData.split(":", -1);
        if (split.length == 1) { // 이거 진짜에요? 버전만 주는거
            return rawData.isBlank() ? new GCBIgnoredC2SPacket(rawData) : new GCBVersionC2SPacket(rawData);
        }
        var constructor = constructors.get(split[0]);
        if (constructor == null) return new GCBIgnoredC2SPacket(rawData);
        try {
            return constructor.apply(split);
        } catch (IllegalArgumentException invalidPayload) {
            return new GCBIgnoredC2SPacket(rawData);
        }
    }

    private static void register(String id, Function<String[], ? extends GCBC2SPacket> constructor) {
        constructors.put(id, constructor);
    }

    static {
        register(GCBFeatureC2SPacket.ID, GCBFeatureC2SPacket::decode);
        register(GCBModDataC2SPacket.ID, GCBModDataC2SPacket::decode);
        register(GCBKeyInputC2SPacket.ID, GCBKeyInputC2SPacket::decode);
        register(GCBCameraTypeC2SPacket.ID, GCBCameraTypeC2SPacket::decode);
    }
}
