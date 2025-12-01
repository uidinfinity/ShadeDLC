package dev.ynki.modules.combat;

import dev.ynki.events.Event;
import dev.ynki.events.impl.input.EventKeyBoard;
import dev.ynki.events.impl.player.EventAttack;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;
import dev.ynki.modules.setting.BooleanSetting;
import dev.ynki.util.move.MoveUtil;

@FunctionAnnotation(name = "WTap", type = Type.Combat, keywords = {"ExtendedAttack","ExtendedKnockBack"}, desc = "Позволяет оттолкнуть противника дальше")
public class AttackExtend extends Function {
    private final BooleanSetting onlyOnGround = new BooleanSetting("Только на земле", true);

    public AttackExtend() {
        addSettings(onlyOnGround);
    }

    private int sprintResetTicks;

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventKeyBoard e && sprintResetTicks > 0 && MoveUtil.isMoving()) {
            e.setMovementForward(0);
            sprintResetTicks--;
        }

        if (event instanceof EventAttack && (!onlyOnGround.get() || mc.player.isOnGround()) && !mc.player.isInFluid() && mc.player.isSprinting()) {
            sprintResetTicks = 1;
        }
    }
}
