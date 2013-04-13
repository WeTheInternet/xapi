package xapi.process.api;

import xapi.annotation.model.ClientToServer;
import xapi.annotation.model.ServerToClient;
import xapi.annotation.process.Blocking;
import xapi.annotation.process.RunParallel;

public @interface ProcessSettings {

  Blocking isBlocking() default @Blocking;
  RunParallel isParallel() default @RunParallel;
  ClientToServer clientSerializer() default @ClientToServer;
  ServerToClient serverSerializer() default @ServerToClient;

}
