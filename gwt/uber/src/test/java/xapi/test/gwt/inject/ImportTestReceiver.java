package xapi.test.gwt.inject;

import xapi.fu.In1;
import xapi.test.gwt.inject.SplitPointTest.ImportTestInterface;

public interface ImportTestReceiver extends In1<ImportTestInterface> {

    @Override
    default void in(ImportTestInterface in) {
    }
}
