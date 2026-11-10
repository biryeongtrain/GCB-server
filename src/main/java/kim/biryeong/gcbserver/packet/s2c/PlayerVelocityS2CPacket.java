package kim.biryeong.gcbserver.packet.s2c;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;

public record PlayerVelocityS2CPacket(double x, double y, double z) implements GCBS2CPacket {
    @Override
    public void send(ServerPlayer player) {
        player.setDeltaMovement(new Vec3(x, y, z));
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
    }

    @Override
    public String encode() {
        // Legacy API only; GCB 4 removed this receiver. send() uses the vanilla 26.3 packet.
        return "VELOCITY:" + x + "," + y + "," + z;
    }
}
