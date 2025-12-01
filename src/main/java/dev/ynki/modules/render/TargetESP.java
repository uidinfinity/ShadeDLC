package dev.ynki.modules.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import dev.ynki.events.Event;
import dev.ynki.events.impl.render.EventRender3D;
import dev.ynki.manager.IMinecraft;
import dev.ynki.manager.Manager;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;
import dev.ynki.modules.setting.ModeSetting;
import dev.ynki.util.animations.impl.EaseInOutQuad;
import dev.ynki.util.color.ColorUtil;
import dev.ynki.util.math.RayTraceUtil;
import dev.ynki.util.render.RenderUtil;
import dev.ynki.util.render.providers.ResourceProvider;
import dev.ynki.util.vector.VectorUtil;

import java.awt.*;

import static dev.ynki.util.math.MathUtil.interpolate;
import static dev.ynki.util.math.MathUtil.interpolateFloat;
import static dev.ynki.util.render.RenderUtil.*;

@SuppressWarnings("All")
@FunctionAnnotation(name = "TargetESP", desc = "Красивый указатель на вашем противнике", type = Type.Render)
public class TargetESP extends Function {
    private final ModeSetting mode = new ModeSetting("Мод","Призраки","Маркер","Маркер2","Призраки","Кружок","Кристаллы");

    private final float[] SCALE_CACHE = new float[101];
    private final EaseInOutQuad animation = new EaseInOutQuad(800, 1);
    private Entity lastTarget = null;
    private double scale = 0.0D;
    public TargetESP() {
        addSettings(mode);
        for (int i = 0; i <= 100; i++) SCALE_CACHE[i] = Math.max(0.28f * (i / 100f), 0.2f);
    }

    @Override
    public void onEvent(Event event) {
        if (!(event instanceof EventRender3D renderEvent)) return;
        Entity currentTarget = Manager.FUNCTION_MANAGER.attackAura.target;

        if (currentTarget != null && (lastTarget == null || !lastTarget.equals(currentTarget))) {
            animation.setDirection(net.minecraft.util.math.Direction.AxisDirection.POSITIVE);
            lastTarget = currentTarget;
        } else if (currentTarget == null && lastTarget != null) {
            animation.setDirection(net.minecraft.util.math.Direction.AxisDirection.NEGATIVE);
            // lastTarget остается прежним для плавного исчезновения
        } else if (currentTarget != null) {
            // Если цель появилась снова во время анимации исчезновения, меняем направление на появление
            if (animation.getDirection() == net.minecraft.util.math.Direction.AxisDirection.NEGATIVE) {
                // Анимация идет в отрицательном направлении (исчезновение), меняем на появление
                animation.setDirection(net.minecraft.util.math.Direction.AxisDirection.POSITIVE);
            }
            lastTarget = currentTarget;
        }

        if (currentTarget != null) {
            if (mode.is("Маркер") || mode.is("Маркер2")) {
                render(currentTarget);
            } else if (mode.is("Призраки")) {
                renderGhosts(14, 8, 1.8f, 3f, currentTarget);
            } else if (mode.is("Кружок")) {
                cicle(currentTarget, renderEvent.getMatrixStack(), renderEvent.getDeltatick().getTickDelta(true));
            } else if (mode.is("Кристаллы")) {
                if (currentTarget instanceof LivingEntity) {
                    renderCrystals(renderEvent.getMatrixStack(), renderEvent.getDeltatick().getTickDelta(true), (LivingEntity) currentTarget);
                }
            }
        } else if (mode.is("Кристаллы") && lastTarget instanceof LivingEntity) {
            // Плавное исчезновение кристаллов
            float animProgress = (float) animation.getOutput();
            if (animProgress > 0) {
                renderCrystals(renderEvent.getMatrixStack(), renderEvent.getDeltatick().getTickDelta(true), (LivingEntity) lastTarget);
            } else {
                // Анимация завершена, очищаем lastTarget
                lastTarget = null;
            }
        }
    }
    @Override
    public void onDisable() {
        super.onDisable();
    }

