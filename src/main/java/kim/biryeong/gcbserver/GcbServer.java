package kim.biryeong.gcbserver;

import kim.biryeong.gcbserver.command.GCBCommands;
import kim.biryeong.gcbserver.packet.GCBPacket;
import kim.biryeong.gcbserver.packet.c2s.GCBC2SPacket;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GcbServer implements ModInitializer {
    public static final String GCB_IDENTIFIER = "gcb";
    public static final Logger LOGGER = LoggerFactory.getLogger(GcbServer.class);
    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(GCBC2SPacket.TYPE, GCBPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(GCBPacket.TYPE, GCBPacket.CODEC);
        GCBCommands.register();

        ServerPlayNetworking.registerGlobalReceiver(GCBPacket.TYPE, (payload, context) -> {
            GCBC2SPacket c2sPayload = (GCBC2SPacket) payload;
            c2sPayload.apply(context);
            if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
                LOGGER.info("Received GCB packet: {}", c2sPayload.encode());
            }
        });
    }
}
