package dev.ynki.modules.misc;

import dev.ynki.events.Event;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;

@FunctionAnnotation(name = "NoCommands", desc = "Отключение команд через точку", type = Type.Misc)
public class NoCommands extends Function {
    public NoCommands() {
    }

    @Override
    public void onEvent(Event event) {

    }
}