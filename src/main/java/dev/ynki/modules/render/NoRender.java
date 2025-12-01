package dev.ynki.modules.render;

import dev.ynki.modules.setting.MultiSetting;
import dev.ynki.events.Event;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;

import java.util.Arrays;

@FunctionAnnotation(name = "NoRender", type = Type.Render, desc = "Убирает разные типы на экране")
public class NoRender extends Function {
    public MultiSetting mods = new MultiSetting(
            "Убрать",
            Arrays.asList("Тряска камеры", "Огонь на экране", "Вода на экране","Удушье","Плохие эффекты"),
            new String[]{"Тряска камеры", "Огонь на экране", "Вода на экране", "Удушье", "Скорборд","Плохие эффекты"}
    );

    public NoRender() {
        addSettings(mods);
    }
    @Override
    public void onEvent(Event event) {

    }
}