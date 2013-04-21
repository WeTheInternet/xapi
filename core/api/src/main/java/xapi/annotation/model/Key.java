package xapi.annotation.model;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({})//only allowed inside an @Model tag.
public @interface Key {

  /**
   * The data type of the key, used to instantiate wrapper maps.
   *
   * @return - The type of key to use in any maps; default is String.
   *
   * You are recommended to avoid the use of Long keys;
   * not only are they emulated and thus slow on the client,
   * they also require your server to lock on a key range to provide you keys.
   *
   * Prefer instead to use deterministic keys that you can contruct from runtime data.
   */
  public Class<?> keyType() default String.class;
  
  public String value();

}
