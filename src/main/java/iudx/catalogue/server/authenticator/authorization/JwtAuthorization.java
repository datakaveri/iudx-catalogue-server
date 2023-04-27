package iudx.catalogue.server.authenticator.authorization;

import iudx.catalogue.server.authenticator.model.JwtData;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class JwtAuthorization {

  private final AuthorizationStratergy authStrategy;

  public JwtAuthorization(final AuthorizationStratergy authStrategy) {
    this.authStrategy = authStrategy;
  }

  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    return authStrategy.isAuthorized(authRequest, jwtData);
  }
}
