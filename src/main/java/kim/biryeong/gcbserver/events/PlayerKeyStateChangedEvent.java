package kim.biryeong.gcbserver.events;

import kim.biryeong.gcbserver.player.Key;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface PlayerKeyStateChangedEvent {
    Event<PlayerKeyStateChangedEvent> EVENT = EventFactory.createArrayBacked(PlayerKeyStateChangedEvent.class, listeners -> (player, pushed, key) -> {
        for (PlayerKeyStateChangedEvent listener : listeners) {
            listener.onKeyStateChanged(player, pushed, key);
        }
    });
    void onKeyStateChanged(ServerPlayer player, boolean pushed, Key key);
}
