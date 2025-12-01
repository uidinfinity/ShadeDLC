package dev.ynki.modules.player;

import dev.ynki.modules.setting.SliderSetting;
import dev.ynki.events.Event;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;

@FunctionAnnotation(name = "ItemScroll", desc = "Быстрое перемещение", type = Type.Player)
public class ItemScroller extends Function {
    public SliderSetting scroll = new SliderSetting("Задержка", 100f, 1f, 100f,1f);

    public ItemScroller() {
        addSettings(scroll);
    }

    @Override
    public void onEvent(Event event) {

    }
}