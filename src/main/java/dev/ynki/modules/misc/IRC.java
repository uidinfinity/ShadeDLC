package dev.ynki.modules.misc;

import dev.ynki.events.Event;
import dev.ynki.manager.Manager;
import dev.ynki.modules.Function;
import dev.ynki.modules.FunctionAnnotation;
import dev.ynki.modules.Type;

@FunctionAnnotation(name = "IRC", desc = "Чат между юзерами других клиентов", type = Type.Misc)
public class IRC extends Function {

    @Override
    public void onEvent(Event event) {
    }


    @Override
    protected void onDisable() {
        Manager.IRC_MANAGER.shutdown();
        super.onDisable();
    }
}