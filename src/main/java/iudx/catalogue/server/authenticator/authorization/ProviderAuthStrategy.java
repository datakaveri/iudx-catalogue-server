package iudx.catalogue.server.authenticator.authorization;

import static iudx.catalogue.server.authenticator.authorization.Method.DELETE;
import static iudx.catalogue.server.authenticator.authorization.Method.POST;
import static iudx.catalogue.server.authenticator.authorization.Method.PUT;

import java.util.ArrayList;
import java.util.List;

import iudx.catalogue.server.util.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import iudx.catalogue.server.authenticator.model.JwtData;

public class ProviderAuthStrategy implements AuthorizationStratergy{
  private static final Logger LOGGER = LogManager.getLogger(ProviderAuthStrategy.class);
  static List<AuthorizationRequest> accessList = new ArrayList<>();
  private Api api;
  private static volatile ProviderAuthStrategy instance;
  private ProviderAuthStrategy(Api api)
  {
    this.api = api;
    buildPermissions(api);
  }

  public static ProviderAuthStrategy getInstance(Api api)
  {
    if (instance == null)
    {
      synchronized (ProviderAuthStrategy.class)
      {
        if (instance == null)
        {
          instance = new ProviderAuthStrategy(api);
        }
      }
    }
    return instance;
  }

  private void buildPermissions(Api api) {
    // /item access list
    accessList.add(new AuthorizationRequest(POST, api.getRouteItems()));
    accessList.add(new AuthorizationRequest(PUT, api.getRouteItems()));
    accessList.add(new AuthorizationRequest(DELETE, api.getRouteItems()));
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    return accessList.contains(authRequest);
  }
}
