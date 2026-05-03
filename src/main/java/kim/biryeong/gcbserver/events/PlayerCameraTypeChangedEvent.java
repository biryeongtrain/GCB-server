package kim.biryeong.gcbserver.events;

import kim.biryeong.gcbserver.player.CameraType;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface PlayerCameraTypeChangedEvent {
    Event<PlayerCameraTypeChangedEvent> EVENT = EventFactory.createArrayBacked(PlayerCameraTypeChangedEvent.class, listeners -> (player, cameraType) -> {
        for (PlayerCameraTypeChangedEvent listener : listeners) {
            listener.onCameraTypeChanged(player, cameraType);
        }
    });
    void onCameraTypeChanged(ServerPlayer player, CameraType cameraType);
}
