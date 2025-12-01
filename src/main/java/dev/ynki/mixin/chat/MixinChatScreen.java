package dev.ynki.mixin.chat;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.ynki.manager.IMinecraft;
import dev.ynki.manager.Manager;

@Mixin(ChatScreen.class)
public abstract class MixinChatScreen implements IMinecraft {
    private boolean wasMouseDown = false;

    @Inject(method = "render", at = {@At("HEAD")}, cancellable = true)
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        long windowHandle = mc.getWindow().getHandle();
        boolean isMouseDown = GLFW.glfwGetMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        
        if (isMouseDown) {
            if (Manager.DRAG_MANAGER.isSelecting()) {
                Manager.DRAG_MANAGER.updateSelection(mouseX, mouseY);
            } else if (Manager.DRAG_MANAGER.isMovingSelection()) {
                Manager.DRAG_MANAGER.updateSelectionMove(mouseX, mouseY);
            }
        } else if (wasMouseDown) {
            if (Manager.DRAG_MANAGER.isSelecting()) {
                Manager.DRAG_MANAGER.endSelection();
            } else if (Manager.DRAG_MANAGER.isMovingSelection()) {
                Manager.DRAG_MANAGER.finishSelectionMove();
            }
            Manager.DRAG_MANAGER.draggables.values().forEach(dragging -> {
                if (dragging.getModule() != null && dragging.getModule().state) {
                    dragging.onRelease(0);
                }
            });
        }
        
        wasMouseDown = isMouseDown;
        
        Manager.DRAG_MANAGER.draggables.values().forEach((dragging) -> {
            if (dragging.getModule() != null && dragging.getModule().state) {
                dragging.onDraw(mouseX, mouseY, mc.getWindow());
                dragging.renderGuides(mc.getWindow());
            }
        });
        Manager.DRAG_MANAGER.renderSelection(context.getMatrices());
    }

    @Inject(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ChatInputSuggestor;mouseClicked(DDI)Z", shift = At.Shift.AFTER), cancellable = true)
    private void afterSuggestionsClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            // Handle MusicRenderer clicks first (right mouse button only)
            if (button == 1 && Manager.FUNCTION_MANAGER.hud.state && Manager.FUNCTION_MANAGER.hud.setting.get("MusicRender") && Manager.FUNCTION_MANAGER.hud.mediaPlayerRenderer != null) {
                // mouseX and mouseY are already in scaled coordinates from ChatScreen
                Manager.FUNCTION_MANAGER.hud.mediaPlayerRenderer.handleClick(mouseX, mouseY, button);
            }
            
            // Only handle draggables with left mouse button (button == 0)
            if (button == 0) {
                if (Manager.DRAG_MANAGER.isSelecting()) {
                    return;
                }
                
                if (Manager.DRAG_MANAGER.hasSelection()) {
                    if (Manager.DRAG_MANAGER.tryBeginSelectionMove(mouseX, mouseY)) {
                        return;
                    }
                    
                    Manager.DRAG_MANAGER.startSelection(mouseX, mouseY);
                    return;
                }
                
                boolean clickedOnDraggable = false;
                for (dev.ynki.manager.dragManager.Dragging dragging : Manager.DRAG_MANAGER.draggables.values()) {
                    if (dragging.getModule() != null && dragging.getModule().state) {
                        if (dragging.onClick(mouseX, mouseY, button)) {
                            clickedOnDraggable = true;
                            break;
                        }
                    }
                }
                
                if (!clickedOnDraggable) {
                    Manager.DRAG_MANAGER.startSelection(mouseX, mouseY);
                }
            }
            
            if (Manager.FUNCTION_MANAGER.hud.state && Manager.FUNCTION_MANAGER.hud.setting.get("Notifications")) {
                Manager.NOTIFICATION_MANAGER.onClick(mouseX, mouseY, button);
            }
        }
    }
}