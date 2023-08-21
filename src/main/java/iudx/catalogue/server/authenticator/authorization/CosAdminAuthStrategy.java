package iudx.catalogue.server.authenticator.authorization;

import static iudx.catalogue.server.authenticator.authorization.Method.*;
import static iudx.catalogue.server.authenticator.authorization.Method.DELETE;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_COS;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_RESOURCE_SERVER;

import iudx.catalogue.server.util.Api;
import java.util.ArrayList;
import java.util.List;

public class CosAdminAuthStrategy implements AuthorizationStratergy {

  static List<AuthorizationRequest> accessList = new ArrayList<>();
  private static volatile CosAdminAuthStrategy instance;
  private Api api;

  private CosAdminAuthStrategy(Api api) {
    this.api = api;
    buildPermissions(api);
  }

  /**
   * Returns a singleton instance of the AdminAuthStrategy class for the specified API.
   *
   * @param api the API to create an AdminAuthStrategy instance for
   * @return a singleton instance of the AdminAuthStrategy class
   */
  public static CosAdminAuthStrategy getInstance(Api api) {
    if (instance == null) {
      synchronized (CosAdminAuthStrategy.class) {
        if (instance == null) {
          instance = new CosAdminAuthStrategy(api);
        }
      }
    }
    return instance;
  }

  private void buildPermissions(Api api) {
    // /item access list
    accessList.add(new AuthorizationRequest(POST, api.getRouteItems(), ITEM_TYPE_COS));
    accessList.add(new AuthorizationRequest(PUT, api.getRouteItems(), ITEM_TYPE_COS));
    accessList.add(new AuthorizationRequest(DELETE, api.getRouteItems(), ITEM_TYPE_COS));
    accessList.add(new AuthorizationRequest(POST, api.getRouteItems(), ITEM_TYPE_RESOURCE_SERVER));
    accessList.add(new AuthorizationRequest(PUT, api.getRouteItems(), ITEM_TYPE_RESOURCE_SERVER));
    accessList.add(new AuthorizationRequest(DELETE, api.getRouteItems(), ITEM_TYPE_RESOURCE_SERVER));
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest) {
    return accessList.contains(authRequest);
  }
}
