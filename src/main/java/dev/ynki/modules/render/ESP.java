package dev.ynki.modules.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector4d;
import dev.ynki.manager.IMinecraft;
import dev.ynki.modules.setting.BooleanSetting;
import dev.ynki.modules.setting.MultiSetting;
import dev.ynki.events.Event;
import dev.ynki.events.impl.render.EventRender2D;
import dev.ynki.events.impl.render.EventRender3D;
import dev.ynki.manager.Manager;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;
import dev.ynki.util.color.ColorUtil;
import dev.ynki.util.render.RenderUtil;
import dev.ynki.util.vector.VectorUtil;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("All")
@FunctionAnnotation(name = "ESP", desc = "Красивые квадраты на игроках", type = Type.Render)
public class ESP extends Function {

    private final MultiSetting targets = new MultiSetting(
            "Отображать",
            Arrays.asList("Игроков", "Друзей", "Меня"),
            new String[]{"Игроков", "Друзей", "Меня", "Предметы"}
    );

    // 3D ESP settings
    private final BooleanSetting trideESP = new BooleanSetting("3D ESP", false);

    public ESP() {
        addSettings(targets, trideESP);
    }

    @Override
    public void onEvent(Event event) {
        // Handle 3D ESP
        if (trideESP.get() && event instanceof EventRender3D e) {
            render3DESP(e);
            return;
        }
        
        // Handle 2D ESP
        if (!(event instanceof EventRender2D e)) return;
        if (mc.options.hudHidden) return;

        Matrix4f matrix = e.getDrawContext().getMatrices().peek().getPositionMatrix();

        RenderUtil.enableRender();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buffer = IMinecraft.tessellator().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        List<AbstractClientPlayerEntity> players = Manager.SYNC_MANAGER.getPlayers();
        List<Entity> entities = targets.get("Предметы") ? Manager.SYNC_MANAGER.getEntities() : List.of();

        for (PlayerEntity player : players) {
            if (shouldRender(player)) {
                drawBox(e.getDeltatick(), buffer, player, matrix);
            }
        }

        for (Entity entity : entities) {
            if (entity instanceof ItemEntity) {
                drawBox(e.getDeltatick(), buffer, entity, matrix);
            }
        }

        RenderUtil.render3D.endBuilding(buffer);
        RenderUtil.disableRender();
    }

    private boolean shouldRender(PlayerEntity entity) {
        if (entity == mc.player) {
            if (mc.options.getPerspective() == Perspective.FIRST_PERSON) return false;
            return targets.get("Меня");
        }
        if (targets.get("Друзей") && Manager.FRIEND_MANAGER.isFriend(entity.getName().getString())) {
            return true;
        }
        return targets.get("Игроков");
    }

