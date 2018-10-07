package xapi.source.api;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 10/7/18 @ 2:53 AM.
 */
public interface IsInterfaceDeclaration extends IsTypeDeclaration {

    @Override
    default boolean isInterface() {
        return true;
    }
}
