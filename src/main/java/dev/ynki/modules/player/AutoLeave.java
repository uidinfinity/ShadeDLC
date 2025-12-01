package dev.ynki.modules.player;

import net.minecraft.text.Text;
import dev.ynki.events.Event;
import dev.ynki.events.impl.EventUpdate;
import dev.ynki.manager.ClientManager;
import dev.ynki.manager.Manager;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;
import dev.ynki.modules.setting.BooleanSetting;
import dev.ynki.modules.setting.ModeSetting;
import dev.ynki.modules.setting.SliderSetting;
import dev.ynki.modules.setting.TextSetting;

@SuppressWarnings("All")
@FunctionAnnotation(name = "AutoLeave", desc = "Автоматически ливает с сервера", type = Type.Player)
public class AutoLeave extends Function {

    private final ModeSetting mode = new ModeSetting("Режим",
            "Рядом игрок", "Рядом игрок", "Мало Хп");

    private final SliderSetting heal = new SliderSetting("Здоровье",
            3, 1, 20, 1, () -> mode.is("Мало Хп"));

    private final SliderSetting radius = new SliderSetting("Радиус до игрока",
            60, 20, 150, 1, () -> mode.is("Рядом игрок"));

    private final ModeSetting run = new ModeSetting("Что делать",
            "Выходить с сервера", "Выходить с сервера", "/hub", "телепортация домой");

    private final TextSetting homeName = new TextSetting("Название точки дома",
            "home", () -> run.is("телепортация домой"));

    private final BooleanSetting pvpNoLeave =
            new BooleanSetting("Не выходить если режим PVP", true,
                    () -> run.is("Выходить с сервера") || run.is("/hub"));

    private boolean triggered = false;

    public AutoLeave() {
        addSettings(mode, heal, radius, run, homeName, pvpNoLeave);
    }

    @Override
    public void onEvent(Event event) {
        if (!(event instanceof EventUpdate)) return;
        if (mc.player == null || mc.world == null) return;

        if (pvpNoLeave.get() && ClientManager.playerIsPVP()) return;

        boolean shouldTrigger = false;

        if (mode.is("Мало Хп") && mc.player.getHealth() <= heal.get().floatValue()) {
            shouldTrigger = true;
        } else if (mode.is("Рядом игрок")) {
            shouldTrigger = Manager.SYNC_MANAGER.getPlayers().stream().anyMatch(other -> other != mc.player && mc.player.distanceTo(other) <= radius.get().floatValue());
        }

        if (shouldTrigger && !triggered) {
            executeAction();
            triggered = true;
        } else if (!shouldTrigger) {
            triggered = false;
        }
    }

    private void executeAction() {
        if (mc.player == null) return;

        if (run.is("Выходить с сервера")) {
            mc.player.networkHandler.getConnection().disconnect(Text.literal("AutoLeave"));
        } else if (run.is("/hub")) {
            mc.player.networkHandler.sendChatCommand("hub");
        } else if (run.is("телепортация домой")) {
            mc.player.networkHandler.sendChatCommand("home " + homeName.getValue());
        }
        toggle();
    }
}
