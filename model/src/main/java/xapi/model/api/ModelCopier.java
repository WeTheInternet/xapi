package xapi.model.api;

import xapi.collect.proxy.api.CollectionProxy;
import xapi.fu.has.HasLock;

/**
 * ModelCopier:
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 06/12/2023 @ 1:24 a.m.
 */
public class ModelCopier {
    public static void copy(final Model model, final Model me, final boolean append) {
        HasLock.alwaysLock(me, ()->{
            model.getProperties().forAll(e -> {
                final Object yourVal = e.getValue();
                if (yourVal == null) {
                    if (!append) {
                        me.removeProperty(e.getKey());
                    }
                    return;
                }
                final Object myVal = me.getProperty(e.getKey());
                final Class<?> propType = me.getPropertyType(e.getKey());
                if (myVal == null) {
                    me.setProperty(e.getKey(), yourVal);
                    return;
                }
                if (Model.class.isAssignableFrom(propType)) {
                    Model myModel = (Model) myVal;
                    assert propType == model.getPropertyType(e.getKey());
                    // perform deeper absorb, to avoid clearing references...
                    myModel.absorb((Model) yourVal, append);
                    return;
                }
                if (CollectionProxy.class.isAssignableFrom(propType)) {
                    CollectionProxy myList = (CollectionProxy) myVal;
                    CollectionProxy yourList = (CollectionProxy) yourVal;
                    if (!append) {
                        myList.clear();
                    }
                    myList.copyFrom(yourList);
                    return;
                }
                // TODO handle more collection types...
                me.setProperty(e.getKey(), e.getValue());
            });
            return null;
        });
    }
}
