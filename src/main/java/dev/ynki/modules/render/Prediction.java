package dev.ynki.modules.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import dev.ynki.events.Event;
import dev.ynki.events.impl.render.EventRender2D;
import dev.ynki.events.impl.render.EventRender3D;
import dev.ynki.manager.IMinecraft;
import dev.ynki.manager.Manager;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;
import dev.ynki.modules.setting.BooleanSetting;
import dev.ynki.util.color.ColorUtil;
import dev.ynki.manager.fontManager.FontUtils;
import dev.ynki.util.math.MathUtil;
import dev.ynki.util.render.RenderAddon;
import dev.ynki.util.render.RenderUtil;
import dev.ynki.util.shader.ShaderManager;
import dev.ynki.util.vector.VectorUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static dev.ynki.util.render.RenderUtil.render3D.drawHoleOutline;

@SuppressWarnings("All")
@FunctionAnnotation(name = "Prediction", type = Type.Render, desc = "Рисует линию куда упадёт эндер-жемчюг")
public class Prediction extends Function {
    private final BooleanSetting box = new BooleanSetting("Рисовать бокс", false);
    private final BooleanSetting rect = new BooleanSetting("Рисовать рект под эндер-жемчюгом", false);
    private static final ItemStack ENDER_PEARL_STACK = new ItemStack(Items.ENDER_PEARL);
    private static final Color BOX_COLOR = new Color(255, 255, 255, 255);
    private static final int MAX_STEPS = 150;
    private static final float FADE_LEN = 6.0f;

    private final List<PearlPoint> pearlPoints = new ArrayList<>();

