package dev.ynki.modules.player;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;
import dev.ynki.events.Event;
import dev.ynki.events.impl.input.EventMouse;
import dev.ynki.manager.ClientManager;
import dev.ynki.manager.Manager;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;

@FunctionAnnotation(name = "MiddleClickFriend", keywords = {"MCF"}, desc = "Управление друзьями", type = Type.Player)
public class MiddleClickFriend extends Function {
    @Override
    public void onEvent(Event event) {
        if (event instanceof EventMouse e) {
            if (e.getButton() == 2) {
                if (mc.crosshairTarget instanceof EntityHitResult entityHitResult) {
                    if (entityHitResult.getEntity() instanceof PlayerEntity player) {
                        final String name = player.getName().getString();
                        if (Manager.FRIEND_MANAGER.isFriend(name)) {
                            Manager.FRIEND_MANAGER.removeFriend(name);
                            ClientManager.message(Formatting.GRAY + name + Formatting.RED + " удалён из друзей");
                        } else {
                            Manager.FRIEND_MANAGER.addFriend(name);
                            ClientManager.message(Formatting.GRAY + name + Formatting.GREEN + " добавлен в друзья");
                        }
                    }
                }
            }
        }
    }
}
