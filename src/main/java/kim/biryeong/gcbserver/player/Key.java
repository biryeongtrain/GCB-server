package kim.biryeong.gcbserver.player;

import kim.biryeong.gcbserver.packet.c2s.GCBKeyInputC2SPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum Key {
    KEY_1(getHotbarTranslationKey("1")),
    KEY_2(getHotbarTranslationKey("2")),
    KEY_3(getHotbarTranslationKey("3")),
    KEY_4(getHotbarTranslationKey("4")),
    KEY_5(getHotbarTranslationKey("5")),
    KEY_6(getHotbarTranslationKey("6")),
    KEY_7(getHotbarTranslationKey("7")),
    KEY_8(getHotbarTranslationKey("8")),
    KEY_9(getHotbarTranslationKey("9")),
    KEY_0(getKeyboardTranslationKey("0")),
    KEY_Z(getKeyboardTranslationKey("z")),
    KEY_X(getKeyboardTranslationKey("x")),
    KEY_C(getKeyboardTranslationKey("c")),
    KEY_V(getKeyboardTranslationKey("v")),
    KEY_ATTACK("key.attack"),
    KEY_USE("key.use"),
    KEY_PICK_ITEM("key.pickItem"),
    KEY_DROP("key.drop"),
    ;

    private static final Map<String, Key> translationKeyToKey = new HashMap<>();
    private final String translationKey;

    Key(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }

    private static String getHotbarTranslationKey(String translationKey) {
        return "key.hotbar." + translationKey;
    }

    private static String getKeyboardTranslationKey(String translationKey) {
        return "key.keyboard." + translationKey;
    }

    public static Optional<Key> fromTranslationKey(String translationKey) {
        return Optional.ofNullable(translationKeyToKey.get(translationKey));
    }

    static {
        for (Key key : values()) {
            translationKeyToKey.put(key.translationKey, key);
        }
    }
}
