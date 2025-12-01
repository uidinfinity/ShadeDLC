package dev.ynki.modules.render;

import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import dev.ynki.events.Event;
import dev.ynki.events.impl.render.EventRender3D;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;
import dev.ynki.util.color.ColorUtil;
import dev.ynki.util.render.Render3DUtil;

@FunctionAnnotation(name = "BlockHighLight", desc = "Подсвечивает текущий блок, на который ты навёлся", type = Type.Render)
public class BlockHighLight extends Function {

    @Override
    public void onEvent(Event event) {
        if (!(event instanceof EventRender3D e)) return;
        if (!(mc.crosshairTarget instanceof BlockHitResult result)) return;
        if (result.getType() != HitResult.Type.BLOCK) return;
        BlockPos pos = result.getBlockPos();
        if (pos == null) return;
        Render3DUtil.drawShapeAlternative(pos, mc.world.getBlockState(pos).getOutlineShape(mc.world, pos), ColorUtil.getColorStyle(360), 2, true, true);

    }
}
