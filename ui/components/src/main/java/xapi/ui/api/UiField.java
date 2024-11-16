package xapi.ui.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Any method, field or parameter can be marked with @UiField to tell the
 * generator that we want to fill it in with a dynamically assembled UI element.
 *
 * Given the vast possible scope which can be applied, not all use cases will be
 * implemented in the initial version.
 *
 * When a method is annotated with @UiField, it is expected that the method in
 * question will return a Ui element / controller via generated code;
 * thus, only abstract methods in types eligible to be generated can support method annotations.
 *
 * A field annotated with @UiField will be filled in when the class in question is bound
 * to a Ui.  In the initial implementation, this binding will be limited to generated
 * controller / UiComponent classes; however, in the future, a dynamic binding mechanism
 * will be supplied to allow filling in these fields via imperative code.
 *
 * A parameter annotated with @UiField will indicate that the method in question should
 * be called with the correct UiElements after binding.  As such, these methods can only
 * take @UiField parameters.  In the future, we could also support any @Inject-able type,
 * once the annotated injection subsystem is complete.
 *
 * Long story short: There are big plans for how to support @UiField,
 * but in the short term, it will only be useful in a small but growing subset of use cases.
 *
 * Created by james on 6/6/16.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
public @interface UiField {
    String ref() default "";
    String selector() default "";
    int priority() default 0;
}