    public Prediction() {
        addSettings(box, rect);
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventRender2D render2D) {
            for (PearlPoint pearlPoint : pearlPoints) {

                Vector3d projection = VectorUtil.toScreen(pearlPoint.position.x, pearlPoint.position.y - 0.3F, pearlPoint.position.z);
                if (projection == null || projection.z < 0) continue;

                double time = pearlPoint.ticks * 0.05;
                float centerX = (float) projection.x;
                float centerY = (float) projection.y;

                // Размеры и позиции
                float pearlSize = 11;
                float circleRadius = 7;
                float pearlX = centerX - pearlSize / 2f;
                float pearlY = centerY - pearlSize / 2f;

                // Рисуем фон круга
                RenderUtil.drawRoundedRect(render2D.getMatrixStack(), centerX - circleRadius - 0.5f, centerY - circleRadius - 0.5f,
                        (circleRadius + 0.5f) * 2, (circleRadius + 0.5f) * 2, circleRadius + 0.5f,
                        new Color(22, 22, 22, 150).getRGB());

                // Анимированная обводка (прогресс таймера) - ВСЕГДА проходит полный круг (360°)
                // Скорость зависит от времени полета: быстрый полет = быстрая обводка, медленный = медленная
                // Обводка начинается сверху и идет по часовой стрелке
                if (pearlPoint.entity == null) continue;
                
                // Пересчитываем оставшееся время полета каждый кадр для точности
                int currentAge = pearlPoint.entity.age;
                int remainingTicks = calculateRemainingTicks(pearlPoint.entity);
                
                // Используем пересчитанное общее время полета (текущий возраст + оставшееся время)
                int totalTicks = currentAge + remainingTicks;
                float totalTime = totalTicks * 0.05f; // Общее время полета в секундах
                float currentTime = currentAge * 0.05f; // Текущее время полета в секундах
                
                // Прогресс от 0 до 1, где 1 = полный круг (момент падения)
                // Когда currentAge приближается к totalTicks, progress приближается к 1.0
                float progress = totalTicks > 0 ? Math.min(1.0f, (float)currentAge / totalTicks) : 1.0f;

                drawCircularProgress(render2D.getMatrixStack(), centerX, centerY, circleRadius,
                        progress, ColorUtil.getColorStyle((int)pearlPoint.ticks));

                // Рисуем предмет в центре
                RenderAddon.renderItem(render2D.getDrawContext(), ENDER_PEARL_STACK, pearlX, pearlY, pearlSize / 16f, false);

                // Текст времени снизу
                String text = String.format("%.1f", time);
                float fontHeight = FontUtils.durman[13].getHeight();
                float textWidth = FontUtils.durman[13].getWidth(text);

                float textY = centerY + circleRadius + 11;
                float bgX = centerX - textWidth / 2f - 1.5f;
                float bgY = textY - 1;
                float textX = centerX - textWidth / 2f;

                RenderUtil.drawRoundedRect(render2D.getMatrixStack(), bgX, bgY, textWidth + 3, fontHeight + 2, 2,
                        new Color(22, 22, 22, 150).getRGB());
                FontUtils.durman[13].drawLeftAligned(render2D.getDrawContext().getMatrices(), text, textX, textY, -1);
            }
        }
        if (event instanceof EventRender3D e3d) {
            renderTrajectories(e3d);
        }
    }

    private void drawCircularProgress(MatrixStack matrices, float centerX, float centerY, float radius, float progress, int color) {
        if (progress <= 0) return; // Не рисуем если прогресс 0
        
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glLineWidth(2.5f);

        // В экранных координатах: 0° = право, 90° = низ, 180° = лево, 270°/-90° = верх
        // Начинаем строго сверху: -90° (или 270°)
        float startAngleDegrees = -90f; // Верх = -90° в экранных координатах
        // Общий угол заполнения по часовой стрелке (от 0 до 360)
        // По часовой стрелке в экранных координатах = увеличение угла от -90° до 270°
        float totalAngleDegrees = 360f * progress;
        
        // Количество сегментов для плавной обводки
        int segments = Math.max(72, (int)(360 * progress)); // Минимум 72 сегмента для плавности

        BufferBuilder buffer = IMinecraft.tessellator().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        float[] rgba = ColorUtil.rgba(color);

        // Рисуем обводку по часовой стрелке от -90° (сверху) до -90° + 360° (снова сверху)
        // Когда progress = 1, totalAngle = 360°, обводка делает полный круг и заканчивается там же где началась (сверху)
        for (int i = 0; i <= segments; i++) {
            float currentAngleDegrees = startAngleDegrees + (totalAngleDegrees * i / segments);
            float angleRadians = (float) Math.toRadians(currentAngleDegrees);
            float x = centerX + (float) Math.cos(angleRadians) * radius;
            float y = centerY + (float) Math.sin(angleRadians) * radius;
            buffer.vertex(matrix, x, y, 0).color(rgba[0], rgba[1], rgba[2], rgba[3]);
        }

        RenderUtil.render3D.endBuilding(buffer);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.disableBlend();
    }

    private void renderTrajectories(EventRender3D event) {
        MatrixStack stack = event.getMatrixStack();
        Vec3d cameraPos = mc.getEntityRenderDispatcher().camera.getPos();

        stack.push();
        stack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderUtil.enableRender(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
        RenderSystem.lineWidth(3);

        BufferBuilder buffer = IMinecraft.tessellator().begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
        pearlPoints.clear();

        for (Entity entity : Manager.SYNC_MANAGER.getEntities()) {
            if (entity instanceof EnderPearlEntity enderPearlEntity)
                simulatePearl(stack, buffer, enderPearlEntity);
        }

        RenderUtil.render3D.endBuilding(buffer);

        if (box.get()) {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            for (PearlPoint pearlPoint : pearlPoints) {
                Vec3d pos = pearlPoint.position;
                Box outlineBox = new Box(pos.x - 0.15, pos.y - 0.15, pos.z - 0.15, pos.x + 0.15, pos.y + 0.15, pos.z + 0.15);
                drawHoleOutline(outlineBox, BOX_COLOR.getRGB(), 1);
            }
        }


        RenderUtil.disableRender();
        RenderSystem.enableDepthTest();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        stack.pop();
    }

    private void simulatePearl(MatrixStack stack, BufferBuilder buffer, EnderPearlEntity pearl) {
        Vec3d motion = pearl.getVelocity();
        Vec3d pos = pearl.getPos();
        int ticks = 0;
        int totalTicks = 0;

        float dist = 0f;
        int baseRGB = ColorUtil.getColorStyle(360) & 0x00FFFFFF;

        // Сначала вычисляем общее время полета
        Vec3d simPos = pos;
        Vec3d simMotion = motion;
        for (int i = 0; i < MAX_STEPS; i++) {
            Vec3d prevSimPos = simPos;
            simPos = simPos.add(simMotion);
            simMotion = getNextMotion(pearl, prevSimPos, simMotion);

            HitResult hitResult = mc.world.raycast(new RaycastContext(prevSimPos, simPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, pearl));
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                simPos = hitResult.getPos();
            }

            totalTicks++;
            if (hitResult.getType() == HitResult.Type.BLOCK || simPos.y < -128) {
                break;
            }
        }

        // Теперь рисуем траекторию и отслеживаем текущее время
        for (int i = 0; i < MAX_STEPS; i++) {
            Vec3d prevPos = pos;
            pos = pos.add(motion);
            motion = getNextMotion(pearl, prevPos, motion);

            HitResult hitResult = mc.world.raycast(new RaycastContext(prevPos, pos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, pearl));
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                pos = hitResult.getPos();
            }

            float segLen = (float) prevPos.distanceTo(pos);
            float a1 = MathUtil.smoothstep(0f, FADE_LEN, dist);
            float a2 = MathUtil.smoothstep(0f, FADE_LEN, dist + segLen);

            int c1 = ColorUtil.withAlpha(baseRGB, a1);
            int c2 = ColorUtil.withAlpha(baseRGB, a2);

            vertexLineGradient(stack, buffer, (float) prevPos.x, (float) prevPos.y, (float) prevPos.z, (float) pos.x, (float) pos.y, (float) pos.z, c1, c2);

            dist += segLen;

            if (hitResult.getType() == HitResult.Type.BLOCK || pos.y < -128) {
                // Сохраняем текущие тики, общее время полета и ссылку на эндер-жемчуг
                pearlPoints.add(new PearlPoint(pos, ticks, totalTicks, pearl));
                break;
            }
            ticks++;
        }
    }

    private void vertexLineGradient(MatrixStack matrices, VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2, int color1, int color2) {
        Matrix4f model = matrices.peek().getPositionMatrix();
        float[] col1 = ColorUtil.rgba(color1);
        float[] col2 = ColorUtil.rgba(color2);
        Vector3f normalVec = ShaderManager.getNormal(x1, y1, z1, x2, y2, z2);

        buffer.vertex(model, x1, y1, z1).color(col1[0], col1[1], col1[2], col1[3]).normal(matrices.peek(), normalVec.x(), normalVec.y(), normalVec.z());
        buffer.vertex(model, x2, y2, z2).color(col2[0], col2[1], col2[2], col2[3]).normal(matrices.peek(), normalVec.x(), normalVec.y(), normalVec.z());
    }

    private int calculateRemainingTicks(EnderPearlEntity pearl) {
        Vec3d motion = pearl.getVelocity();
        Vec3d pos = pearl.getPos();
        int remainingTicks = 0;

        for (int i = 0; i < MAX_STEPS; i++) {
            Vec3d prevPos = pos;
            pos = pos.add(motion);
            motion = getNextMotion(pearl, prevPos, motion);

            HitResult hitResult = mc.world.raycast(new RaycastContext(prevPos, pos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, pearl));
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                pos = hitResult.getPos();
            }

            remainingTicks++;
            if (hitResult.getType() == HitResult.Type.BLOCK || pos.y < -128) {
                break;
            }
        }

        return remainingTicks;
    }

    private Vec3d getNextMotion(ThrownEntity throwable, Vec3d prevPos, Vec3d motion) {
        boolean isInWater = mc.world.getBlockState(BlockPos.ofFloored(prevPos)).getFluidState().isIn(FluidTags.WATER);

        motion = motion.multiply(isInWater ? 0.8 : 0.99);

        if (!throwable.hasNoGravity()) {
            motion = motion.add(0, -0.03F, 0);
        }
        return motion;
    }

    record PearlPoint(Vec3d position, int ticks, int totalTicks, EnderPearlEntity entity) {}
}