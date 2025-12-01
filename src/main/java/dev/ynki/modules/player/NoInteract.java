package dev.ynki.modules.player;

import dev.ynki.modules.setting.BooleanSetting;
import dev.ynki.events.Event;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;

@FunctionAnnotation(name = "NoInteract", desc = "Не даст вам открыть контейнер по нажатию на ПКМ", type = Type.Player)
public class NoInteract extends Function {
    public final BooleanSetting onlyAura = new BooleanSetting("Только с AttackAura",false);

    public NoInteract() {
        addSettings(onlyAura);
    }
    @Override
    public void onEvent(Event event) {

    }
}