package iudx.catalogue.server.authenticator.authorization;

import static iudx.catalogue.server.authenticator.authorization.Method.*;

import iudx.catalogue.server.authenticator.model.JwtData;
import iudx.catalogue.server.util.Api;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class DelegateAuthStrategy implements AuthorizationStratergy {
  private static final Logger LOGGER = LogManager.getLogger(DelegateAuthStrategy.class);
  private Api api;
  private static volatile DelegateAuthStrategy instance;
  static List<AuthorizationRequest> accessList = new ArrayList<>();

  private DelegateAuthStrategy(Api api) {
    this.api = api;
    buildPermissions(api);
  }

  public static DelegateAuthStrategy getInstance(Api api) {
    if (instance == null) {
      synchronized (DelegateAuthStrategy.class) {
        if (instance == null) {
          instance = new DelegateAuthStrategy(api);
        }
      }
    }
    return instance;
  }

  private void buildPermissions(Api api) {
    // /item access rules
    accessList.add(new AuthorizationRequest(POST, api.getRouteItems()));
    accessList.add(new AuthorizationRequest(PUT, api.getRouteItems()));
    accessList.add(new AuthorizationRequest(DELETE, api.getRouteItems()));
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    return accessList.contains(authRequest);
  }
}
