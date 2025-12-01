package dev.ynki.modules.misc;


import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import dev.ynki.modules.setting.BooleanSetting;
import dev.ynki.modules.setting.SliderSetting;
import dev.ynki.events.Event;
import dev.ynki.events.impl.render.EventRender3D;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;
import dev.ynki.util.render.RenderUtil;

import java.awt.*;


@FunctionAnnotation(name = "Xray", desc = "Иксрей", type = Type.Misc)
public class Xray extends Function {
    public static SliderSetting radius = new SliderSetting("Радиус", 20f, 1f, 30f, 1f);
    public static BooleanSetting ancient = new BooleanSetting("Незерит", true);

    public static BooleanSetting diamond = new BooleanSetting("Алмазы", true);

    public static BooleanSetting emerald = new BooleanSetting("Изумруды", true);

    public static BooleanSetting gold = new BooleanSetting("Золото", true);

    public static BooleanSetting iron = new BooleanSetting("Железо", true);

    public static BooleanSetting coal = new BooleanSetting("Уголь", true);

    public static BooleanSetting redstone = new BooleanSetting("Редстоун", true);

    public static BooleanSetting lapise = new BooleanSetting("Лазурит", true);


    public Xray() {
        addSettings(radius,
                ancient,
                diamond,
                emerald,
                gold,
                iron,
                coal,
                redstone,
                lapise);
    }
    @Override
    public void onEvent(Event event) {
        if (event instanceof EventRender3D e) {
            for (int x = (int) (mc.player.getX() - 30); x <= mc.player.getX() + radius.get().floatValue(); x++) {
                for (int y = (int) (mc.player.getY() - 30); y <= mc.player.getY() + radius.get().floatValue(); y++) {
                    for (int z = (int) (mc.player.getZ() - 30); z <= mc.player.getZ() + radius.get().floatValue(); z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = mc.world.getBlockState(pos);
                        Box box = new Box(pos).contract(0.01);
                        MatrixStack poseStack = new MatrixStack();
                        if (ancient.get()) {
                            if (state.getBlock() == Blocks.ANCIENT_DEBRIS) {
                                RenderUtil.render3D.drawHoleOutline(box, Color.green.getRGB(),2);
                            }
                        }
                        if (diamond.get()) {
                            if (state.getBlock() == Blocks.DIAMOND_ORE) {
                                RenderUtil.render3D.drawHoleOutline(box, new Color(0, 255, 255,80).getRGB(),2);
                            }
                        }
                        if (emerald.get()) {
                            if (state.getBlock() == Blocks.EMERALD_ORE) {
                                RenderUtil.render3D.drawHoleOutline(box, new Color(0, 128, 0,80).getRGB(),2);
                            }
                        }
                        if (gold.get()) {
                            if (state.getBlock() == Blocks.GOLD_ORE) {
                                RenderUtil.render3D.drawHoleOutline(box, new Color(255, 255, 0,80).getRGB(),2);
                            }
                        }
                        if (iron.get()) {
                            if (state.getBlock() == Blocks.IRON_ORE) {
                                RenderUtil.render3D.drawHoleOutline(box, new Color(	192, 192, 192,80).getRGB(),2);
                            }
                        }
                        if (coal.get()) {
                            if (state.getBlock() == Blocks.COAL_ORE) {
                                RenderUtil.render3D.drawHoleOutline(box, new Color(	0, 0, 0,80).getRGB(),2);
                            }
                        }
                        if (redstone.get()) {
                            if (state.getBlock() == Blocks.REDSTONE_ORE) {
                                RenderUtil.render3D.drawHoleOutline(box, new Color(	255, 0, 0,80).getRGB(),2);
                            }
                        }
                        if (lapise.get()) {
                            if (state.getBlock() == Blocks.LAPIS_ORE) {
                                RenderUtil.render3D.drawHoleOutline(box, new Color(	0, 0, 255,80).getRGB(),2);
                            }
                        }
                    }
                }
            }
        }
    }
}
