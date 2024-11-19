package xapi.dev.ui.html;

import com.google.gwt.core.ext.typeinfo.JClassType;

public interface CreatesContextObject <Ctx> {

  Ctx newContext(JClassType winner, String pkgName, String name);

}
