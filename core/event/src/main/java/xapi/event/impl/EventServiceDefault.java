package xapi.event.impl;

import xapi.annotation.inject.SingletonDefault;
import xapi.event.api.EventHandler;
import xapi.event.api.EventService;
import xapi.event.api.IsEvent;
import xapi.platform.AndroidPlatform;
import xapi.platform.JrePlatform;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/16/16.
 */
@JrePlatform
@AndroidPlatform
@SingletonDefault(implFor = EventService.class)
public class EventServiceDefault extends AbstractEventService {

    @Override
    protected <Source, Event extends IsEvent<Source>> Serializable backupExtractIdentifier(EventHandler<Source, Event> handler)
    throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(handler);
        }
        return bytes.toByteArray();
    }
}
