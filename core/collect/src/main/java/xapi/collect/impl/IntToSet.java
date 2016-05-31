package xapi.collect.impl;

import xapi.collect.X_Collect;
import xapi.collect.api.CollectionOptions;

import java.util.Comparator;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 5/21/16.
 */
public class IntToSet <E> extends IntToAbstract<E> {

  public <Generic extends E> IntToSet(
      Class<Generic> cls,
      CollectionOptions opts,
      Comparator<E> comparator
  ) {
    super(X_Collect.newProxy(Integer.class, cls,
        CollectionOptions.from(opts).forbidsDuplicate(true).build()
    ), CollectionOptions.from(opts).forbidsDuplicate(true).build(), comparator);
  }
}
