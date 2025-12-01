package dev.ynki.events.impl.input;


import dev.ynki.events.Event;

public class EventKey extends Event {
    public int key;

    public EventKey(int key) {
        this.key = key;
    }
}