    public void drawBox(RenderTickCounter tick, BufferBuilder buffer, @NotNull Entity ent, Matrix4f matrix) {
        Vec3d[] corners = getVectors(tick, ent);

        Vector4d pos = null;
        for (Vec3d corner : corners) {
            Vec3d screen = RenderUtil.render3D.worldSpaceToScreenSpace(corner);
            if (screen.z <= 0 || screen.z >= 1) continue;

            if (pos == null) pos = new Vector4d(screen.x, screen.y, screen.x, screen.y);
            else {
                if (screen.x < pos.x) pos.x = screen.x;
                if (screen.y < pos.y) pos.y = screen.y;
                if (screen.x > pos.z) pos.z = screen.x;
                if (screen.y > pos.w) pos.w = screen.y;
            }
        }

        if (pos == null) return;

        double screenW = mc.getWindow().getScaledWidth();
        double screenH = mc.getWindow().getScaledHeight();
        if (pos.z < 0 || pos.x > screenW || pos.w < 0 || pos.y > screenH) return;

        float x1 = (float) pos.x;
        float y1 = (float) pos.y;
        float x2 = (float) pos.z;
        float y2 = (float) pos.w;

        int black = Color.BLACK.getRGB();

        drawRect(buffer, matrix, x1 - 1f, y1, x1 + 0.5f, y2 + 0.5f, black);
        drawRect(buffer, matrix, x1 - 1f, y1 - 0.5f, x2 + 0.5f, y1 + 1f, black);
        drawRect(buffer, matrix, x2 - 1f, y1, x2 + 0.5f, y2 + 0.5f, black);
        drawRect(buffer, matrix, x1 - 1f, y2 - 1f, x2 + 0.5f, y2 + 0.5f, black);

        int cTop = ColorUtil.getColorStyle(270);
        int cRight = ColorUtil.getColorStyle(90);
        int cBottom = ColorUtil.getColorStyle(180);
        int cLeft = ColorUtil.getColorStyle(0);

        drawRect(buffer, matrix, x1 - 0.5f, y1, x1 + 0.5f, y2, cTop, cLeft, cLeft, cTop);
        drawRect(buffer, matrix, x1, y2 - 0.5f, x2, y2, cLeft, cBottom, cBottom, cLeft);
        drawRect(buffer, matrix, x1 - 0.5f, y1, x2, y1 + 0.5f, cBottom, cRight, cRight, cBottom);
        drawRect(buffer, matrix, x2 - 0.5f, y1, x2, y2, cRight, cTop, cTop, cRight);
    }

    private void drawRect(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float x2, float y2, int c1) {
        buffer.vertex(matrix, x1, y2, 0f).color(c1);
        buffer.vertex(matrix, x2, y2, 0f).color(c1);
        buffer.vertex(matrix, x2, y1, 0f).color(c1);
        buffer.vertex(matrix, x1, y1, 0f).color(c1);
    }

    private void drawRect(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float x2, float y2,
                          int c1, int c2, int c3, int c4) {
        buffer.vertex(matrix, x1, y2, 0f).color(c1);
        buffer.vertex(matrix, x2, y2, 0f).color(c2);
        buffer.vertex(matrix, x2, y1, 0f).color(c3);
        buffer.vertex(matrix, x1, y1, 0f).color(c4);
    }

    @NotNull
    private Vec3d[] getVectors(RenderTickCounter tick, @NotNull Entity ent) {
        double x = ent.prevX + (ent.getX() - ent.prevX) * tick.getTickDelta(true);
        double y = ent.prevY + (ent.getY() - ent.prevY) * tick.getTickDelta(true);
        double z = ent.prevZ + (ent.getZ() - ent.prevZ) * tick.getTickDelta(true);

        Box bb = ent.getBoundingBox();
        double dx = bb.minX - ent.getX() + x;
        double dy = bb.minY - ent.getY() + y;
        double dz = bb.minZ - ent.getZ() + z;
        double dx2 = bb.maxX - ent.getX() + x;
        double dy2 = bb.maxY - ent.getY() + y;
        double dz2 = bb.maxZ - ent.getZ() + z;

        return new Vec3d[]{
                new Vec3d(dx - 0.05, dy, dz - 0.05),
                new Vec3d(dx - 0.05, dy2 + 0.15, dz - 0.05),
                new Vec3d(dx2 + 0.05, dy, dz - 0.05),
                new Vec3d(dx2 + 0.05, dy2 + 0.15, dz - 0.05),
                new Vec3d(dx - 0.05, dy, dz2 + 0.05),
                new Vec3d(dx - 0.05, dy2 + 0.15, dz2 + 0.05),
                new Vec3d(dx2 + 0.05, dy, dz2 + 0.05),
                new Vec3d(dx2 + 0.05, dy2 + 0.15, dz2 + 0.05)
        };
    }

