package dev.ynki.mixin.client;

import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.ynki.manager.Manager;
import dev.ynki.manager.proxyManager.GuiProxy;
import dev.ynki.manager.proxyManager.Proxy;
import dev.ynki.manager.proxyManager.ProxyManager;
import dev.ynki.mixin.iface.ScreenAccessor;

@Mixin(MultiplayerScreen.class)
public class MultiplayerScreenMixin {
    @Inject(method = "init()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/multiplayer/MultiplayerScreen;updateButtonActivationStates()V"))
    public void multiplayerGuiOpen(CallbackInfo ci) {
        ProxyManager pm = Manager.PROXY_MANAGER;
        String playerName = pm.mc.getSession().getUsername();

        if (!playerName.equals(pm.lastPlayerName)) {
            pm.lastPlayerName = playerName;
            pm.proxy = pm.accounts.getOrDefault(playerName, pm.accounts.getOrDefault("", new Proxy()));
        }

        MultiplayerScreen screen = (MultiplayerScreen) (Object) this;
        pm.proxyMenuButton = ButtonWidget.builder(Text.literal("Прокси: " + pm.getLastUsedProxyIp()), b -> pm.mc.setScreen(new GuiProxy(screen)))
                .dimensions(screen.width - 320, 479, 100, 20).build();

        ScreenAccessor sa = (ScreenAccessor) screen;
        sa.getDrawables().add(pm.proxyMenuButton);
        sa.getSelectables().add(pm.proxyMenuButton);
        sa.getChildren().add(pm.proxyMenuButton);
    }
}