    public void renderGhosts(int espLength, int factor, float shaking, float amplitude, Entity target) {
        if (target == null) return;

        Camera camera = mc.gameRenderer.getCamera();
        if (camera == null) return;
        float hitProgress = RayTraceUtil.getHitProgress(target);
        float delta = mc.getRenderTickCounter().getTickDelta(true);
        Vec3d camPos = camera.getPos();
        double tX = interpolate(target.prevX, target.getX(), delta) - camPos.x;
        double tY = interpolate(target.prevY, target.getY(), delta) - camPos.y;
        double tZ = interpolate(target.prevZ, target.getZ(), delta) - camPos.z;
        float age = interpolateFloat(target.age - 1, target.age, delta);

        boolean canSee = mc.player.canSee(target);

        RenderUtil.enableRender(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShaderTexture(0, ResourceProvider.firefly);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        if (canSee) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
        } else {
            RenderSystem.disableDepthTest();
        }

        BufferBuilder buffer = IMinecraft.tessellator().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        float pitch = camera.getPitch();
        float yaw = camera.getYaw();
        float ghostAlpha = (float) animation.getOutput();

        for (int j = 0; j < 3; j++) {
            for (int i = 0; i <= espLength; i++) {
                float offset = (float) i / espLength;
                double radians = Math.toRadians(((i / 1.5f + age) * factor + j * 120) % (factor * 360));
                double sinQuad = Math.sin(Math.toRadians(age * 2.5f + i * (j + 1)) * amplitude) / shaking;

                MatrixStack matrices = new MatrixStack();
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw + 180f));
                matrices.translate(tX + Math.cos(radians) * target.getWidth(), tY + 1 + sinQuad, tZ + Math.sin(radians) * target.getWidth());
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));

                Matrix4f matrix = matrices.peek().getPositionMatrix();
                int baseColor;
                if (hitProgress > 0) {
                    baseColor = Color.RED.getRGB();
                } else {
                    baseColor = ColorUtil.getColorStyle((int) (180 * offset));
                }

                int color = applyOpacity(baseColor, offset * ghostAlpha);

                float scale = SCALE_CACHE[Math.min((int)(offset * 100), 100)];
                buffer.vertex(matrix, -scale,  scale, 0).texture(0f, 1f).color(color);
                buffer.vertex(matrix,  scale,  scale, 0).texture(1f, 1f).color(color);
                buffer.vertex(matrix,  scale, -scale, 0).texture(1f, 0).color(color);
                buffer.vertex(matrix, -scale, -scale, 0).texture(0f, 0).color(color);
            }
        }
        RenderUtil.render3D.endBuilding(buffer);

        if (canSee) {
            RenderSystem.depthMask(true);
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
        }
        RenderSystem.disableBlend();
    }

    private void cicle(Entity target, MatrixStack matrices, float tickDelta) {
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        double x = MathHelper.lerp(tickDelta, target.lastRenderX, target.getX()) - camPos.x;
        double z = MathHelper.lerp(tickDelta, target.lastRenderZ, target.getZ()) - camPos.z;
        double y = MathHelper.lerp(tickDelta, target.lastRenderY, target.getY()) - camPos.y + Math.min(Math.sin(System.currentTimeMillis() / 400.0) + 0.95, target.getHeight());

        disableDepth();
        RenderUtil.enableRender(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = IMinecraft.tessellator().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        int baseColor = ColorUtil.getColorStyle(360);
        float r = ((baseColor >> 16) & 0xFF) / 255f;
        float g = ((baseColor >> 8) & 0xFF) / 255f;
        float b = (baseColor & 0xFF) / 255f;

        float alpha = (float) animation.getOutput();

        float radius = target.getWidth() * 0.8f;

        for (float i = 0; i <= Math.PI * 2 + (Math.PI * 5 / 100); i += Math.PI * 5 / 100) {
            double vecX = x + radius * Math.cos(i);
            double vecZ = z + radius * Math.sin(i);

            buffer.vertex(matrix, (float) vecX, (float) (y - Math.cos(System.currentTimeMillis() / 400.0) / 2), (float) vecZ).color(r, g, b, 0.01f * alpha);
            buffer.vertex(matrix, (float) vecX, (float) y, (float) vecZ).color(r, g, b, 1f * alpha);
        }

        RenderUtil.render3D.endBuilding(buffer);
        endRender();
    }

    private static void disableDepth() {
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
    }

    private static void endRender() {
        RenderUtil.disableRender();
        RenderSystem.enableDepthTest();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private void render(Entity target) {
        Camera camera = mc.gameRenderer.getCamera();
        if (camera == null) return;

        scale = animation.getOutput();
        if (scale == 0.0) return;

        float delta = mc.getRenderTickCounter().getTickDelta(true);
        float hitProgress = RayTraceUtil.getHitProgress(target);
        Vec3d camPos = camera.getPos();
        double tX = interpolate(target.prevX, target.getX(), delta) - camPos.x;
        double tY = interpolate(target.prevY, target.getY(), delta) - camPos.y;
        double tZ = interpolate(target.prevZ, target.getZ(), delta) - camPos.z;
        MatrixStack matrices = setupMatrices(camera, target, delta, tX, tY, tZ);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        disableDepth();
        RenderUtil.enableRender(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        if (mode.is("Маркер")) {
            RenderSystem.setShaderTexture(0, ResourceProvider.marker);
        }
        if (mode.is("Маркер2")) {
            RenderSystem.setShaderTexture(0, ResourceProvider.marker2);
        }

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        float alpha = (float) animation.getOutput();
        int[] baseColors = hitProgress > 0 ? new int[]{Color.RED.getRGB(), ColorUtil.getColorStyle(0), Color.RED.getRGB(), ColorUtil.getColorStyle(270)} : new int[]{ColorUtil.getColorStyle(90), ColorUtil.getColorStyle(0), ColorUtil.getColorStyle(180), ColorUtil.getColorStyle(270)};

        drawQuad(matrix, applyAlphaToColors(baseColors, alpha));
        endRender();
    }

    private MatrixStack setupMatrices(Camera camera, Entity target, float delta, double tX, double tY, double tZ) {
        MatrixStack matrices = new MatrixStack();
        float pitch = camera.getPitch();
        float yaw = camera.getYaw();

        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw + 180f));
        matrices.translate(tX, tY + target.getEyeHeight(target.getPose()) / 2f, tZ);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));

        float interpolatedAngle = interpolateFloat(1f, 1f, delta);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(interpolatedAngle));

        float radians = (float) Math.toRadians(System.currentTimeMillis() % 3600 / 5f);
        matrices.multiplyPositionMatrix(new Matrix4f().rotate(radians, 0, 0, 1));
        matrices.translate(-0.75, -0.75, -0.01);
        return matrices;
    }

    private int[] applyAlphaToColors(int[] colors, float alpha) {
        int[] out = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            Color color = new Color(colors[i]);
            out[i] = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * alpha)).getRGB();
        }
        return out;
    }

    private void drawQuad(Matrix4f matrix, int[] colors) {
        BufferBuilder buffer = IMinecraft.tessellator().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix,0,1.5f,0).texture(0f,1f).color(colors[0]);
        buffer.vertex(matrix,1.5f,1.5f,0).texture(1f,1f).color(colors[1]);
        buffer.vertex(matrix,1.5f,0,0).texture(1f,0).color(colors[2]);
        buffer.vertex(matrix,0,0,0).texture(0f,0).color(colors[3]);
        RenderUtil.render3D.endBuilding(buffer);
    }

    // Crystal rendering
    private void renderCrystals(MatrixStack matrixStack, float partialTicks, LivingEntity target) {
        float animProgress = (float) animation.getOutput();
        if (animProgress <= 0) return;

        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        Vec3d interpolated = VectorUtil.getInterpolatedPos(target, partialTicks);
        double cx = interpolated.x - camPos.x;
        double cy = interpolated.y + target.getHeight() * 0.55 - camPos.y;
        double cz = interpolated.z - camPos.z;

        renderCrystalRing(matrixStack, partialTicks, target, cx, cy, cz, 0.9, 1.8, 0, animProgress);
        renderCrystalRing(matrixStack, partialTicks, target, cx, cy, cz, 1.1, 2.0, 0.6, animProgress);
    }

    private void renderCrystalRing(MatrixStack matrixStack, float partialTicks, LivingEntity entity, double cx, double cy, double cz, 
                                    double orbitR, double speed, double yOffset, float animProgress) {
        long curMs = System.currentTimeMillis();
        int hue = ColorUtil.getColorStyle((int) ((curMs / 10) % 360));
        // Multiply brightness by increasing RGB values
        int r = ColorUtil.getRed(hue);
        int g = ColorUtil.getGreen(hue);
        int b = ColorUtil.getBlue(hue);
        int a = ColorUtil.getAlpha(hue);
        r = Math.min(255, (int)(r * 1.5f));
        g = Math.min(255, (int)(g * 1.5f));
        b = Math.min(255, (int)(b * 1.5f));
        int lighterThemeColor = new Color(r, g, b, a).getRGB();
        int baseRGB = ColorUtil.applyAlpha(lighterThemeColor, 200f / 255f);

        double t = curMs / 1000.0;
        double pulse = 1.0 + 0.1 * Math.sin(t * 3.0);
        float scale = animProgress;

        Vec3d top = new Vec3d(0, 0.14 * scale, 0);
        Vec3d bottom = new Vec3d(0, -0.14 * scale, 0);
        Vec3d px = new Vec3d(0.14 * scale, 0, 0);
        Vec3d nx = new Vec3d(-0.14 * scale, 0, 0);
        Vec3d pz = new Vec3d(0, 0, 0.14 * scale);
        Vec3d nz = new Vec3d(0, 0, -0.14 * scale);

        Vec3d[][] faces = new Vec3d[][]{
            {top, px, pz}, {top, pz, nx}, {top, nx, nz}, {top, nz, px},
            {bottom, pz, px}, {bottom, nx, pz}, {bottom, nz, nx}, {bottom, px, nz}
        };

        float shellScale = 1.15f;

        disableDepth();
        RenderUtil.enableRender(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        Matrix4f matrix = matrixStack.peek().getPositionMatrix();
        BufferBuilder buffer = IMinecraft.tessellator().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        // Render shell (outer layer)
        for (int i = 0; i < 6; i++) {
            double ang = t * speed + i * (2 * Math.PI / 6.0);
            double ox = cx + orbitR * pulse * Math.cos(ang);
            double oy = cy + yOffset + 0.15 * Math.sin(t * 2.5 + i * 0.8);
            double oz = cz + orbitR * pulse * Math.sin(ang);

            Vec3d crystalPos = new Vec3d(ox, oy, oz);
            Vec3d entityCenter = new Vec3d(cx, cy, cz);

            for (Vec3d[] tri : faces) {
                Vec3d l0 = tri[0].multiply(shellScale);
                Vec3d l1 = tri[1].multiply(shellScale);
                Vec3d l2 = tri[2].multiply(shellScale);
                Vec3d v0 = transformLocalToWorld(crystalPos, entityCenter, l0);
                Vec3d v1 = transformLocalToWorld(crystalPos, entityCenter, l1);
                Vec3d v2 = transformLocalToWorld(crystalPos, entityCenter, l2);

                Vec3d n = v1.subtract(v0).crossProduct(v2.subtract(v0)).normalize();
                Vec3d lightDir = new Vec3d(0.6, 1.0, 0.4).normalize();
                double ndl = Math.max(0.1, n.dotProduct(lightDir));
                int faceColor = shadeColor(baseRGB, (float) ndl);
                int shellColor = ColorUtil.applyAlpha(faceColor, 0.3f * animProgress);

                float shellA = (float) (shellColor >> 24 & 255) / 255.0F;
                float shellR = (float) (shellColor >> 16 & 255) / 255.0F;
                float shellG = (float) (shellColor >> 8 & 255) / 255.0F;
                float shellB = (float) (shellColor & 255) / 255.0F;

                buffer.vertex(matrix, (float)v0.x, (float)v0.y, (float)v0.z).color(shellR, shellG, shellB, shellA);
                buffer.vertex(matrix, (float)v1.x, (float)v1.y, (float)v1.z).color(shellR, shellG, shellB, shellA);
                buffer.vertex(matrix, (float)v2.x, (float)v2.y, (float)v2.z).color(shellR, shellG, shellB, shellA);
            }
        }

        // Render main crystals
        for (int i = 0; i < 6; i++) {
            double ang = t * speed + i * (2 * Math.PI / 6.0);
            double ox = cx + orbitR * pulse * Math.cos(ang);
            double oy = cy + yOffset + 0.15 * Math.sin(t * 2.5 + i * 0.8);
            double oz = cz + orbitR * pulse * Math.sin(ang);

            Vec3d crystalPos = new Vec3d(ox, oy, oz);
            Vec3d entityCenter = new Vec3d(cx, cy, cz);

            for (Vec3d[] tri : faces) {
                Vec3d l0 = tri[0];
                Vec3d l1 = tri[1];
                Vec3d l2 = tri[2];
                Vec3d v0 = transformLocalToWorld(crystalPos, entityCenter, l0);
                Vec3d v1 = transformLocalToWorld(crystalPos, entityCenter, l1);
                Vec3d v2 = transformLocalToWorld(crystalPos, entityCenter, l2);

                Vec3d n = v1.subtract(v0).crossProduct(v2.subtract(v0)).normalize();
                Vec3d lightDir = new Vec3d(0.6, 1.0, 0.4).normalize();
                double ndl = Math.max(0.1, n.dotProduct(lightDir));
                int faceColor = shadeColor(baseRGB, (float) ndl);
                faceColor = ColorUtil.applyAlpha(faceColor, animProgress);

                float faceA = (float) (faceColor >> 24 & 255) / 255.0F;
                float faceR = (float) (faceColor >> 16 & 255) / 255.0F;
                float faceG = (float) (faceColor >> 8 & 255) / 255.0F;
                float faceB = (float) (faceColor & 255) / 255.0F;

                buffer.vertex(matrix, (float)v0.x, (float)v0.y, (float)v0.z).color(faceR, faceG, faceB, faceA);
                buffer.vertex(matrix, (float)v1.x, (float)v1.y, (float)v1.z).color(faceR, faceG, faceB, faceA);
                buffer.vertex(matrix, (float)v2.x, (float)v2.y, (float)v2.z).color(faceR, faceG, faceB, faceA);
            }
        }

        RenderUtil.render3D.endBuilding(buffer);
        endRender();
    }

    private Vec3d transformLocalToWorld(Vec3d crystalPos, Vec3d targetPos, Vec3d localPos) {
        Vec3d modelUp = targetPos.subtract(crystalPos);
        if (modelUp.lengthSquared() < 0.001) {
            modelUp = new Vec3d(0, 1, 0);
        }
        modelUp = modelUp.normalize();

        Vec3d modelRight = new Vec3d(0, 1, 0).crossProduct(modelUp);
        if (modelRight.lengthSquared() < 0.001) {
            modelRight = new Vec3d(1, 0, 0).crossProduct(modelUp);
        }
        modelRight = modelRight.normalize();

        Vec3d modelForward = modelUp.crossProduct(modelRight).normalize();

        double worldX = crystalPos.x + modelRight.x * localPos.x + modelUp.x * localPos.y + modelForward.x * localPos.z;
        double worldY = crystalPos.y + modelRight.y * localPos.x + modelUp.y * localPos.y + modelForward.y * localPos.z;
        double worldZ = crystalPos.z + modelRight.z * localPos.x + modelUp.z * localPos.y + modelForward.z * localPos.z;

        return new Vec3d(worldX, worldY, worldZ);
    }

    private int shadeColor(int rgba, float k) {
        int r = ColorUtil.getRed(rgba);
        int g = ColorUtil.getGreen(rgba);
        int b = ColorUtil.getBlue(rgba);
        int a = ColorUtil.getAlpha(rgba);
        r = Math.min(255, (int)(r * k));
        g = Math.min(255, (int)(g * k));
        b = Math.min(255, (int)(b * k));
        return new Color(r, g, b, a).getRGB();
    }

}