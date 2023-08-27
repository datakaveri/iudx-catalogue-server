package iudx.catalogue.server.authenticator.authorization;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.authenticator.authorization.Method.*;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_RESOURCE_SERVER;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_PROVIDER;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_INSTANCE;

import iudx.catalogue.server.authenticator.model.JwtData;
import iudx.catalogue.server.util.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class AdminAuthStrategy implements AuthorizationStratergy {
  private static Logger LOGGER = LogManager.getLogger(AdminAuthStrategy.class);
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
    accessList.add(new AuthorizationRequest(POST, api.getRouteItems(), ITEM_TYPE_PROVIDER));
    accessList.add(new AuthorizationRequest(DELETE, api.getRouteItems(), ITEM_TYPE_PROVIDER));
    accessList.add(new AuthorizationRequest(POST, api.getRouteInstance(), ITEM_TYPE_INSTANCE));
    accessList.add(new AuthorizationRequest(DELETE, api.getRouteInstance(), ITEM_TYPE_INSTANCE));
    accessList.add(new AuthorizationRequest(POST, api.getRouteMlayerInstance(), ""));
    accessList.add(new AuthorizationRequest(DELETE, api.getRouteMlayerInstance(), ""));
    accessList.add(new AuthorizationRequest(PUT, api.getRouteMlayerInstance(), ""));
    accessList.add(new AuthorizationRequest(POST, api.getRouteMlayerDomains(), ""));
    accessList.add(new AuthorizationRequest(PUT, api.getRouteMlayerDomains(), ""));
    accessList.add(new AuthorizationRequest(DELETE, api.getRouteMlayerDomains(), ""));
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest) {
    return accessList.contains(authRequest);
  }
}
