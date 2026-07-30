package com.tacz.guns.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.resource.CommonAssetsManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.time.StopWatch;

import java.util.concurrent.TimeUnit;

public class ReloadCommand {
    private static final String RELOAD_NAME = "reload";

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        LiteralArgumentBuilder<CommandSourceStack> reload = Commands.literal(RELOAD_NAME);
        reload.executes(ReloadCommand::reloadAllPack);
        return reload;
    }

    private static int reloadAllPack(CommandContext<CommandSourceStack> context) {
        StopWatch watch = StopWatch.createStarted();
        // 数据端的重载是同步的，vanilla 在服务端线程上调用时会自己 managedBlock 等完
        CommonAssetsManager.reloadAllPack();
        watch.stop();
        double time = watch.getTime(TimeUnit.MICROSECONDS) / 1000.0;
        /* 客户端资源要另外重载一次，而且必须派发到客户端线程上，所以它不算进上面的时间里。
         * 专用服务器上没有客户端资源可以重载。 */
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            reloadClient();
        }
        context.getSource().sendSystemMessage(Component.translatable("commands.tacz.reload.success", time));
        return Command.SINGLE_SUCCESS;
    }

    public static void reloadClient() {
        ClientAssetsManager.reloadAllPack();
    }
}
