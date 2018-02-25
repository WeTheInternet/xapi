package xapi.ui.api;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 2/24/18.
 */
@FunctionalInterface
public interface CreatesChildren <Source, Child> {

    Child createChild(Source source);

}
