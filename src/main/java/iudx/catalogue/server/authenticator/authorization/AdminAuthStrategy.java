package iudx.catalogue.server.authenticator.authorization;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.authenticator.authorization.Method.*;

import iudx.catalogue.server.authenticator.model.JwtData;
import iudx.catalogue.server.util.Api;
import java.util.ArrayList;
import java.util.List;

public class AdminAuthStrategy implements AuthorizationStratergy {
  static List<AuthorizationRequest> accessList = new ArrayList<>();
  private static volatile AdminAuthStrategy instance;
  private Api api;

  private  AdminAuthStrategy(Api api) {
    this.api = api;
    buildPermissions(api);
  }

  /**
   * Returns a singleton instance of the AdminAuthStrategy class for the specified API.
   * @param api the API to create an AdminAuthStrategy instance for
   * @return a singleton instance of the AdminAuthStrategy class
   */
  public static AdminAuthStrategy getInstance(Api api) {
    if (instance == null) {
      synchronized (AdminAuthStrategy.class) {
        if (instance == null) {
          instance = new AdminAuthStrategy(api);
        }
      }
    }
    return instance;
  }


  private void buildPermissions(Api api) {
    // /item access list
    accessList.add(new AuthorizationRequest(POST, api.getRouteItems())); 
    accessList.add(new AuthorizationRequest(DELETE, api.getRouteItems()));
    accessList.add(new AuthorizationRequest(POST, api.getRouteInstance()));
    accessList.add(new AuthorizationRequest(DELETE, api.getRouteInstance()));
    accessList.add(new AuthorizationRequest(POST, api.getRouteMlayerInstance()));
    accessList.add(new AuthorizationRequest(DELETE, api.getRouteMlayerInstance()));
    accessList.add(new AuthorizationRequest(PUT, api.getRouteMlayerInstance()));
    accessList.add(new AuthorizationRequest(POST, ROUTE_MLAYER_DOMAIN));
    accessList.add(new AuthorizationRequest(PUT, ROUTE_MLAYER_DOMAIN));
    accessList.add(new AuthorizationRequest(DELETE, ROUTE_MLAYER_DOMAIN));
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    return accessList.contains(authRequest);
  }
}
