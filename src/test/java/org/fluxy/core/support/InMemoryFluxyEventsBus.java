package org.fluxy.core.support;

import org.fluxy.core.model.FluxyEvent;
import org.fluxy.core.service.FluxyEventsBus;

import java.util.ArrayList;
import java.util.List;

public class InMemoryFluxyEventsBus implements FluxyEventsBus {

    private final List<FluxyEvent<?, ?>> publishedEvents = new ArrayList<>();
    private final List<FluxyEvent<?, ?>> listenedEvents = new ArrayList<>();

    @Override
    public void publish(FluxyEvent<?, ?> event) {
        publishedEvents.add(event);
    }

    @Override
    public void listen(FluxyEvent<?, ?> event) {
        listenedEvents.add(event);
    }

    public List<FluxyEvent<?, ?>> getPublishedEvents() {
        return publishedEvents;
    }

    public List<FluxyEvent<?, ?>> getListenedEvents() {
        return listenedEvents;
    }

    public void clear() {
        publishedEvents.clear();
        listenedEvents.clear();
    }
}

