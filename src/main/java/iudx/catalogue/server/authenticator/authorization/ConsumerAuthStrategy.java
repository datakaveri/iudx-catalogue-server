package iudx.catalogue.server.authenticator.authorization;


import iudx.catalogue.server.authenticator.model.JwtData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static iudx.catalogue.server.authenticator.authorization.Api.RATING;
import static iudx.catalogue.server.authenticator.authorization.Method.DELETE;
import static iudx.catalogue.server.authenticator.authorization.Method.POST;
import static iudx.catalogue.server.authenticator.authorization.Method.PUT;

public class ConsumerAuthStrategy implements AuthorizationStratergy{
  private static final Logger LOGGER = LogManager.getLogger(ProviderAuthStrategy.class);

  static List<AuthorizationRequest> accessList = new ArrayList<>();
  static {
    accessList.add(new AuthorizationRequest(POST, RATING));
    accessList.add(new AuthorizationRequest(PUT, RATING));
    accessList.add(new AuthorizationRequest(DELETE, RATING));
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authorizationRequest, JwtData jwtData) {
    return accessList.contains(authorizationRequest);
  }
}
