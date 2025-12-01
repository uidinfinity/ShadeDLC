package dev.ynki.modules.movement;

import dev.ynki.events.Event;
import dev.ynki.events.impl.EventUpdate;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

@FunctionAnnotation(name = "ShulkerBoost", desc = "Прыгает выше используя шалкеры", type = Type.Move)
public class ShulkerBoost extends Function {

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventUpdate) {
            boostFromShulkers();
        }
    }

    private void boostFromShulkers() {
        if (mc.player == null || mc.world == null) return;

        ChunkPos playerChunk = new ChunkPos(BlockPos.ofFloored(mc.player.getX(), mc.player.getY(), mc.player.getZ()));

        for (int cx = -1; cx <= 1; cx++) {
            for (int cz = -1; cz <= 1; cz++) {
                WorldChunk chunk = mc.world.getChunk(playerChunk.x + cx, playerChunk.z + cz);
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof ShulkerBoxBlockEntity shulker)) continue;

                    BlockPos pos = shulker.getPos();
                    double dx = mc.player.getX() - (pos.getX() + 0.5);
                    double dz = mc.player.getZ() - (pos.getZ() + 0.5);
                    double distance = Math.sqrt(dx * dx + dz * dz);

                    double yDiff = Math.abs(mc.player.getY() - (pos.getY() + 0.5));
                    double allowedY = mc.player.getVelocity().y > 1 ? 30 : 2;

                    if (distance <= 1.0 && yDiff <= allowedY && mc.player.fallDistance == 0) {
                        float progress = shulker.getAnimationProgress(mc.getRenderTickCounter().getTickDelta(true));
                        if (progress > 0f && progress < 1f) {
                            mc.player.setVelocity(mc.player.getVelocity().x, 1.5, mc.player.getVelocity().z);
                            return;
                        }
                    }
                }
            }
        }
    }
}
