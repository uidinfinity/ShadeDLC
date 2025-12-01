package dev.ynki.mixin.world;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.ynki.manager.ClientManager;
import dev.ynki.manager.IMinecraft;
import dev.ynki.manager.Manager;
import dev.ynki.util.render.providers.ResourceProvider;

@Mixin(AbstractClientPlayerEntity.class)
public class MixinAbstractClientPlayerEntity implements IMinecraft {
    private String cachedPlayerName;

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void injectGetSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
        if (ClientManager.legitMode) {
            return;
        }
        AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) (Object) this;
        String playerName = player.getName().getString();
        if (cachedPlayerName == null) {
            cachedPlayerName = mc.player.getName().getString();
        }
        if (Manager.FRIEND_MANAGER.isFriend(playerName) || playerName.equalsIgnoreCase(cachedPlayerName)) {
            SkinTextures original = cir.getReturnValue();
            SkinTextures newTextures = new SkinTextures(original.texture(),
                    original.textureUrl(),
                    ResourceProvider.CUSTOM_CAPE,
                    ResourceProvider.CUSTOM_ELYTRA,
                    original.model(),
                    original.secure()
            );
            cir.setReturnValue(newTextures);
        }
    }
}