    // 3D ESP rendering
    private void render3DESP(EventRender3D event) {
        MatrixStack.Entry entry = event.getMatrixStack().peek();

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        List<AbstractClientPlayerEntity> players = Manager.SYNC_MANAGER.getPlayers();

        for (PlayerEntity player : players) {
            if (!shouldRender3D(player)) continue;
            if (!player.isAlive()) continue;

            Vec3d camPos = mc.gameRenderer.getCamera().getPos();
            Vec3d position = VectorUtil.getInterpolatedPos(player, event.getDeltatick().getTickDelta(true))
                    .subtract(camPos);

            // Fixed fill alpha: 0.85
            float fillAlphaValue = 0.85f;
            int fillColor = ColorUtil.applyAlpha(Color.WHITE.getRGB(), (int)(fillAlphaValue * 255));

            // Always draw filled box
            BufferBuilder buffer = IMinecraft.tessellator().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            drawFilledBox(entry, buffer, position, player.getHeight(), fillColor);
            RenderUtil.render3D.endBuilding(buffer);

            // Fixed line width: 1.0
            RenderSystem.lineWidth(1.0f);
            buffer = IMinecraft.tessellator().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);
            drawColorfulLines(entry, buffer, position, player.getHeight());
            RenderUtil.render3D.endBuilding(buffer);
        }

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private boolean shouldRender3D(PlayerEntity entity) {
        if (entity == mc.player) {
            if (mc.options.getPerspective() == Perspective.FIRST_PERSON) return false;
            return targets.get("Меня");
        }
        if (Manager.FUNCTION_MANAGER.antiBot != null && Manager.FUNCTION_MANAGER.antiBot.check(entity)) {
            return false;
        }
        if (targets.get("Друзей") && Manager.FRIEND_MANAGER.isFriend(entity.getName().getString())) {
            return true;
        }
        return targets.get("Игроков");
    }

    private void drawFilledBox(MatrixStack.Entry entry, BufferBuilder buffer, Vec3d position, double height, int color) {
        double minX = position.x - 0.33;
        double maxX = position.x + 0.33;
        double minY = position.y;
        double maxY = position.y + height;
        double minZ = position.z - 0.33;
        double maxZ = position.z + 0.33;

        // Bottom face
        buffer.vertex(entry, (float)minX, (float)minY, (float)minZ).color(color);
        buffer.vertex(entry, (float)maxX, (float)minY, (float)minZ).color(color);
        buffer.vertex(entry, (float)maxX, (float)minY, (float)maxZ).color(color);
        buffer.vertex(entry, (float)minX, (float)minY, (float)maxZ).color(color);

        // Top face
        buffer.vertex(entry, (float)minX, (float)maxY, (float)maxZ).color(color);
        buffer.vertex(entry, (float)maxX, (float)maxY, (float)maxZ).color(color);
        buffer.vertex(entry, (float)maxX, (float)maxY, (float)minZ).color(color);
        buffer.vertex(entry, (float)minX, (float)maxY, (float)minZ).color(color);

        // North face
        buffer.vertex(entry, (float)minX, (float)minY, (float)maxZ).color(color);
        buffer.vertex(entry, (float)maxX, (float)minY, (float)maxZ).color(color);
        buffer.vertex(entry, (float)maxX, (float)maxY, (float)maxZ).color(color);
        buffer.vertex(entry, (float)minX, (float)maxY, (float)maxZ).color(color);

        // South face
        buffer.vertex(entry, (float)minX, (float)maxY, (float)minZ).color(color);
        buffer.vertex(entry, (float)maxX, (float)maxY, (float)minZ).color(color);
        buffer.vertex(entry, (float)maxX, (float)minY, (float)minZ).color(color);
        buffer.vertex(entry, (float)minX, (float)minY, (float)minZ).color(color);

        // West face
        buffer.vertex(entry, (float)minX, (float)minY, (float)minZ).color(color);
        buffer.vertex(entry, (float)minX, (float)minY, (float)maxZ).color(color);
        buffer.vertex(entry, (float)minX, (float)maxY, (float)maxZ).color(color);
        buffer.vertex(entry, (float)minX, (float)maxY, (float)minZ).color(color);

        // East face
        buffer.vertex(entry, (float)maxX, (float)maxY, (float)minZ).color(color);
        buffer.vertex(entry, (float)maxX, (float)maxY, (float)maxZ).color(color);
        buffer.vertex(entry, (float)maxX, (float)minY, (float)maxZ).color(color);
        buffer.vertex(entry, (float)maxX, (float)minY, (float)minZ).color(color);
    }

