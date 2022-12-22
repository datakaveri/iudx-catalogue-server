package iudx.catalogue.server.authenticator.authorization;

import iudx.catalogue.server.authenticator.model.JwtData;
import iudx.catalogue.server.util.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static iudx.catalogue.server.apiserver.util.Constants.ROUTE_RATING;
import static iudx.catalogue.server.authenticator.authorization.Method.*;

public class ConsumerAuthStrategy implements AuthorizationStratergy {
  private static final Logger LOGGER = LogManager.getLogger(ProviderAuthStrategy.class);

  static List<AuthorizationRequest> accessList = new ArrayList<>();
  private Api api;
  public ConsumerAuthStrategy(Api api) {
    this.api = api;
    buildPermissions(api);
  }
  private void buildPermissions(Api api) {
    accessList.add(new AuthorizationRequest(GET, ROUTE_RATING));
    accessList.add(new AuthorizationRequest(POST, ROUTE_RATING));
    accessList.add(new AuthorizationRequest(PUT, ROUTE_RATING));
    accessList.add(new AuthorizationRequest(DELETE, ROUTE_RATING));
  }



    @Override
  public boolean isAuthorized(AuthorizationRequest authorizationRequest, JwtData jwtData) {
    return accessList.contains(authorizationRequest);
  }
}
