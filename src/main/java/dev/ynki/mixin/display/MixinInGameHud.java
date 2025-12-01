package dev.ynki.mixin.display;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.ynki.events.Event;
import dev.ynki.events.impl.render.EventRender2D;
import dev.ynki.manager.ClientManager;
import dev.ynki.manager.Manager;
import dev.ynki.manager.commandManager.impl.GpsCommand;
import dev.ynki.manager.commandManager.impl.WayPointCommand;
import dev.ynki.modules.render.CrossHair;

@Mixin(InGameHud.class)
public class MixinInGameHud {
    @Inject(at = @At(value = "HEAD"), method = "render")
    public void renderHook(DrawContext drawContext, RenderTickCounter tickCounter, CallbackInfo ci) {
        RenderSystem.enableDepthTest();
        MatrixStack matrices = drawContext.getMatrices();
        if (!ClientManager.legitMode) {
            GpsCommand.render(matrices);
            WayPointCommand.render(matrices);
        }

        Event.call(new EventRender2D(drawContext,matrices,tickCounter));
        if (Manager.FUNCTION_MANAGER.hud.state && Manager.FUNCTION_MANAGER.hud.setting.get("Notifications")) {
            Manager.NOTIFICATION_MANAGER.draw(drawContext);
        }
        RenderSystem.disableDepthTest();
    }


    @Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true)
    private void cancelVignette(DrawContext context, Entity entity, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "renderCrosshair", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/hud/InGameHud;CROSSHAIR_TEXTURE:Lnet/minecraft/util/Identifier;"), cancellable = true)
    public void renderCrosshairHook(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        CrossHair crossHair = Manager.FUNCTION_MANAGER.crossHair;
        if (crossHair.state) {
            crossHair.render(context);
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "renderStatusEffectOverlay", cancellable = true)
    public void renderStatusEffectOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
       if (!ClientManager.legitMode) {
           ci.cancel();
       }
    }

    @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void onRenderScoreboard(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (Manager.FUNCTION_MANAGER.hud.state && Manager.FUNCTION_MANAGER.hud.setting.get("Scoreboard")) {
            ci.cancel();
        }
    }
}