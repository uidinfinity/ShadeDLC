package dev.ynki.modules.player;

import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import dev.ynki.events.Event;
import dev.ynki.events.impl.EventUpdate;
import dev.ynki.manager.Manager;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;
import dev.ynki.modules.setting.ModeSetting;
import dev.ynki.modules.setting.SliderSetting;
import dev.ynki.util.player.TimerUtil;

import java.util.List;

@FunctionAnnotation(name = "ChestStealer", desc = "", type = Type.Player)
public class ChestStealer extends Function {

    private final ModeSetting mode = new ModeSetting("Тип", "Обычный", "Обычный", "Умный");
    private final SliderSetting stealDelay = new SliderSetting("Задержка", 120f, 0f, 1000f, 1f);

    private final TimerUtil timer = new TimerUtil();

    private static final List<String> BLOCKED_TITLES = List.of(
            "Аукцион", "Warp", "Варпы", "Меню", "Выбор набора", "Кейсы", "Магазин"
    );

    public ChestStealer() {
        addSettings(mode, stealDelay);
    }

    @Override
    public void onEvent(Event event) {
        if (!(event instanceof EventUpdate) || !(mc.currentScreen instanceof GenericContainerScreen container)) return;

        String title = container.getTitle().getString().toLowerCase();
        for (String blocked : BLOCKED_TITLES) {
            if (title.contains(blocked.toLowerCase())) return;
        }

        var handler = container.getScreenHandler();
        int chestSize = handler.getRows() * 9;
        boolean instant = stealDelay.get().floatValue() == 0;

        for (int i = 0; i < chestSize; i++) {
            var stack = handler.getSlot(i).getStack();
            if (stack.isEmpty() || stack.getItem() == Items.AIR) continue;

            if (mode.is("Умный") && !Manager.CHESTSTEALER_MANAGER.isAllowed(stack.getItem())) continue;

            if (instant || timer.hasTimeElapsed(stealDelay.get().longValue())) {
                click(handler.syncId, i);
                if (!instant) timer.reset();
                if (!instant) break;
            }
        }
    }

    private void click(int id, int slot) {
        mc.interactionManager.clickSlot(id, slot, 0, SlotActionType.QUICK_MOVE, mc.player);
    }
}
