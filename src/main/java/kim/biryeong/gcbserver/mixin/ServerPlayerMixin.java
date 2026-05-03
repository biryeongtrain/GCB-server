package kim.biryeong.gcbserver.mixin;

import kim.biryeong.gcbserver.player.CameraType;
import kim.biryeong.gcbserver.player.GCBPlayer;
import kim.biryeong.gcbserver.player.Key;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin implements GCBPlayer {
    @Unique
    private String gcb$modVersion = "unknown";
    @Unique
    private final List<String> gcb$playerMods = new ArrayList<>();
    @Unique
    private final EnumMap<Key, Boolean> gcb$keyStates = new EnumMap<>(Key.class);
    @Unique
    CameraType gcb$cameraType = CameraType.FIRST_PERSON;
    @Unique
    private boolean gcb$hasMod = false;

    @Override
    public CameraType gcb$getCameraType() {
        return this.gcb$cameraType;
    }

    @Override
    public void gcb$setCameraType(CameraType cameraType) {
        this.gcb$cameraType = cameraType;
    }

    @Override
    public void gcb$setKey(Key key, boolean pushed) {
        this.gcb$keyStates.put(key, pushed);
    }

    @Override
    public boolean gcb$isKeyPressed(Key key) {
        return this.gcb$keyStates.getOrDefault(key, false);
    }

    @Override
    public void gcb$setModVersion(String version) {
        gcb$modVersion = version;
    }

    @Override
    public String gcb$getModVersion() {
        return this.gcb$modVersion;
    }

    @Override
    public void gcb$addPlayerModId(String modId) {
        gcb$playerMods.add(modId);
    }

    @Override
    public boolean gcb$isModEnabled(String modId) {
        return gcb$playerMods.contains(modId);
    }

    @Override
    public boolean gcb$hasMod() {
        return gcb$hasMod;
    }

    @Override
    public void gcb$markHasMod() {
        this.gcb$hasMod = true;
    }
}
