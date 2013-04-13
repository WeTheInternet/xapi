package xapi.jre.collect;

import xapi.annotation.inject.SingletonDefault;
import xapi.collect.impl.CollectionServiceDefault;
import xapi.collect.service.CollectionService;
import xapi.platform.GwtDevPlatform;
import xapi.platform.JrePlatform;

@JrePlatform
@GwtDevPlatform
@SingletonDefault(implFor=CollectionService.class)
public class CollectionServiceJre extends CollectionServiceDefault{

}
