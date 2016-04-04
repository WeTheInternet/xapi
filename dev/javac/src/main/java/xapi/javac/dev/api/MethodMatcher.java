package xapi.javac.dev.api;

import xapi.source.read.JavaModel.IsNamedType;

import java.util.Optional;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/2/16.
 */
public interface MethodMatcher<T> {

  Optional<T> matches(IsNamedType element);

}
