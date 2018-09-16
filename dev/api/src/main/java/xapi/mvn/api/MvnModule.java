package xapi.mvn.api;

import xapi.collect.X_Collect;
import xapi.fu.In1Out1;
import xapi.fu.data.MapLike;
import xapi.fu.X_Fu;

import static xapi.fu.Immutable.immutable1;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/4/16.
 */
public interface MvnModule extends MvnDependency {

    default MvnProps moduleProps() {
        return getProperty("props", ()->{
            MapLike<String, String> map = X_Collect.newStringMap(String.class);
            final MvnProps props = immutable1(map)::out1;
            return props;
        });
    }

    default MvnModule setProps(MvnProps props) {
        setProperty("props", props);
        return this;
    }

     default String getProperty(MvnCache cache, String property) {
         final MvnProps props = moduleProps();
         if (props.hasProperty(property)) {
             return X_Fu.mapIfNotNull(props.getProperty(property), In1Out1.of(String::valueOf));
         }
         final MvnModule parent = getParent(cache);
         if (parent == null) {
             return null;
         }
         return parent.getProperty(cache, property);
     }


}
