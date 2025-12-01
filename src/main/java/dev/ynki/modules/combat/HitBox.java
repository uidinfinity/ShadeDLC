package dev.ynki.modules.combat;

import dev.ynki.modules.setting.SliderSetting;
import dev.ynki.events.Event;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;

@FunctionAnnotation(name = "HitBox", type = Type.Combat, desc = "Позволяет увеличивать хит-бокс игроков")
public class HitBox extends Function {

    public SliderSetting size = new SliderSetting("Размер", 0.4f, 0.1f, 5.5f, 0.1f);

    public HitBox() {
        addSettings(size);
    }
    @Override
    public void onEvent(Event event) {
    }

}