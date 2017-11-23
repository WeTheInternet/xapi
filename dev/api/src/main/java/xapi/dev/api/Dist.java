package xapi.dev.api;

import xapi.annotation.compile.Dependency;
import xapi.platform.JrePlatform;
import xapi.platform.Platform;

import java.lang.annotation.Annotation;

/**
 * Used on a type to signal to the Xapi toolchain that we want to run a "dist" build.
 *
 * That is, we are going to treat the type we annotate as an entry point,
 * visit all of the types it references, processing each one for final overrides
 * (performing source-to-source transpilation on all dependencies),
 * then compiling that result into a final output artifact.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 11/12/17.
 */
public @interface Dist {

    /**
     * Whether or not third party dependencies should be rebased.
     *
     * The definition of "third party" is derived from {@link #allowedPackages()}
     * (the allowed packages are left alone).
     *
     * Default allowed packages are (java|xapi).*
     *
     * @return false to skip rebasing dependencies.
     */
    boolean rebaseThirdParty() default true;

    /**
     * Define what packages are allowed to exist without transformation in final output.
     *
     * @return a regex to decide what package names we should leave alone.
     *
     * All other package names (outside of jre type) should be rebased into final output.
     * You can disable this using {@link #rebaseThirdParty()}
     */
    String allowedPackages() default "(java|xapi).*";

    /**
     * @return Any non-empty string to add a main class to the output manifest.
     */
    String mainClassName() default "";
    /**
     * @return An actual class to add a main class to the output manifest.
     *
     * Default return is Dist.class which means "no main class".
     *
     */
    Class<?> mainClass() default Dist.class;

    /**
     * A list of dependencies you want to add to your build.
     *
     * This should be considered supplementary to the classpath you provide.
     */
    Dependency[] dependencies() default {};

    /**
     * The list of platforms to generate a dist build for.
     *
     * Must be
     *
     * You should likely return one type at most,
     * since the final build for a specific runtime
     * should likely have source that only matters for that runtime,
     * but we do want to support multi-tenant builds,
     * in cases where you may want to specify all jre-like platforms at once.
     *
     * @return the class of an annotation that is, itself, annotated by {@link Platform}.
     *
     * Default value is {@link JrePlatform}.
     */
    Class<? extends Annotation>[] platforms() default { JrePlatform.class };
}
