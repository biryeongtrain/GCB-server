package kim.biryeong.gcbserver.player;

public interface GCBPlayer {
    CameraType gcb$getCameraType();
    void gcb$setCameraType(CameraType cameraType);
    void gcb$setKey(Key key, boolean pushed);
    boolean gcb$isKeyPressed(Key key);
    void gcb$setModVersion(String version);
    String gcb$getModVersion();
    void gcb$addPlayerModId(String modId);
    boolean gcb$isModEnabled(String modId);
    boolean gcb$hasMod();
    void gcb$markHasMod();
}
