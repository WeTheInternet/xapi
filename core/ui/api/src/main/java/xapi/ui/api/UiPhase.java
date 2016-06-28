package xapi.ui.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/26/16.
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface UiPhase {

    /**
     * A phase id you expect to be complete before you run.
     * (phases that run before this phase)
     */
    String prerequisite() default "";

    /**
     * A phase you wish to block
     * (a phase that you expect to run after this phase)
     */
    String block() default "";
    String id(); // a key we can use to put these into maps.
    int priority() default 0;

    Class[] CORE_PHASES = new Class[]{
          PhasePreprocess.class,
          PhaseSupertype.class,
          PhaseIntegration.class,
          PhaseImplementation.class,
          PhaseBinding.class
    };

    @UiPhase(
          id = PhasePreprocess.PHASE_PREPROCESS,
          block = PhaseSupertype.PHASE_SUPERTYPE
    )
    @interface PhasePreprocess{
        String PHASE_PREPROCESS = "preprocess";
        int priority() default 0;
    }

    @UiPhase(
          prerequisite = PhasePreprocess.PHASE_PREPROCESS,
          id = PhaseSupertype.PHASE_SUPERTYPE,
          block = PhaseIntegration.PHASE_INTEGRATION
    )
    @interface PhaseSupertype{
        String PHASE_SUPERTYPE = "supertype";
        int priority() default 0;
    }

    @UiPhase(
          prerequisite = PhaseSupertype.PHASE_SUPERTYPE,
          id = PhaseIntegration.PHASE_INTEGRATION,
          block = PhaseImplementation.PHASE_IMPLEMENTATION
    )
    @interface PhaseIntegration{
        String PHASE_INTEGRATION = "integration";

        int priority() default 0;
    }

    @UiPhase(
          prerequisite = PhaseIntegration.PHASE_INTEGRATION,
          id = PhaseImplementation.PHASE_IMPLEMENTATION,
          block = PhaseBinding.PHASE_BINDING
    )
    @interface PhaseImplementation{
        String PHASE_IMPLEMENTATION = "implementation";

        int priority() default 0;
    }

    @UiPhase(
          prerequisite = PhaseImplementation.PHASE_IMPLEMENTATION,
          id = PhaseBinding.PHASE_BINDING
    )
    @interface PhaseBinding{
        String PHASE_BINDING = "binding";

        int priority() default 0;
    }

}
