package com.google.gwt.dev.codeserver;

import xapi.dev.gwtc.api.IsAppSpace;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

public class SuperDevUtil {

  public static IsAppSpace newAppSpace(String module) {
    AppSpace app;
    try {
      File tmp = File.createTempFile("recompile", "log").getParentFile();
      tmp.deleteOnExit();
      // We've overridden AppSpace so we can use more deterministic names for our compile folders,
      // but if the user does not order the jars correctly, our overridden method will be missing.
      try {
        // So, to be safe, we'll try with reflection first, and, on failure, use the existing method.
        //        System.out.println(SuperDevUtil.class.getClassLoader());
        //        System.out.println(Thread.currentThread().getContextClassLoader());
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Class<?> cls = cl.loadClass(AppSpace.class.getName());
        Method method = cls.getDeclaredMethod("create", File.class, String.class);
        method.setAccessible(true);
        app = (AppSpace) method.invoke(null, tmp , "Gwtc"+module);
      } catch (Exception e) {
        e.printStackTrace();
        app = AppSpace.create(tmp);
      }

    } catch (IOException e1) {
      throw new Error("Unable to initialize gwt recompiler ",e1);
    }
    return app;
  }
}
