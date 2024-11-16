package xapi.server.auth;

import xapi.model.user.ModelUser;

public interface AuthService <AuthSource> {

  final String NOT_LOGGED_IN = "anonymous";

  String getUuid(AuthSource request);

  ModelUser getLoggedInUser();

}
