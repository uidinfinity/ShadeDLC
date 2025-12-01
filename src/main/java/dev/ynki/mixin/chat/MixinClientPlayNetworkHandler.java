package dev.ynki.mixin.chat;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.ynki.manager.ClientManager;
import dev.ynki.manager.Manager;
import dev.ynki.manager.commandManager.CommandManager;
import dev.ynki.manager.notificationManager.NotificationManager;


@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {
    @Shadow private ClientWorld world;

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void sendChatMessageHook(@NotNull String message, CallbackInfo ci) {
        if (!ClientManager.legitMode) {
            CommandManager commandManager = Manager.COMMAND_MANAGER;
            if (message.startsWith(commandManager.getPrefix())) {
                try {
                    commandManager.getDispatcher().execute(message.substring(commandManager.getPrefix().length()), commandManager.getSource());
                } catch (CommandSyntaxException ignored) {
                }
                ci.cancel();
            }
        }
    }

    @Inject(method = "onEntityStatus", at = @At("HEAD"))
    private void onEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
        if (packet.getStatus() != EntityStatuses.USE_TOTEM_OF_UNDYING || world == null) {
            return;
        }

        Entity entity = packet.getEntity(world);
        if (entity instanceof PlayerEntity player) {
            NotificationManager.addTotemPop(player.getName().getString());
        }
    }
}