package xapi.annotation.inject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

  /**
   * Used to link an instance implementation class to an injectable interface.
   *
   * <br/><br/>
   *
   * There can be one and only one @InstanceDefault PER scope class;
   *
   * A ThreadLocal variable is used to affect what types are injected;
   *
   *
   *
   * <br/><br/>
   *
   * Example:
   *
   * <br/>
   *
   * static interface MyService{}
   *
   * <br/>
   *
   * <pre>@SingletonDefault(implFor=MyService.class)</pre>
   * static class MyServiceImpl implements MyService{}
   *
   * <br/><br/>
   *
   * //returns a singleton instance of MyServiceImpl
   * <br/>
   * MyService service = X_Inject.singleton(MyService.class);

   * <br/><br/>
   *
   * @author James X. Nelson (james@wetheinter.net, @james)
   */


@Target(value=ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface InstanceDefault {

  /**
   * @return - The class object for the instance / class the annotated type is implementing.
   * Since your class must implement this interface, it must be on your classpath anyway ;)
   *
   */
  Class<?> implFor();

}
