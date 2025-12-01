package dev.ynki.modules.movement;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.Box;
import dev.ynki.events.Event;
import dev.ynki.events.impl.move.EventMotion;
import dev.ynki.manager.ClientManager;
import dev.ynki.manager.Manager;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;
import dev.ynki.modules.combat.TargetStrafe;
import dev.ynki.modules.setting.BooleanSetting;
import dev.ynki.modules.setting.ModeSetting;
import dev.ynki.modules.setting.SliderSetting;
import dev.ynki.util.move.MoveUtil;
import dev.ynki.util.player.TimerUtil;

@FunctionAnnotation(name = "Speed", desc = "Поможет умереть быстрее", type = Type.Move)
public class Speed extends Function {

    private final ModeSetting mode = new ModeSetting("Режим", "Vanilla", "Vanilla", "Collision");

    private final SliderSetting speed = new SliderSetting("Скорость",1f,0.1f,3f,0.1f, () -> mode.is("Vanilla"));
    private final SliderSetting collisionRadius = new SliderSetting("Радиус", 2.5f, 0.5f, 6f, 0.1f, () -> mode.is("Collision"));
    private final SliderSetting collisionBoost = new SliderSetting("Прирост", 40f, 5f, 150f, 5f, () -> mode.is("Collision"));
    private final BooleanSetting onlyPlayers = new BooleanSetting("Только игроки", true, () -> mode.is("Collision"));
    private final TimerUtil timerUtil = new TimerUtil();

    public Speed() {
        addSettings(mode, speed, collisionRadius, collisionBoost, onlyPlayers);
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventMotion) {
            if (mc.player == null || mc.world == null) return;

            switch (mode.get()) {
                case "Vanilla" -> vanilla();
                case "Collision" -> collisionBoost();
            }
        }
    }

    private void vanilla() {
        if (MoveUtil.isMoving() && !mc.player.isGliding()) {
            MoveUtil.setSpeed(speed.get().floatValue());
        }
    }

    private void collisionBoost() {
        if (!MoveUtil.isMoving() || mc.player.isGliding()) return;

        Box expanded = mc.player.getBoundingBox().expand(collisionRadius.get().doubleValue());
        int collisions = 0;

        for (Entity entity : mc.world.getEntities()) {
            if (entity == null || entity == mc.player || entity.isRemoved()) continue;
            if (onlyPlayers.get() && !(entity instanceof PlayerEntity)) continue;
            if (!(entity instanceof LivingEntity || entity instanceof BoatEntity)) continue;

            if (expanded.intersects(entity.getBoundingBox())) {
                collisions++;
            }
        }

        if (collisions <= 0) return;

        double boost = collisionBoost.get().doubleValue() * 0.01 * collisions;
        double[] motion = MoveUtil.forward(boost);
        mc.player.addVelocity(motion[0], 0.0, motion[1]);
        mc.player.velocityModified = true;
    }


    @Override
    protected void onEnable() {
        TargetStrafe targetStrafe = Manager.FUNCTION_MANAGER.targetStrafe;
        if (targetStrafe.state) {
            targetStrafe.setState(false);
        }
        timerUtil.reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        ClientManager.TICK_TIMER = 1.0f;
        super.onDisable();
    }
}
