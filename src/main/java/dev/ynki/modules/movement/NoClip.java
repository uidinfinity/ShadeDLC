package dev.ynki.modules.movement;

import net.minecraft.block.BlockState;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import dev.ynki.events.Event;
import dev.ynki.events.impl.EventPacket;
import dev.ynki.events.impl.EventUpdate;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;
import dev.ynki.modules.setting.SliderSetting;
import dev.ynki.util.move.NetworkUtils;
import dev.ynki.util.player.TimerUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@FunctionAnnotation(name = "NoClip", type = Type.Move, desc = "Позволяет пройти через стены")
public class NoClip extends Function {
    private final List<Packet<?>> bufferedPackets = new ArrayList<>();
    private final SliderSetting semiPackets = new SliderSetting("Кол-во попыток", 5.0F, 1.0F, 10.0F, 1.0F);
    private final TimerUtil antiKickTimer = new TimerUtil();
    private final TimerUtil microExitTimer = new TimerUtil();
    private boolean semiPacketSent;
    private int ticksInBlock;
    private int antiKickTicks;

    public NoClip() {
        this.addSettings(semiPackets);
    }

    @Override
    public void onEvent(Event event) {
        if (mc.player == null || mc.world == null) return;

        if (event instanceof EventPacket) {
            this.onPacket((EventPacket) event);
        } else if (event instanceof EventUpdate) {
            this.eventUpdate((EventUpdate) event);
        }
    }

    public void onPacket(EventPacket eventPacket) {
        if (mc.player != null && mc.player.networkHandler != null) {
            Packet<?> packet = eventPacket.getPacket();

            if (packet instanceof PlayerMoveC2SPacket) {
                this.bufferedPackets.add(packet);
                eventPacket.setCancel(true);
            }
        }
    }

    public void eventUpdate(EventUpdate eventPacket) {
        if (mc.player != null && mc.world != null) {
            Box playerBox = mc.player.getBoundingBox().expand(-0.001D);
            
            long totalStates = 0;
            long solidStates = 0;

            int minX = MathHelper.floor(playerBox.minX);
            int minY = MathHelper.floor(playerBox.minY);
            int minZ = MathHelper.floor(playerBox.minZ);
            int maxX = MathHelper.floor(playerBox.maxX);
            int maxY = MathHelper.floor(playerBox.maxY);
            int maxZ = MathHelper.floor(playerBox.maxZ);

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = mc.world.getBlockState(pos);
                        totalStates++;
                        if (state.isSolidBlock(mc.world, pos)) {
                            solidStates++;
                        }
                    }
                }
            }

            boolean noSolidInAABB = solidStates == 0;
            boolean semiInsideBlock = solidStates > 0 && solidStates < totalStates;

            // Обновляем счетчики
            if (semiInsideBlock) {
                this.ticksInBlock++;
            } else {
                this.ticksInBlock = 0;
            }

            // Основная логика прохода через блок
            if (!this.semiPacketSent && semiInsideBlock) {
                int packetsToSend = Math.max(1, Math.min(10, this.semiPackets.get().intValue()));

                double x = mc.player.getX();
                double y = mc.player.getY();
                double z = mc.player.getZ();
                float yaw = mc.player.getYaw();
                float pitch = mc.player.getPitch();
                boolean onGround = mc.player.isOnGround();

                for (int i = 0; i < packetsToSend; ++i) {
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, onGround, false));
                }

                this.semiPacketSent = true;
            } else if (this.semiPacketSent && noSolidInAABB) {
                this.setState(false);
            }

            // Антикик система - работает всегда когда в блоке
            if (semiInsideBlock) {
                this.antiKickTicks++;

                // Каждые 20 тиков отправляем валидный пакет
                if (this.antiKickTicks % 20 == 0 && this.antiKickTimer.hasTimeElapsed(1000L)) {
                    this.sendAntiKickPacket();
                    this.antiKickTimer.reset();
                }

                // Если слишком долго в блоке (100 тиков = 5 секунд), делаем микро-выход каждые 80 тиков
                if (this.ticksInBlock > 100 && this.ticksInBlock % 80 == 0 && this.microExitTimer.hasTimeElapsed(4000L)) {
                    this.performMicroExit();
                    this.microExitTimer.reset();
                }
            }
        }
    }

    private void sendAntiKickPacket() {
        if (mc.player != null && mc.player.networkHandler != null) {
            double x = mc.player.getX();
            double y = mc.player.getY();
            double z = mc.player.getZ();
            float yaw = mc.player.getYaw();
            float pitch = mc.player.getPitch();
            boolean onGround = mc.player.isOnGround();

            // Отправляем легитимный пакет для предотвращения кика
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, onGround, false));
        }
    }

    private void performMicroExit() {
        if (mc.player != null && mc.player.networkHandler != null) {
            double x = mc.player.getX();
            double y = mc.player.getY();
            double z = mc.player.getZ();
            float yaw = mc.player.getYaw();
            float pitch = mc.player.getPitch();

            // Микро-телепорт вверх и обратно для предотвращения долгого нахождения
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x, y + 0.1D, z, yaw, pitch, false, false));
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, false, false));
        }
    }

    @Override
    protected void onDisable() {
        if (mc.player != null && mc.player.networkHandler != null) {
            double x = mc.player.getX();
            double y = mc.player.getY();
            double z = mc.player.getZ();
            float yaw = mc.player.getYaw();
            float pitch = mc.player.getPitch();

            // Улучшенный выход из блока с большим количеством пакетов
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x, y + 0.0625D, z, yaw, pitch, false, false));
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, false, false));
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x, y + 0.03125D, z, yaw, pitch, false, false));
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x, y + 0.015625D, z, yaw, pitch, true, false));
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, mc.player.isOnGround(), false));

            if (mc.player != null && mc.player.networkHandler != null && !this.bufferedPackets.isEmpty()) {
                Iterator<Packet<?>> var7 = this.bufferedPackets.iterator();

                while (var7.hasNext()) {
                    Packet<?> packet = var7.next();
                    NetworkUtils.sendSilentPacket(packet);
                }

                this.bufferedPackets.clear();
            }
        }

        super.onDisable();
    }

    @Override
    protected void onEnable() {
        this.bufferedPackets.clear();
        this.semiPacketSent = false;
        this.ticksInBlock = 0;
        this.antiKickTicks = 0;
        this.antiKickTimer.reset();
        this.microExitTimer.reset();

        super.onEnable();
    }
}

