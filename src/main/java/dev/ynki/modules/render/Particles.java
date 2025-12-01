package dev.ynki.modules.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import dev.ynki.events.Event;
import dev.ynki.events.impl.EventPacket;
import dev.ynki.events.impl.EventUpdate;
import dev.ynki.events.impl.player.EventAttack;
import dev.ynki.events.impl.render.EventRender3D;
import dev.ynki.manager.IMinecraft;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;
import dev.ynki.modules.setting.BooleanSetting;
import dev.ynki.modules.setting.ModeSetting;
import dev.ynki.modules.setting.MultiSetting;
import dev.ynki.modules.setting.SliderSetting;
import dev.ynki.util.color.ColorUtil;
import dev.ynki.util.math.MathUtil;
import dev.ynki.util.render.RenderUtil;
import dev.ynki.util.render.providers.ResourceProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@FunctionAnnotation(name = "Particles", desc = "Красивые частицы с эффектом свечения", type = Type.Render)
public class Particles extends Function {
    private final MultiSetting types = new MultiSetting(
            "Типы",
            Arrays.asList("Урон", "Мир"),
            new String[]{"Урон", "Мир", "Тотем"}
    );

    private final ModeSetting mode = new ModeSetting("Текстура", "Звезда",
            "Корона",
            "Доллар",
            "Светлячок",
            "Сердце",
            "Молния",
            "Линия",
            "Точка",
            "Ромб",
            "Снежинка",
            "Искра",
            "Звезда",
            "Куб",
            "Гекс"
    );

    private final SliderSetting countDamage = new SliderSetting("Количество при уроне", 20f, 5f, 50f, 1f, () -> types.get("Урон"));
    private final SliderSetting countWorld = new SliderSetting("Количество в мире", 12f, 2f, 15f, 1f, () -> types.get("Мир"));
    private final SliderSetting countTotem = new SliderSetting("Количество при тотеме", 30f, 10f, 100f, 1f, () -> types.get("Тотем"));
    private final SliderSetting sizeDamage = new SliderSetting("Размер при уроне", 0.3f, 0.1f, 0.6f, 0.1f, () -> types.get("Урон"));
    private final SliderSetting sizeWorld = new SliderSetting("Размер в мире", 1.1f, 0.1f, 1.2f, 0.1f, () -> types.get("Мир"));
    private final SliderSetting sizeTotem = new SliderSetting("Размер при тотеме", 0.4f, 0.1f, 0.8f, 0.1f, () -> types.get("Тотем"));
    private final SliderSetting sila = new SliderSetting("Сила разброса", 0.2f, 0.1f, 0.5f, 0.1f);
    private final SliderSetting time = new SliderSetting("Время жизни", 4000f, 500f, 8000f, 100f);
    private final SliderSetting speedMultiplier = new SliderSetting("Скорость", 1.2f, 0.1f, 3f, 0.1f);
    private final BooleanSetting randomRotation = new BooleanSetting("Рандомный поворот", true);
    private final BooleanSetting glowEffect = new BooleanSetting("Эффект свечения", true);
    private final SliderSetting glowIntensity = new SliderSetting("Интенсивность свечения", 2.5f, 1f, 5f, 0.1f, () -> glowEffect.get());

    private final ArrayList<World> worldParticles = new ArrayList<>();
    private final ArrayList<Damage> damageParticles = new ArrayList<>();
    private final ArrayList<Totem> totemParticles = new ArrayList<>();

    public Particles() {
        addSettings(types, mode, countDamage, countWorld, countTotem, sizeDamage, sizeWorld, sizeTotem,
                sila, time, speedMultiplier, randomRotation, glowEffect, glowIntensity);
    }

    private static final Map<String, Identifier> TEXTURES = new HashMap<>();
    static {
        TEXTURES.put("Корона", ResourceProvider.crown);
        TEXTURES.put("Доллар", ResourceProvider.dollar);
        TEXTURES.put("Светлячок", ResourceProvider.firefly);
        TEXTURES.put("Сердце", ResourceProvider.heart);
        TEXTURES.put("Молния", ResourceProvider.lightning);
        TEXTURES.put("Линия", ResourceProvider.line);
        TEXTURES.put("Точка", ResourceProvider.point);
        TEXTURES.put("Ромб", ResourceProvider.rhombus);
        TEXTURES.put("Снежинка", ResourceProvider.snowflake);
        TEXTURES.put("Искра", ResourceProvider.spark);
        TEXTURES.put("Звезда", ResourceProvider.star);
    }

