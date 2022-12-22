package iudx.catalogue.server.authenticator.authorization;

import static iudx.catalogue.server.authenticator.authorization.Method.DELETE;
import static iudx.catalogue.server.authenticator.authorization.Method.POST;
import static iudx.catalogue.server.authenticator.authorization.Method.PUT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import iudx.catalogue.server.util.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import iudx.catalogue.server.authenticator.model.JwtData;

public class AdminAuthStrategy implements AuthorizationStratergy{
  private static final Logger LOGGER = LogManager.getLogger(ProviderAuthStrategy.class);
  private Api api;
  static List<AuthorizationRequest> accessList = new ArrayList<>();

  public  AdminAuthStrategy(Api api)
  {
    this.api = api;
    buildPermissions(api);
  }

  private void buildPermissions(Api api) {
    // /item access list
    accessList.add(new AuthorizationRequest(POST, api.getRouteInstance()));
    accessList.add(new AuthorizationRequest(DELETE, api.getRouteInstance()));
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    return accessList.contains(authRequest);
  }
}
