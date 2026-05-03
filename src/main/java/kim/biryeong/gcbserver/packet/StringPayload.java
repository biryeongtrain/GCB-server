package kim.biryeong.gcbserver.packet;

import kim.biryeong.gcbserver.GcbServer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public class StringPayload implements CustomPacketPayload {
    private final Type<CustomPacketPayload> type;
    private final String data;

    public StringPayload(String data) {
        this.type = CustomPacketPayload.createType(GcbServer.GCB_IDENTIFIER);
        this.data = data;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return type;
    }
}
