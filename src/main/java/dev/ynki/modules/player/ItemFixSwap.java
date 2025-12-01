package dev.ynki.modules.player;

import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import dev.ynki.events.Event;
import dev.ynki.events.impl.EventPacket;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;

@FunctionAnnotation(
        name = "ItemSwapFix",
        keywords = {"NoSlotChange", "NoServerDesync", "СлотФиксер"},
        desc = "Убирает переключение слота от античита",
        type = Type.Player
)
public class ItemFixSwap extends Function {
    private int lastKnownSlot = -1;
    
    public ItemFixSwap() {
        addSettings();
    }

    @Override
    public void onEvent(Event event) {
        if (mc.player == null) return;
        
        if (event instanceof EventPacket e) {
            // Отслеживаем отправляемые пакеты для синхронизации
            if (!e.isReceivePacket() && e.getPacket() instanceof UpdateSelectedSlotC2SPacket packet) {
                lastKnownSlot = packet.getSelectedSlot();
            }
            
            // В версии 1.21.4 сервер может пытаться изменить слот через другие механизмы
            // Синхронизируем слот при необходимости
            if (e.isReceivePacket()) {
                int currentSlot = mc.player.getInventory().selectedSlot;
                
                // Если слот изменился не по нашей воле (после получения пакета от сервера)
                // и у нас есть запомненный слот, отправляем правильный слот обратно
                if (lastKnownSlot != -1 && currentSlot != lastKnownSlot) {
                    // Отправляем правильный слот обратно серверу (как в PZIDATIE SWAPI YO)
                    mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(Math.max(lastKnownSlot - 1, 0)));
                    mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(lastKnownSlot));
                }
            }
        }
        
        // Обновляем последний известный слот из инвентаря
        if (mc.player != null) {
            int currentSlot = mc.player.getInventory().selectedSlot;
            if (lastKnownSlot == -1 || currentSlot != lastKnownSlot) {
                lastKnownSlot = currentSlot;
            }
        }
    }
    
    @Override
    public void onDisable() {
        lastKnownSlot = -1;
        super.onDisable();
    }
}