    private void drawSquare(MatrixStack.Entry entry, BufferBuilder buffer, Vec3d position, double height, int color) {
        // Top square
        buffer.vertex(entry, (float)(position.x - 0.33), (float)(position.y + height), (float)(position.z - 0.33)).color(color);
        buffer.vertex(entry, (float)(position.x + 0.33), (float)(position.y + height), (float)(position.z - 0.33)).color(color);
        
        buffer.vertex(entry, (float)(position.x + 0.33), (float)(position.y + height), (float)(position.z - 0.33)).color(color);
        buffer.vertex(entry, (float)(position.x + 0.33), (float)(position.y + height), (float)(position.z + 0.33)).color(color);
        
        buffer.vertex(entry, (float)(position.x + 0.33), (float)(position.y + height), (float)(position.z + 0.33)).color(color);
        buffer.vertex(entry, (float)(position.x - 0.33), (float)(position.y + height), (float)(position.z + 0.33)).color(color);
        
        buffer.vertex(entry, (float)(position.x - 0.33), (float)(position.y + height), (float)(position.z + 0.33)).color(color);
        buffer.vertex(entry, (float)(position.x - 0.33), (float)(position.y + height), (float)(position.z - 0.33)).color(color);

        // Bottom square
        buffer.vertex(entry, (float)(position.x - 0.33), (float)position.y, (float)(position.z - 0.33)).color(color);
        buffer.vertex(entry, (float)(position.x - 0.33), (float)position.y, (float)(position.z + 0.33)).color(color);
        
        buffer.vertex(entry, (float)(position.x + 0.33), (float)position.y, (float)(position.z + 0.33)).color(color);
        buffer.vertex(entry, (float)(position.x + 0.33), (float)position.y, (float)(position.z - 0.33)).color(color);
        
        buffer.vertex(entry, (float)(position.x + 0.33), (float)position.y, (float)(position.z - 0.33)).color(color);
        buffer.vertex(entry, (float)(position.x - 0.33), (float)position.y, (float)(position.z - 0.33)).color(color);
        
        buffer.vertex(entry, (float)(position.x - 0.33), (float)position.y, (float)(position.z + 0.33)).color(color);
        buffer.vertex(entry, (float)(position.x + 0.33), (float)position.y, (float)(position.z + 0.33)).color(color);
    }

    private void drawVerticalLines(MatrixStack.Entry entry, BufferBuilder buffer, Vec3d position, double height, int color) {
        buffer.vertex(entry, (float)(position.x - 0.33), (float)position.y, (float)(position.z - 0.33)).color(color);
        buffer.vertex(entry, (float)(position.x - 0.33), (float)(position.y + height), (float)(position.z - 0.33)).color(color);

        buffer.vertex(entry, (float)(position.x + 0.33), (float)position.y, (float)(position.z - 0.33)).color(color);
        buffer.vertex(entry, (float)(position.x + 0.33), (float)(position.y + height), (float)(position.z - 0.33)).color(color);

        buffer.vertex(entry, (float)(position.x + 0.33), (float)position.y, (float)(position.z + 0.33)).color(color);
        buffer.vertex(entry, (float)(position.x + 0.33), (float)(position.y + height), (float)(position.z + 0.33)).color(color);

        buffer.vertex(entry, (float)(position.x - 0.33), (float)position.y, (float)(position.z + 0.33)).color(color);
        buffer.vertex(entry, (float)(position.x - 0.33), (float)(position.y + height), (float)(position.z + 0.33)).color(color);
    }

