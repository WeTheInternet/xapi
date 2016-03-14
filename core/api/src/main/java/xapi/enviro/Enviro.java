package xapi.enviro;

import xapi.util.api.ProvidesValue;
import xapi.util.service.PropertyService;

import java.lang.annotation.Annotation;

/**
 * An interface to encapsulate a runtime environment.
 *
 * In xapi, this provider is used primarily for selecting
 * runtime environments during the compile phase,
 * however, the environment model is designed to allow
 * encapsulation of a runtime within a runtime,
 * by providing {@link #getOrCreate(Object, ProvidesValue)},
 * which allows a create-once-per-enviro abstraction.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public interface Enviro {

  Annotation[] platform();

  PropertyService properties();

  boolean isDev();

  boolean isProd();

  boolean isTest();

  int maxThreads();

  String version();

}
