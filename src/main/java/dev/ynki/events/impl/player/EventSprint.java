package dev.ynki.events.impl.player;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import dev.ynki.events.Event;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class EventSprint extends Event {
    private boolean sprinting;
}