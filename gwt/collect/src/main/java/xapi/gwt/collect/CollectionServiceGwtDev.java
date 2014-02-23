package xapi.gwt.collect;

import xapi.annotation.inject.SingletonOverride;
import xapi.collect.api.CollectionOptions;
import xapi.collect.api.StringTo;
import xapi.collect.impl.CollectionServiceDefault;
import xapi.collect.service.CollectionService;
import xapi.log.X_Log;
import xapi.platform.GwtDevPlatform;

/**
 * Because gwt prod uses low-level JSO collection types,
 * we must override gwt dev with our default java.util backed
 * default collection service.
 *
 * You may have your own collection library you may with to adapt,
 * and can simply annotate an override with priority > Integer.MIN_VALUE,
 * and add the correct @Gwt___Platform annotations.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 */
@GwtDevPlatform
@SingletonOverride(implFor=CollectionService.class, priority=Integer.MIN_VALUE+1)
public class CollectionServiceGwtDev
extends CollectionServiceDefault
implements CollectionService{

  static {
    Package java = Package.getPackage("java");
    touch(java);
  }

  @Override
  public <V> StringTo<V> newStringMap(Class<? extends V> cls, CollectionOptions opts) {
    return super.newStringMap(cls, opts);
  }

  private static void touch(Package java) {
    X_Log.info(java);
  }

}
