package iudx.catalogue.server.authenticator.authorization;

import static iudx.catalogue.server.authenticator.authorization.Method.*;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_RESOURCE;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_RESOURCE_GROUP;

import iudx.catalogue.server.util.Api;
import java.util.ArrayList;
import java.util.List;

public class DelegateAuthStrategy implements AuthorizationStratergy {
  static List<AuthorizationRequest> accessList = new ArrayList<>();
  private static volatile DelegateAuthStrategy instance;

  private DelegateAuthStrategy(Api api) {
    buildPermissions(api);
  }

  /**
   * Returns the instance of the DelegateAuthStrategy class.
   * If the instance does not exist, creates one using the
   * provided API object and returns it.
   * @param api The API object used to create the instance.
   * @return The instance of DelegateAuthStrategy.
   */
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
    accessList.add(new AuthorizationRequest(POST, api.getRouteItems(), ITEM_TYPE_RESOURCE_GROUP));
    accessList.add(new AuthorizationRequest(POST, api.getRouteItems(), ITEM_TYPE_RESOURCE));
    accessList.add(new AuthorizationRequest(PUT, api.getRouteItems(), ITEM_TYPE_RESOURCE_GROUP));
    accessList.add(new AuthorizationRequest(PUT, api.getRouteItems(), ITEM_TYPE_RESOURCE));
    accessList.add(new AuthorizationRequest(DELETE, api.getRouteItems(), ITEM_TYPE_RESOURCE_GROUP));
    accessList.add(new AuthorizationRequest(DELETE, api.getRouteItems(), ITEM_TYPE_RESOURCE));
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest) {
    return accessList.contains(authRequest);
  }
}