    private boolean isCustomMode() {
        return mode.is("Куб") || mode.is("Гекс");
    }

    @Override
    public void onEvent(Event event) {
        if (types.get("Мир")) {
            if (event instanceof EventUpdate) {
                worldParticles.removeIf(World::tick);
                float con = countWorld.get().floatValue() * 100f;
                for (int j = worldParticles.size(); j < con; j++) {
                    worldParticles.add(new World(
                            (float) (mc.player.getX() + MathUtil.random(-48f, 48f)),
                            (float) (mc.player.getY() + MathUtil.random(2, 48f)),
                            (float) (mc.player.getZ() + MathUtil.random(-48f, 48f)),
                            MathUtil.random(-0.4f, 0.4f),
                            MathUtil.random(-0.1f, 0.1f),
                            MathUtil.random(-0.4f, 0.4f)
                    ));
                }
            }
            if (event instanceof EventRender3D e) {
                renderParticles(e.getMatrixStack(), worldParticles, sizeWorld.get().floatValue(), true);
            }
        }

        if (types.get("Урон")) {
            if (event instanceof EventUpdate) {
                damageParticles.removeIf(Damage::tick);
            } else if (event instanceof EventAttack eventAttack) {
                Entity target = eventAttack.getTarget();
                if (target == null) return;

                double x = target.getX();
                double y = target.getY() + 0.5f;
                double z = target.getZ();
                float con = countDamage.get().floatValue();
                for (int i = 0; i < con; i++) {
                    float motionX = MathUtil.random(-sila.get().floatValue(), sila.get().floatValue()) * speedMultiplier.get().floatValue();
                    float motionZ = MathUtil.random(-sila.get().floatValue(), sila.get().floatValue()) * speedMultiplier.get().floatValue();
                    float motionY = MathUtil.random(-0.05f, -0.1f) * speedMultiplier.get().floatValue();
                    float rotation = randomRotation.get() ? MathUtil.random(0f, 360f) : 0f;
                    int color = ColorUtil.getColorStyle((int) (Math.random() * 360));
                    damageParticles.add(new Damage((float) x, (float) y, (float) z, motionX, motionY, motionZ, time.get().longValue(), color, rotation));
                }
            } else if (event instanceof EventRender3D e) {
                renderParticles(e.getMatrixStack(), damageParticles, sizeDamage.get().floatValue(), false);
            }
        }

        if (types.get("Тотем")) {
            if (event instanceof EventUpdate) {
                totemParticles.removeIf(Totem::tick);
            } else if (event instanceof EventPacket ep && ep.isReceivePacket() && ep.getPacket() instanceof EntityStatusS2CPacket packet) {
                if (packet.getStatus() == EntityStatuses.USE_TOTEM_OF_UNDYING) {
                    Entity entity = packet.getEntity(mc.world);
                    if (entity == null) return;

                    double x = entity.getX();
                    double y = entity.getY() + entity.getHeight() / 2.0;
                    double z = entity.getZ();
                    float con = countTotem.get().floatValue();
                    for (int i = 0; i < con; i++) {
                        float motionX = MathUtil.random(-sila.get().floatValue() * 1.5f, sila.get().floatValue() * 1.5f) * speedMultiplier.get().floatValue();
                        float motionZ = MathUtil.random(-sila.get().floatValue() * 1.5f, sila.get().floatValue() * 1.5f) * speedMultiplier.get().floatValue();
                        float motionY = MathUtil.random(0.1f, 0.3f) * speedMultiplier.get().floatValue();
                        float rotation = randomRotation.get() ? MathUtil.random(0f, 360f) : 0f;
                        int color = ColorUtil.getColorStyle((int) (Math.random() * 360));
                        totemParticles.add(new Totem((float) x, (float) y, (float) z, motionX, motionY, motionZ, time.get().longValue(), color, rotation));
                    }
                }
            } else if (event instanceof EventRender3D e) {
                renderParticles(e.getMatrixStack(), totemParticles, sizeTotem.get().floatValue(), false);
            }
        }
    }

