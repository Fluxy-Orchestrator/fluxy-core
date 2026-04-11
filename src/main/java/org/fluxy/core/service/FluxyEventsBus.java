package org.fluxy.core.service;

import org.fluxy.core.model.FluxyEvent;

public interface FluxyEventsBus {

    void publish(FluxyEvent<?,?> event);

    void listen(FluxyEvent<?,?> event);
}
