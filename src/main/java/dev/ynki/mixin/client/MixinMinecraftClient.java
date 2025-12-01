package dev.ynki.mixin.client;

import dev.ynki.manager.ClientManager;
import dev.ynki.manager.IMinecraft;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.ynki.Velyasik;

@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient implements IMinecraft {
    @Inject(method = "isMultiplayerEnabled", at = @At("HEAD"), cancellable = true)
    private void isMultiplayerEnabled(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

    @Inject(method = "getWindowTitle", at = @At("HEAD"), cancellable = true)
    private void getWindowTitle(CallbackInfoReturnable<String> cir) {
        if (!ClientManager.legitMode) {
            cir.setReturnValue("shadedlc - return | uidinfinity");
        }
    }
    @Inject(at = @At("HEAD"), method = "stop")
    private void stop(CallbackInfo ci) {
        Velyasik.getInstance().shutDown();
    }
    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(CallbackInfo callbackInfo) {
        Velyasik.getInstance().init();
    }
}