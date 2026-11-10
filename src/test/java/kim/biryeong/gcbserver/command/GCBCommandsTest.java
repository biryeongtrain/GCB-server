package kim.biryeong.gcbserver.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GCBCommandsTest {
    @Test
    @SuppressWarnings("unchecked")
    void keepsOperatorLevelTwoRequirement() throws ReflectiveOperationException {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var method = GCBCommands.class.getDeclaredMethod("rootCommand");
        method.setAccessible(true);
        var command = (LiteralArgumentBuilder<CommandSourceStack>) method.invoke(null);

        for (PermissionLevel level : PermissionLevel.values()) {
            var source = Commands.createCompilationContext(LevelBasedPermissionSet.forLevel(level));
            assertEquals(level.id() >= 2, command.getRequirement().test(source), level.name());
        }
    }
}
