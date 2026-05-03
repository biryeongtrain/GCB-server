package kim.biryeong.gcbserver.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import kim.biryeong.gcbserver.packet.s2c.GCBParticleS2CPacket;
import kim.biryeong.gcbserver.packet.s2c.PlayerVelocityS2CPacket;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class GCBCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(rootCommand()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> rootCommand() {
        return Commands.literal("gcb")
                .requires(source -> source.hasPermission(2))
                .then(velocityCommand())
                .then(particleCommand());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> velocityCommand() {
        return Commands.literal("velocity")
                .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                        .executes(context -> {
                                            new PlayerVelocityS2CPacket(
                                                    DoubleArgumentType.getDouble(context, "x"),
                                                    DoubleArgumentType.getDouble(context, "y"),
                                                    DoubleArgumentType.getDouble(context, "z")
                                            ).send(context.getSource().getPlayerOrException());
                                            return Command.SINGLE_SUCCESS;
                                        }))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> particleCommand() {
        return Commands.literal("particle")
                .executes(GCBCommands::executeDefaultParticle)
                .then(circleParticleCommand())
                .then(lineParticleCommand())
                .then(trailParticleCommand())
                .then(sphereParticleCommand())
                .then(spiralParticleCommand())
                .then(spiralLineParticleCommand());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> circleParticleCommand() {
        var points = withParticleOptions(
                Commands.argument("points", IntegerArgumentType.integer(1)),
                GCBCommands::executeCircleParticle
        );
        var radius = Commands.argument("radius", DoubleArgumentType.doubleArg(0.0d)).then(points);
        var center = Commands.argument("center", Vec3Argument.vec3()).then(radius);
        var particle = particleArgument().then(center);
        return Commands.literal("circle").then(particle);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> lineParticleCommand() {
        var period = withParticleOptions(
                Commands.argument("period", DoubleArgumentType.doubleArg(0.0d)),
                GCBCommands::executeLineParticle
        );
        var to = Commands.argument("to", Vec3Argument.vec3()).then(period);
        var from = Commands.argument("from", Vec3Argument.vec3()).then(to);
        var particle = particleArgument().then(from);
        return Commands.literal("line").then(particle);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> trailParticleCommand() {
        var period = withParticleOptions(
                Commands.argument("period", DoubleArgumentType.doubleArg(0.0d)),
                GCBCommands::executeTrailParticle
        );
        var to = Commands.argument("to", Vec3Argument.vec3()).then(period);
        var from = Commands.argument("from", Vec3Argument.vec3()).then(to);
        var particle = particleArgument().then(from);
        return Commands.literal("trail").then(particle);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> sphereParticleCommand() {
        var points = withParticleOptions(
                Commands.argument("points", IntegerArgumentType.integer(1)),
                GCBCommands::executeSphereParticle
        );
        var radius = Commands.argument("radius", DoubleArgumentType.doubleArg(0.0d)).then(points);
        var center = Commands.argument("center", Vec3Argument.vec3()).then(radius);
        var particle = particleArgument().then(center);
        return Commands.literal("sphere").then(particle);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> spiralParticleCommand() {
        var converge = withParticleOptions(
                Commands.argument("converge", BoolArgumentType.bool()),
                GCBCommands::executeSpiralParticle
        );
        var increment = Commands.argument("increment", DoubleArgumentType.doubleArg()).then(converge);
        var period = Commands.argument("period", DoubleArgumentType.doubleArg(0.0d)).then(increment);
        var points = Commands.argument("points", IntegerArgumentType.integer(1)).then(period);
        var radius = Commands.argument("radius", DoubleArgumentType.doubleArg(0.0d)).then(points);
        var center = Commands.argument("center", Vec3Argument.vec3()).then(radius);
        var particle = particleArgument().then(center);
        return Commands.literal("spiral").then(particle);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> spiralLineParticleCommand() {
        var inverse = withParticleOptions(
                Commands.argument("inverse", BoolArgumentType.bool()),
                GCBCommands::executeSpiralLineParticle
        );
        var startDegree = Commands.argument("startDegree", DoubleArgumentType.doubleArg()).then(inverse);
        var points = Commands.argument("points", IntegerArgumentType.integer(1)).then(startDegree);
        var radius = Commands.argument("radius", DoubleArgumentType.doubleArg(0.0d)).then(points);
        var period = Commands.argument("period", DoubleArgumentType.doubleArg(0.0d)).then(radius);
        var to = Commands.argument("to", Vec3Argument.vec3()).then(period);
        var from = Commands.argument("from", Vec3Argument.vec3()).then(to);
        var particle = particleArgument().then(from);
        return Commands.literal("spiralline").then(particle);
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> particleArgument() {
        return Commands.argument("particle", StringArgumentType.string())
                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.PARTICLE_TYPE.keySet(), builder));
    }

    private static <T extends ArgumentBuilder<CommandSourceStack, T>> T withParticleOptions(T builder, ParticleExecutor executor) {
        var data = Commands.argument("data", StringArgumentType.string())
                .executes(context -> executor.run(
                        context,
                        IntegerArgumentType.getInteger(context, "count"),
                        DoubleArgumentType.getDouble(context, "speed"),
                        BoolArgumentType.getBool(context, "force"),
                        StringArgumentType.getString(context, "data")
                ));
        var force = Commands.argument("force", BoolArgumentType.bool()).then(data);
        var speed = Commands.argument("speed", DoubleArgumentType.doubleArg()).then(force);
        var count = Commands.argument("count", IntegerArgumentType.integer(0)).then(speed);
        return builder
                .executes(context -> executor.run(context, 0, 0.0d, false, ""))
                .then(count);
    }

    private static int executeDefaultParticle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        sendParticle(
                player,
                "flame",
                0,
                0.0d,
                false,
                "",
                new GCBParticleS2CPacket.Circle(
                        GCBParticleS2CPacket.Vec.UNIT_Y,
                        GCBParticleS2CPacket.Vec.UNIT_X,
                        60,
                        360.0d,
                        GCBParticleS2CPacket.ShapeOptions.DEFAULT,
                        new GCBParticleS2CPacket.Vec(player.getX(), player.getY() + 1.0d, player.getZ())
                )
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int executeCircleParticle(CommandContext<CommandSourceStack> context, int count, double speed, boolean force, String data) throws CommandSyntaxException {
        sendParticle(
                context.getSource().getPlayerOrException(),
                StringArgumentType.getString(context, "particle"),
                count,
                speed,
                force,
                data,
                new GCBParticleS2CPacket.Circle(
                        GCBParticleS2CPacket.Vec.UNIT_Y,
                        new GCBParticleS2CPacket.Vec(DoubleArgumentType.getDouble(context, "radius"), 0.0d, 0.0d),
                        IntegerArgumentType.getInteger(context, "points"),
                        360.0d,
                        GCBParticleS2CPacket.ShapeOptions.DEFAULT,
                        getVec(context, "center")
                )
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int executeLineParticle(CommandContext<CommandSourceStack> context, int count, double speed, boolean force, String data) throws CommandSyntaxException {
        sendParticle(
                context.getSource().getPlayerOrException(),
                StringArgumentType.getString(context, "particle"),
                count,
                speed,
                force,
                data,
                new GCBParticleS2CPacket.Line(
                        DoubleArgumentType.getDouble(context, "period"),
                        GCBParticleS2CPacket.ShapeOptions.DEFAULT,
                        getVec(context, "from"),
                        getVec(context, "to")
                )
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int executeTrailParticle(CommandContext<CommandSourceStack> context, int count, double speed, boolean force, String data) throws CommandSyntaxException {
        sendParticle(
                context.getSource().getPlayerOrException(),
                StringArgumentType.getString(context, "particle"),
                count,
                speed,
                force,
                data,
                new GCBParticleS2CPacket.Trail(
                        DoubleArgumentType.getDouble(context, "period"),
                        GCBParticleS2CPacket.ShapeOptions.DEFAULT,
                        getVec(context, "from"),
                        getVec(context, "to")
                )
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int executeSphereParticle(CommandContext<CommandSourceStack> context, int count, double speed, boolean force, String data) throws CommandSyntaxException {
        sendParticle(
                context.getSource().getPlayerOrException(),
                StringArgumentType.getString(context, "particle"),
                count,
                speed,
                force,
                data,
                new GCBParticleS2CPacket.Sphere(
                        new GCBParticleS2CPacket.Vec(0.0d, DoubleArgumentType.getDouble(context, "radius"), 0.0d),
                        IntegerArgumentType.getInteger(context, "points"),
                        GCBParticleS2CPacket.ShapeOptions.DEFAULT,
                        getVec(context, "center")
                )
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int executeSpiralParticle(CommandContext<CommandSourceStack> context, int count, double speed, boolean force, String data) throws CommandSyntaxException {
        sendParticle(
                context.getSource().getPlayerOrException(),
                StringArgumentType.getString(context, "particle"),
                count,
                speed,
                force,
                data,
                new GCBParticleS2CPacket.Spiral(
                        GCBParticleS2CPacket.Vec.UNIT_Y,
                        new GCBParticleS2CPacket.Vec(DoubleArgumentType.getDouble(context, "radius"), 0.0d, 0.0d),
                        IntegerArgumentType.getInteger(context, "points"),
                        DoubleArgumentType.getDouble(context, "period"),
                        DoubleArgumentType.getDouble(context, "increment"),
                        BoolArgumentType.getBool(context, "converge"),
                        GCBParticleS2CPacket.ShapeOptions.DEFAULT,
                        getVec(context, "center")
                )
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int executeSpiralLineParticle(CommandContext<CommandSourceStack> context, int count, double speed, boolean force, String data) throws CommandSyntaxException {
        sendParticle(
                context.getSource().getPlayerOrException(),
                StringArgumentType.getString(context, "particle"),
                count,
                speed,
                force,
                data,
                new GCBParticleS2CPacket.SpiralLine(
                        DoubleArgumentType.getDouble(context, "period"),
                        DoubleArgumentType.getDouble(context, "radius"),
                        IntegerArgumentType.getInteger(context, "points"),
                        DoubleArgumentType.getDouble(context, "startDegree"),
                        BoolArgumentType.getBool(context, "inverse"),
                        GCBParticleS2CPacket.ShapeOptions.DEFAULT,
                        getVec(context, "from"),
                        getVec(context, "to")
                )
        );
        return Command.SINGLE_SUCCESS;
    }

    private static void sendParticle(
            ServerPlayer player,
            String particle,
            int count,
            double speed,
            boolean force,
            String data,
            GCBParticleS2CPacket.ShapeData shape
    ) {
        new GCBParticleS2CPacket(
                stripMinecraftNamespace(particle),
                GCBParticleS2CPacket.Vec.ZERO,
                count,
                speed,
                force,
                stripMinecraftNamespace(data),
                shape
        ).send(player);
    }

    private static GCBParticleS2CPacket.Vec getVec(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        Vec3 vec = Vec3Argument.getVec3(context, name);
        return new GCBParticleS2CPacket.Vec(vec.x, vec.y, vec.z);
    }

    private static String stripMinecraftNamespace(String value) {
        return value.startsWith("minecraft:") ? value.substring("minecraft:".length()) : value;
    }

    @FunctionalInterface
    private interface ParticleExecutor {
        int run(CommandContext<CommandSourceStack> context, int count, double speed, boolean force, String data) throws CommandSyntaxException;
    }
}
