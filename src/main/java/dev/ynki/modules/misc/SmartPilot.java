package dev.ynki.modules.misc;

import dev.ynki.modules.setting.BooleanSetting;
import dev.ynki.modules.setting.ModeSetting;
import dev.ynki.modules.setting.SliderSetting;
import dev.ynki.events.Event;
import dev.ynki.events.impl.EventUpdate;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;

import net.minecraft.block.Blocks;
import net.minecraft.util.math.Vec3d;

@FunctionAnnotation(
        name = "SmartAIPilot",
        type = Type.Misc,
        desc = "ИИ-автопилот: перемещается к цели, избегает лавы и пропастей"
)
public class SmartPilot extends Function {

    private final ModeSetting mode = new ModeSetting("Режим", "None", "None", "Фарм", "Патруль", "Бегство");
    private final SliderSetting speed = new SliderSetting("Скорость", 0.3f, 0.1f, 1.0f, 0.1f);
    private final BooleanSetting avoidLava = new BooleanSetting("Избегать лаву", true);
    private final BooleanSetting avoidVoid = new BooleanSetting("Избегать пропасть", true);

    private Vec3d targetPos = null;
    private long lastActionTime = 0;

    public SmartPilot() {
        addSettings(mode, speed, avoidLava, avoidVoid);
    }

    @Override
    public void onEnable() {
        if (mc.player != null) {
            targetPos = mc.player.getPos();
        }
        lastActionTime = System.currentTimeMillis();
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventUpdate) {
            thinkAndMove();
        }
    }

    private void thinkAndMove() {
        if (mc.player == null || mc.world == null) return;

        long now = System.currentTimeMillis();
        if (now - lastActionTime < 200) return;

        Vec3d current = mc.player.getPos();

        // Обработка режимов
        if (mode.is("Фарм")) {
            if (targetPos == null || current.squaredDistanceTo(targetPos) < 2.0) {
                double range = 8.0;
                targetPos = new Vec3d(
                        current.x + (Math.random() * range - range / 2),
                        current.y,
                        current.z + (Math.random() * range - range / 2)
                );
            }
        } else if (mode.is("Патруль") || mode.is("Бегство")) {
            // Можно расширить позже — пока ведёт себя как None
            // Для простоты оставим как None
        }
        // Режим "None": targetPos не обновляется, остаётся как есть (или null)

        // Проверка на опасность
        boolean danger = false;
        if (avoidLava.get() && isLavaBelow()) {
            danger = true;
        }
        if (avoidVoid.get() && isVoidBelow()) {
            danger = true;
        }

        if (danger) {
            // Отступаем назад при опасности
            Vec3d look = mc.player.getRotationVector();
            targetPos = current.subtract(look.x * 1.5, 0, look.z * 1.5);
        } else if (mode.is("None")) {
            // В режиме None — не двигаемся, если нет угрозы
            // Сбрасываем цель, чтобы не мешать управлению
            targetPos = null;
        }

        // Движение к цели, если она задана
        if (targetPos != null) {
            double dx = targetPos.x - current.x;
            double dz = targetPos.z - current.z;
            double dist = Math.sqrt(dx * dx + dz * dz);

            if (dist > 0.3) {
                double moveSpeed = speed.get().doubleValue();
                mc.player.setVelocity(dx / dist * moveSpeed, mc.player.getVelocity().y, dz / dist * moveSpeed);
            }
        }

        lastActionTime = now;
    }

    private boolean isLavaBelow() {
        return mc.world.getBlockState(mc.player.getBlockPos().down()).isOf(Blocks.LAVA);
    }

    private boolean isVoidBelow() {
        return mc.player.getY() < 5.0 && mc.world.getDimension().natural();
    }
}