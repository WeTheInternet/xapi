package xapi.annotation.model;

import java.lang.annotation.Repeatable;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 10/20/18 @ 5:46 AM.
 */

@Repeatable(ComponentTypes.class)
public @interface ComponentType {
    int index() default 0;
    String value();
}
