package xapi.elemental.impl;

import xapi.annotation.inject.SingletonOverride;
import xapi.event.api.EventHandler;
import xapi.event.api.EventService;
import xapi.event.api.IsEvent;
import xapi.event.impl.AbstractEventService;
import xapi.platform.GwtDevPlatform;
import xapi.platform.GwtPlatform;

import java.io.IOException;
import java.io.Serializable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/24/16.
 */
@GwtPlatform
@GwtDevPlatform
@SingletonOverride(implFor = EventService.class, priority = 1)
public class EventServiceElemental extends AbstractEventService {

    @Override
    protected <Source, Event extends IsEvent<Source>> Serializable backupExtractIdentifier(EventHandler<Source, Event> handler)
    throws IOException {
        return handler.getClass();
    }
}
