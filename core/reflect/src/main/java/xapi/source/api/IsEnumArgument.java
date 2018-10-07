package xapi.source.api;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 10/7/18 @ 2:55 AM.
 */
public interface IsEnumArgument extends IsTypeArgument {

    @Override
    IsType getType();
    // TODO: fix api to do something like this:
//    IsEnumDeclaration getType();
    IsEnumDeclaration getEnumType();
}
