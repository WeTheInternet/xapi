package xapi.event.api;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/16/16.
 */
public interface HasBeforeAfter <T> {

    T getBefore();

    T getAfter();

}
