package xapi.dev;

import xapi.dev.api.Dist;
import xapi.dev.impl.DevApp;

/**
 * This class exists to give us somewhere in the source tree to reference
 * into the generated code that we want to keep.
 *
 * It's current purpose is, primarily, to give maven-shade-plugin
 * a ball of source that references all the things we want to keep,
 * and a location that generators can freely edit.
 *
 * Do not add to this class, but feel free to reference it if you depend on the uber jar.
 * Everything sourced from this class will have been resolved when Xapi was built,
 * and will not pay attention to any overrides on your classpath.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 11/12/17.
 */
@Dist(
    mainClass = DevApp.class,
    rebaseThirdParty = false // too hard for right now :-)
)
class X_ {

}
