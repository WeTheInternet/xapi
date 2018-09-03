package xapi.fu.io;

import xapi.fu.In1Out1;
import xapi.fu.api.Generate;

/**
 * This interface is used to house a collection of static methods,
 * as well as @Xapi("") annotations that feed the code generator
 * which creates the myriad of In#Out# classes for us to use.
 *
 * When adding new generated types,
 * it can be useful to create a new static native method with annotations,
 * then once the type you need is generated, remove the native keyword
 * and implement whatever you needed to generate.
 *
 * The bottom portion of this class is maintained by a generator,
 * from the annotated members of the human-maintained source code.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 10/15/17.
 */
@Generate({
    "<generate-interface",
        "var = {",
        "  max: 5",
        "}",
    "/generate-interface>",
})
public interface InOut {

    /**
     * This method exists solely to give you a handy place to declare a lambda.
     */
    @Generate({
        "<for range=[2, $max]",
             "as=n",
             "var={",
             "typeParams = $range(2, $n, ",
                 "t->T$t",
             "}",
             "value=",
             "static <$typeParams> In$nOut1<$typeParams, Boolean> filter(",
                    "In$Out1<$typeParams, Boolean> source) {",
                    "return source;",
            "}",
        "/for>"
    })
    static <T> In1Out1<T, Boolean> filter(In1Out1<T, Boolean> source) {
        // This method is just here to give you a handy place to create lambdas.
        return source;
    }


}
