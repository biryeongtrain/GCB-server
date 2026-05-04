package kim.biryeong.gcbserver.packet.c2s;

import kim.biryeong.gcbserver.events.PlayerCameraTypeChangedEvent;
import kim.biryeong.gcbserver.player.CameraType;
import kim.biryeong.gcbserver.player.GCBPlayer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamDecoder;

public record GCBCameraTypeC2SPacket(CameraType cameraType) implements GCBC2SPacket {
    public static final String ID = "PERSPECTIVE";
    public static final StreamDecoder<FriendlyByteBuf, GCBCameraTypeC2SPacket> DECODER = object -> {
        String s = object.readUtf();
        return new GCBCameraTypeC2SPacket(CameraType.valueOf(s));
    };

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void apply(ServerPlayNetworking.Context context) {
        GCBPlayer player = this.getPlayer(context);
        player.gcb$setCameraType(cameraType);
        PlayerCameraTypeChangedEvent.EVENT.invoker().onCameraTypeChanged(context.player(), cameraType);
    }

    public static GCBCameraTypeC2SPacket decode(String[] data) {
        return new GCBCameraTypeC2SPacket(CameraType.valueOf(data[1]));
    }

    @Override
    public String encode() {
        return id() + ":" + cameraType.toString();
    }

}
