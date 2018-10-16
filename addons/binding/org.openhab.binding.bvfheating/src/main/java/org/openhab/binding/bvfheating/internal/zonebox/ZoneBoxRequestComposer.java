package org.openhab.binding.bvfheating.internal.zonebox;

import java.util.function.BiFunction;

public class ZoneBoxRequestComposer extends AbstractZoneBoxConstants {

    public <R> R switchRoom(int roomNo, BiFunction<String, String, R> paramConsumer) {
        return paramConsumer.apply(ROOM_ELEMENT_NAME, Integer.toString(roomNo));
    }
}