    private <T extends BaseParticle> void renderParticles(MatrixStack matrixStack, ArrayList<T> particles, float baseSize, boolean isWorld) {
        matrixStack.push();

        // Основной рендер
        RenderUtil.enableRender(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        boolean customMode = isCustomMode();
        RenderSystem.setShader(customMode ? ShaderProgramKeys.POSITION_COLOR : ShaderProgramKeys.POSITION_TEX_COLOR);
        if (customMode) RenderSystem.disableCull();

        BufferBuilder bufferBuilder = IMinecraft.tessellator().begin(
                VertexFormat.DrawMode.QUADS,
                customMode ? VertexFormats.POSITION_COLOR : VertexFormats.POSITION_TEXTURE_COLOR
        );

        if (!customMode) {
            Identifier tex = TEXTURES.getOrDefault(mode.get(), TEXTURES.get("Звезда"));
            RenderSystem.setShaderTexture(0, tex);
        }

        particles.forEach(p -> p.render(bufferBuilder, baseSize));
        RenderUtil.render3D.endBuilding(bufferBuilder);

        // Эффект свечения (второй слой)
        if (glowEffect.get()) {
            float glowMult = glowIntensity.get().floatValue();
            RenderSystem.setShader(customMode ? ShaderProgramKeys.POSITION_COLOR : ShaderProgramKeys.POSITION_TEX_COLOR);

            BufferBuilder glowBuffer = IMinecraft.tessellator().begin(
                    VertexFormat.DrawMode.QUADS,
                    customMode ? VertexFormats.POSITION_COLOR : VertexFormats.POSITION_TEXTURE_COLOR
            );

            if (!customMode) {
                Identifier tex = TEXTURES.getOrDefault(mode.get(), TEXTURES.get("Звезда"));
                RenderSystem.setShaderTexture(0, tex);
            }

            particles.forEach(p -> p.render(glowBuffer, baseSize * glowMult));
            RenderUtil.render3D.endBuilding(glowBuffer);
        }

        if (customMode) RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
        matrixStack.pop();
    }

    // Базовый класс для всех частиц
    public abstract class BaseParticle {
        protected float prevposX, prevposY, prevposZ, posX, posY, posZ, motionX, motionY, motionZ;
        protected final float cubeSpinOffset;
        protected final float cubeScale;
        protected float rotation;

        public BaseParticle(float posX, float posY, float posZ, float motionX, float motionY, float motionZ, float rotation) {
            this.posX = this.prevposX = posX;
            this.posY = this.prevposY = posY;
            this.posZ = this.prevposZ = posZ;
            this.motionX = motionX;
            this.motionY = motionY;
            this.motionZ = motionZ;
            this.rotation = rotation;
            this.cubeSpinOffset = rotation + MathUtil.random(-180f, 180f);
            this.cubeScale = MathUtil.random(0.75f, 1.4f);
        }

        protected boolean isSolid(float x, float y, float z) {
            BlockPos pos = new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
            return mc.world != null && mc.world.getBlockState(pos).isFullCube(mc.world, pos);
        }

        protected void tryMove(float dx, float dy, float dz) {
            float newX = posX + dx;
            float newY = posY + dy;
            float newZ = posZ + dz;

            if (!isSolid(newX, posY, posZ)) posX = newX;
            else motionX *= -0.4f;

            if (!isSolid(posX, newY, posZ)) posY = newY;
            else motionY *= -0.4f;

            if (!isSolid(posX, posY, newZ)) posZ = newZ;
            else motionZ *= -0.4f;
        }

        public abstract boolean tick();
        public abstract void render(BufferBuilder bufferBuilder, float size);
        protected abstract int getColor(float alpha);
    }

    public class Damage extends BaseParticle {
        private final long createdTime;
        private final long maxAge;
        private final int baseColor;

        public Damage(float posX, float posY, float posZ, float motionX, float motionY, float motionZ, long maxAge, int color, float rotation) {
            super(posX, posY, posZ, motionX, motionY, motionZ, rotation);
            this.createdTime = System.currentTimeMillis();
            this.maxAge = maxAge;
            this.baseColor = color;
        }

        @Override
        public boolean tick() {
            long now = System.currentTimeMillis();
            if (now - createdTime > maxAge) return true;

            prevposX = posX;
            prevposY = posY;
            prevposZ = posZ;

            for (int i = 0; i < 4; i++) {
                tryMove(motionX / 4, motionY / 4, motionZ / 4);
            }
            motionX *= 0.97f;
            motionY *= 0.97f;
            motionZ *= 0.97f;

            return false;
        }

        @Override
        protected int getColor(float alpha) {
            return RenderUtil.injectAlpha(baseColor, (int) (255 * alpha));
        }

        @Override
        public void render(BufferBuilder bufferBuilder, float size) {
            Camera camera = mc.gameRenderer.getCamera();
            Vec3d pos = RenderUtil.interpolatePos(prevposX, prevposY, prevposZ, posX, posY, posZ);

            MatrixStack matrices = new MatrixStack();
            float pitch = camera.getPitch();
            float yaw = camera.getYaw();
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw + 180.0F));
            matrices.translate(pos.x, pos.y, pos.z);

            float alpha = Math.max(0, 1f - (System.currentTimeMillis() - createdTime) / (float) maxAge);
            int colorWithAlpha = getColor(alpha);

            if (mode.is("Куб")) {
                float time = System.currentTimeMillis() * 0.001f;
                float spin = cubeSpinOffset + (time * 90f); // Более плавное вращение
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(spin));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(spin * 0.65f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(spin * 0.35f)); // Добавляем вращение по Z для более динамичного вида
                renderCube(bufferBuilder, matrices, size * cubeScale, colorWithAlpha);
            } else if (mode.is("Гекс")) {
                float spin = cubeSpinOffset + (System.currentTimeMillis() * 0.1f);
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(spin));
                renderHexagon(bufferBuilder, matrices, size * cubeScale, colorWithAlpha);
            } else {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotation(rotation));
                renderQuad(bufferBuilder, matrices.peek().getPositionMatrix(), size, colorWithAlpha);
            }
        }
    }

    public class World extends BaseParticle {
        protected int age, maxAge;

        public World(float posX, float posY, float posZ, float motionX, float motionY, float motionZ) {
            super(posX, posY, posZ, motionX, motionY, motionZ, 0);
            age = (int) MathUtil.random(100, 300);
            maxAge = age;
        }

        @Override
        public boolean tick() {
            if (mc.player.squaredDistanceTo(posX, posY, posZ) > 4096) age -= 8;
            else age--;

            if (age < 0) return true;

            prevposX = posX;
            prevposY = posY;
            prevposZ = posZ;

            posX += motionX;
            posY += motionY;
            posZ += motionZ;

            motionX *= 0.9f;
            motionY *= 0.9f;
            motionZ *= 0.9f;
            motionY -= 0.001f;

            return false;
        }

        @Override
        protected int getColor(float alpha) {
            int color = ColorUtil.getColorStyle(age * 2);
            return RenderUtil.injectAlpha(color, (int) (255 * alpha));
        }

        @Override
        public void render(BufferBuilder bufferBuilder, float size) {
            Camera camera = mc.gameRenderer.getCamera();
            float alpha = (float) age / (float) maxAge;
            int colorWithAlpha = getColor(alpha);
            Vec3d pos = RenderUtil.interpolatePos(prevposX, prevposY, prevposZ, posX, posY, posZ);

            MatrixStack matrices = new MatrixStack();
            float pitch = camera.getPitch();
            float yaw = camera.getYaw();
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw + 180.0F));
            matrices.translate(pos.x, pos.y, pos.z);

            if (mode.is("Куб")) {
                float time = System.currentTimeMillis() * 0.001f;
                float spin = cubeSpinOffset + (time * 50f) + age * 0.6f;
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(spin));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(spin * 0.5f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(spin * 0.3f));
                renderCube(bufferBuilder, matrices, size * cubeScale, colorWithAlpha);
            } else if (mode.is("Гекс")) {
                float spin = cubeSpinOffset + (System.currentTimeMillis() * 0.05f) + age * 0.4f;
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(spin));
                renderHexagon(bufferBuilder, matrices, size * cubeScale, colorWithAlpha);
            } else {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
                renderQuad(bufferBuilder, matrices.peek().getPositionMatrix(), size, colorWithAlpha);
            }
        }
    }

    public class Totem extends BaseParticle {
        private final long createdTime;
        private final long maxAge;
        private final int baseColor;

        public Totem(float posX, float posY, float posZ, float motionX, float motionY, float motionZ, long maxAge, int color, float rotation) {
            super(posX, posY, posZ, motionX, motionY, motionZ, rotation);
            this.createdTime = System.currentTimeMillis();
            this.maxAge = maxAge;
            this.baseColor = color;
        }

        @Override
        public boolean tick() {
            long now = System.currentTimeMillis();
            if (now - createdTime > maxAge) return true;

            prevposX = posX;
            prevposY = posY;
            prevposZ = posZ;

            for (int i = 0; i < 4; i++) {
                tryMove(motionX / 4, motionY / 4, motionZ / 4);
            }
            motionX *= 0.98f;
            motionY *= 0.98f;
            motionZ *= 0.98f;
            motionY -= 0.01f;

            return false;
        }

        @Override
        protected int getColor(float alpha) {
            return RenderUtil.injectAlpha(baseColor, (int) (255 * alpha));
        }

        @Override
        public void render(BufferBuilder bufferBuilder, float size) {
            Camera camera = mc.gameRenderer.getCamera();
            Vec3d pos = RenderUtil.interpolatePos(prevposX, prevposY, prevposZ, posX, posY, posZ);

            MatrixStack matrices = new MatrixStack();
            float pitch = camera.getPitch();
            float yaw = camera.getYaw();
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw + 180.0F));
            matrices.translate(pos.x, pos.y, pos.z);

            float alpha = Math.max(0, 1f - (System.currentTimeMillis() - createdTime) / (float) maxAge);
            int colorWithAlpha = getColor(alpha);

            if (mode.is("Куб")) {
                float time = System.currentTimeMillis() * 0.001f;
                float spin = cubeSpinOffset + (time * 75f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(spin));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(spin * 0.7f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(spin * 0.4f));
                renderCube(bufferBuilder, matrices, size * cubeScale, colorWithAlpha);
            } else if (mode.is("Гекс")) {
                float spin = cubeSpinOffset + (System.currentTimeMillis() * 0.08f);
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(spin));
                renderHexagon(bufferBuilder, matrices, size * cubeScale, colorWithAlpha);
            } else {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
                renderQuad(bufferBuilder, matrices.peek().getPositionMatrix(), size, colorWithAlpha);
            }
        }
    }

    private void renderQuad(BufferBuilder bufferBuilder, Matrix4f matrix, float size, int color) {
        bufferBuilder.vertex(matrix, 0, -size, 0).texture(0f, 1f).color(color);
        bufferBuilder.vertex(matrix, -size, -size, 0).texture(1f, 1f).color(color);
        bufferBuilder.vertex(matrix, -size, 0, 0).texture(1f, 0).color(color);
        bufferBuilder.vertex(matrix, 0, 0, 0).texture(0, 0).color(color);
    }

    private void renderCube(BufferBuilder bufferBuilder, MatrixStack matrices, float size, int color) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float half = size * 0.5f;
        
        // Улучшенное затенение граней для более реалистичного вида
        int front = shadeColor(color, 1.15f);   // Передняя грань - самая яркая
        int back = shadeColor(color, 0.85f);    // Задняя грань - самая темная
        int top = shadeColor(color, 1.25f);     // Верхняя грань - очень яркая
        int bottom = shadeColor(color, 0.75f);  // Нижняя грань - темная
        int left = shadeColor(color, 0.92f);    // Левая грань
        int right = shadeColor(color, 1.08f);   // Правая грань

        // Передняя грань (Z+)
        addFace(matrix, bufferBuilder,
            -half, -half, half,   // левый нижний
            half, -half, half,    // правый нижний
            half, half, half,      // правый верхний
            -half, half, half,     // левый верхний
            front);
        
        // Задняя грань (Z-)
        addFace(matrix, bufferBuilder,
            half, -half, -half,    // правый нижний
            -half, -half, -half,   // левый нижний
            -half, half, -half,    // левый верхний
            half, half, -half,     // правый верхний
            back);
        
        // Верхняя грань (Y+)
        addFace(matrix, bufferBuilder,
            -half, half, -half,    // левый задний
            half, half, -half,     // правый задний
            half, half, half,       // правый передний
            -half, half, half,      // левый передний
            top);
        
        // Нижняя грань (Y-)
        addFace(matrix, bufferBuilder,
            -half, -half, half,     // левый передний
            half, -half, half,      // правый передний
            half, -half, -half,     // правый задний
            -half, -half, -half,    // левый задний
            bottom);
        
        // Левая грань (X-)
        addFace(matrix, bufferBuilder,
            -half, -half, -half,   // нижний задний
            -half, -half, half,    // нижний передний
            -half, half, half,      // верхний передний
            -half, half, -half,     // верхний задний
            left);
        
        // Правая грань (X+)
        addFace(matrix, bufferBuilder,
            half, -half, half,      // нижний передний
            half, -half, -half,     // нижний задний
            half, half, -half,       // верхний задний
            half, half, half,        // верхний передний
            right);
    }

    private void renderHexagon(BufferBuilder bufferBuilder, MatrixStack matrices, float size, int color) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float angleStep = 60f;

        // Верхние грани
        for (int i = 0; i < 6; i++) {
            float angle1 = (float) Math.toRadians(i * angleStep);
            float angle2 = (float) Math.toRadians((i + 1) * angleStep);

            float x1 = (float) Math.cos(angle1) * size;
            float z1 = (float) Math.sin(angle1) * size;
            float x2 = (float) Math.cos(angle2) * size;
            float z2 = (float) Math.sin(angle2) * size;

            int sideColor = shadeColor(color, 0.9f + i * 0.05f);

            // Верхняя грань
            bufferBuilder.vertex(matrix, 0, size * 0.3f, 0).color(shadeColor(color, 1.2f));
            bufferBuilder.vertex(matrix, x1, size * 0.3f, z1).color(sideColor);
            bufferBuilder.vertex(matrix, x2, size * 0.3f, z2).color(sideColor);
            bufferBuilder.vertex(matrix, 0, size * 0.3f, 0).color(shadeColor(color, 1.2f));

            // Нижняя грань
            bufferBuilder.vertex(matrix, 0, -size * 0.3f, 0).color(shadeColor(color, 0.8f));
            bufferBuilder.vertex(matrix, x2, -size * 0.3f, z2).color(sideColor);
            bufferBuilder.vertex(matrix, x1, -size * 0.3f, z1).color(sideColor);
            bufferBuilder.vertex(matrix, 0, -size * 0.3f, 0).color(shadeColor(color, 0.8f));

            // Боковая грань
            bufferBuilder.vertex(matrix, x1, size * 0.3f, z1).color(sideColor);
            bufferBuilder.vertex(matrix, x1, -size * 0.3f, z1).color(shadeColor(sideColor, 0.85f));
            bufferBuilder.vertex(matrix, x2, -size * 0.3f, z2).color(shadeColor(sideColor, 0.85f));
            bufferBuilder.vertex(matrix, x2, size * 0.3f, z2).color(sideColor);
        }
    }

    private void addFace(Matrix4f matrix, BufferBuilder bufferBuilder, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, int color) {
        bufferBuilder.vertex(matrix, x1, y1, z1).color(color);
        bufferBuilder.vertex(matrix, x2, y2, z2).color(color);
        bufferBuilder.vertex(matrix, x3, y3, z3).color(color);
        bufferBuilder.vertex(matrix, x4, y4, z4).color(color);
    }

    private int shadeColor(int rgba, float k) {
        int r = ColorUtil.getRed(rgba);
        int g = ColorUtil.getGreen(rgba);
        int b = ColorUtil.getBlue(rgba);
        int a = ColorUtil.getAlpha(rgba);
        r = Math.min(255, (int)(r * k));
        g = Math.min(255, (int)(g * k));
        b = Math.min(255, (int)(b * k));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}