package dev.ynki.modules.movement;

import dev.ynki.events.Event;
import dev.ynki.events.impl.EventUpdate;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;

@FunctionAnnotation(name = "AutoSprint", desc = "Автоматически включает бег", type = Type.Move)
public class AutoSprint extends Function {
    @Override
    public void onEvent(Event event) {
        if (event instanceof EventUpdate) {
            if (mc.player != null && mc.player.input != null) {
                boolean movingForward = mc.player.input.movementForward > 0;
                if (movingForward && !mc.player.isSneaking() && !mc.player.horizontalCollision) {
                    mc.player.setSprinting(true);
                } else {
                    mc.player.setSprinting(false);
                }
            }
        }
    }

    @Override
    protected void onDisable() {
        if (mc.player != null) {
            mc.player.setSprinting(false);
        }
        super.onDisable();
    }
}