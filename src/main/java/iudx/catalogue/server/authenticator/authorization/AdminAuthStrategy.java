package iudx.catalogue.server.authenticator.authorization;

import static iudx.catalogue.server.authenticator.authorization.Method.*;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_PROVIDER;

import iudx.catalogue.server.util.Api;
import java.util.ArrayList;
import java.util.List;

public class AdminAuthStrategy implements AuthorizationStratergy {
  static List<AuthorizationRequest> accessList = new ArrayList<>();
  private static volatile AdminAuthStrategy instance;
  private Api api;

  private AdminAuthStrategy(Api api) {
    this.api = api;
    buildPermissions(api);
  }

  /**
   * Returns a singleton instance of the AdminAuthStrategy class for the specified API.
   *
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
    accessList.add(new AuthorizationRequest(POST, api.getRouteItems(), ITEM_TYPE_PROVIDER));
    accessList.add(new AuthorizationRequest(DELETE, api.getRouteItems(), ITEM_TYPE_PROVIDER));
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest) {
    return accessList.contains(authRequest);
  }
}
