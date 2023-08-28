package iudx.catalogue.server.authenticator.authorization;

import iudx.catalogue.server.authenticator.model.JwtData;

public interface AuthorizationStratergy {

  boolean isAuthorized(AuthorizationRequest authRequest);
}
