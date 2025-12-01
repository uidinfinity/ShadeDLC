package dev.ynki.modules.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector2f;
import dev.ynki.modules.setting.BooleanSetting;
import dev.ynki.modules.setting.SliderSetting;
import dev.ynki.events.Event;
import dev.ynki.events.impl.render.EventRender2D;
import dev.ynki.manager.Manager;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;
import dev.ynki.util.color.ColorUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static dev.ynki.util.render.RenderUtil.drawTexture;

@FunctionAnnotation(name = "Arrows", desc = "Стрелки к игрокам на экране", type = Type.Render)
public class Arrows extends Function {
    private final SliderSetting radius = new SliderSetting("Радиус", 70f, 50f, 160f, 1f);
    private final BooleanSetting dynamic = new BooleanSetting("Динамические", true);
    private float animatedRadius = radius.get().floatValue();
    private final Map<UUID, Float> smoothedAngles = new HashMap<>();

    public Arrows() {
        addSettings(radius, dynamic);
    }

    @Override
    public void onEvent(Event event) {
        if (!(event instanceof EventRender2D render2D)) return;
        var player = mc.player;
        if (player == null) return;

        float targetRadius = radius.get().floatValue();
        if (dynamic.get() && player.isSprinting()) {
            targetRadius += 20f;
        }
        animatedRadius += (targetRadius - animatedRadius) * 0.1f;

        var players = Manager.SYNC_MANAGER.getPlayers();
        for (PlayerEntity other : players) {
            if (other.equals(player) || Manager.FUNCTION_MANAGER.antiBot.check(other)) continue;

            drawArrow(render2D.getMatrixStack(), other, other.getX(), other.getZ(), ColorUtil.getColorStyle(360));
        }
    }

    private void drawArrow(MatrixStack stack, PlayerEntity target, double x, double z, int baseColor) {
        var player = mc.player;
        var window = mc.getWindow();
        int width = window.getScaledWidth();
        int height = window.getScaledHeight();

        float centerX = width / 2f;
        float centerY = height / 2f;

        float desiredAngle = MathHelper.wrapDegrees(getRotationTo(new Vector2f((float) x, (float) z)) - player.getYaw());
        float angle = smoothAngle(target.getUuid(), desiredAngle);

        stack.push();
        stack.translate(centerX, centerY, 0.0F);
        stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
        stack.translate(-centerX, -centerY, 0.0F);

        // Статичный оттенок #9290F0 с лёгкой прозрачностью
        int staticColor = ColorUtil.rgba(0x92, 0x90, 0xF0, 255);
        int arrowColor = ColorUtil.applyAlpha(staticColor, 0.74f);
        int glowColor = ColorUtil.applyAlpha(staticColor, 0.32f);

        // Glow layer – немного больше и с постоянной прозрачностью
        float glowPadding = 3.5f;
        drawTexture(stack, "images/triangle.png",
                centerX - 7 - glowPadding,
                centerY - animatedRadius - glowPadding,
                20 + glowPadding * 2,
                20 + glowPadding * 2,
                3,
                glowColor);

        // Основная стрелка
        drawTexture(stack, "images/triangle.png", centerX - 7, centerY - animatedRadius, 20, 20, 1, arrowColor);
        stack.pop();
    }

    private float smoothAngle(UUID id, float targetAngle) {
        float prev = smoothedAngles.getOrDefault(id, targetAngle);
        float delta = MathHelper.wrapDegrees(targetAngle - prev);
        float factor = 0.08f;
        float result = prev + delta * factor;
        smoothedAngles.put(id, result);
        return result;
    }

    private float getRotationTo(Vector2f vec) {
        var player = mc.player;
        if (player == null) return 0f;

        double dx = vec.x - player.getX();
        double dz = vec.y - player.getZ();

        return (float) -(Math.atan2(dx, dz) * (180.0 / Math.PI));
    }
}