    private void drawColorfulLines(MatrixStack.Entry entry, BufferBuilder buffer, Vec3d position, double height) {
        int cTop = ColorUtil.getColorStyle(270);
        int cRight = ColorUtil.getColorStyle(90);
        int cBottom = ColorUtil.getColorStyle(180);
        int cLeft = ColorUtil.getColorStyle(0);

        // Top square
        buffer.vertex(entry, (float)(position.x - 0.33), (float)(position.y + height), (float)(position.z - 0.33)).color(cTop);
        buffer.vertex(entry, (float)(position.x + 0.33), (float)(position.y + height), (float)(position.z - 0.33)).color(cTop);

        buffer.vertex(entry, (float)(position.x + 0.33), (float)(position.y + height), (float)(position.z - 0.33)).color(cRight);
        buffer.vertex(entry, (float)(position.x + 0.33), (float)(position.y + height), (float)(position.z + 0.33)).color(cRight);

        buffer.vertex(entry, (float)(position.x + 0.33), (float)(position.y + height), (float)(position.z + 0.33)).color(cBottom);
        buffer.vertex(entry, (float)(position.x - 0.33), (float)(position.y + height), (float)(position.z + 0.33)).color(cBottom);

        buffer.vertex(entry, (float)(position.x - 0.33), (float)(position.y + height), (float)(position.z + 0.33)).color(cLeft);
        buffer.vertex(entry, (float)(position.x - 0.33), (float)(position.y + height), (float)(position.z - 0.33)).color(cLeft);

        // Bottom square
        buffer.vertex(entry, (float)(position.x - 0.33), (float)position.y, (float)(position.z - 0.33)).color(cLeft);
        buffer.vertex(entry, (float)(position.x - 0.33), (float)position.y, (float)(position.z + 0.33)).color(cLeft);

        buffer.vertex(entry, (float)(position.x + 0.33), (float)position.y, (float)(position.z + 0.33)).color(cBottom);
        buffer.vertex(entry, (float)(position.x + 0.33), (float)position.y, (float)(position.z - 0.33)).color(cBottom);

        buffer.vertex(entry, (float)(position.x + 0.33), (float)position.y, (float)(position.z - 0.33)).color(cRight);
        buffer.vertex(entry, (float)(position.x - 0.33), (float)position.y, (float)(position.z - 0.33)).color(cRight);

        buffer.vertex(entry, (float)(position.x - 0.33), (float)position.y, (float)(position.z + 0.33)).color(cTop);
        buffer.vertex(entry, (float)(position.x + 0.33), (float)position.y, (float)(position.z + 0.33)).color(cTop);

        // Vertical lines
        buffer.vertex(entry, (float)(position.x - 0.33), (float)position.y, (float)(position.z - 0.33)).color(cLeft);
        buffer.vertex(entry, (float)(position.x - 0.33), (float)(position.y + height), (float)(position.z - 0.33)).color(cTop);

        buffer.vertex(entry, (float)(position.x + 0.33), (float)position.y, (float)(position.z - 0.33)).color(cRight);
        buffer.vertex(entry, (float)(position.x + 0.33), (float)(position.y + height), (float)(position.z - 0.33)).color(cTop);

        buffer.vertex(entry, (float)(position.x + 0.33), (float)position.y, (float)(position.z + 0.33)).color(cRight);
        buffer.vertex(entry, (float)(position.x + 0.33), (float)(position.y + height), (float)(position.z + 0.33)).color(cBottom);

        buffer.vertex(entry, (float)(position.x - 0.33), (float)position.y, (float)(position.z + 0.33)).color(cLeft);
        buffer.vertex(entry, (float)(position.x - 0.33), (float)(position.y + height), (float)(position.z + 0.33)).color(cBottom);
    }
}
