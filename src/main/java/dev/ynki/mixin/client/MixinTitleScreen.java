package dev.ynki.mixin.client;

import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.ynki.manager.ClientManager;
import dev.ynki.manager.IMinecraft;
import dev.ynki.screens.mainmenu.MainMenu;

@Mixin(TitleScreen.class)
public class MixinTitleScreen implements IMinecraft {
    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void onInit(CallbackInfo ci) {
        if (!ClientManager.legitMode) {
            mc.setScreen(new MainMenu());
            ci.cancel();
        }
    }
}
