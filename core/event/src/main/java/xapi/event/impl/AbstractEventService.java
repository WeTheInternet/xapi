package xapi.event.impl;

import xapi.annotation.inject.SingletonDefault;
import xapi.event.api.EventHandler;
import xapi.event.api.EventHandlerWithIdentity;
import xapi.event.api.EventService;
import xapi.event.api.IsEvent;
import xapi.error.NotImplemented;
import xapi.fu.X_Fu;
import xapi.log.X_Log;
import xapi.debug.X_Debug;
import xapi.util.X_Util;

import java.io.IOException;
import java.io.Serializable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/16/16.
 */
@SingletonDefault(implFor = EventService.class)
public class AbstractEventService implements EventService {

    private boolean warn = true;

    @Override
    public <Source, Event extends IsEvent<Source>> EventHandler<Source, Event> normalizeHandler(EventHandler<Source, Event> handler) {
        try {
            // in case our handler is a lambda, we want to create an equals-safe implementation...
            if (isLambda(handler)) {
                Serializable lambdaBytes = extractIdentifier(handler);
                return new EventHandlerWithIdentity<>(lambdaBytes, handler);
            }
        } catch (Exception e) {
            warnOnce(handler);
            if (shouldDebug()) {
                X_Debug.maybeRethrow(e);
            } else {
                X_Util.maybeRethrow(e);
            }
        }
        // no luck... just return the handler unmodified
        return handler;
    }

    private <Source, Event extends IsEvent<Source>> void warnOnce(EventHandler<Source, Event> handler) {
        if (warn) {
            warn = false;
            X_Log.warn(getClass(), "Unable to serialize lambda-typed event handler", handler,
                ".\nMost likely this lambda is attached to a non-serializable instance, or closes over non-serializable " +
                    "values.\nAs such, identity-semantics cannot be ensured, and you will be unable to remove this handler " +
                    "by calling EventManager.removeHandler\n(instead, you MUST retain the RemovalHandler returned by " +
                    "EventManager.addHandler, or else your application may leak memory).\nAlternatively, use an EventHandler " +
                    "which provides equality semantics (EventHandlerWithIdentity).");
        }
    }

    protected boolean shouldDebug() {
        return false;
    }

    protected <Source, Event extends IsEvent<Source>> Serializable extractIdentifier(EventHandler<Source, Event> handler)
    throws IOException {
        String name = X_Fu.getLambdaMethodName(handler);
        if (name != null) {
            return name;
        }
        return backupExtractIdentifier(handler);
    }

    protected  <Source, Event extends IsEvent<Source>> Serializable backupExtractIdentifier(EventHandler<Source, Event> handler)
    throws IOException {
        throw new NotImplemented(getClass() + " must implement backupExtractIdentifier; could not get id for "+ handler);
    }

    public boolean isLambda(EventHandler<?, ?> handler) {
        return X_Fu.isLambda(handler);
    }
}
