package iudx.catalogue.server.authenticator.authorization;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.authenticator.authorization.Method.*;

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
  private static volatile AdminAuthStrategy instance;
  static List<AuthorizationRequest> accessList = new ArrayList<>();

  private  AdminAuthStrategy(Api api)
  {
    this.api = api;
    buildPermissions(api);
  }
  public static AdminAuthStrategy getInstance(Api api)
  {
    if (instance == null)
    {
      synchronized (AdminAuthStrategy.class)
      {
        if (instance == null)
        {
          instance = new AdminAuthStrategy(api);
        }
      }
    }
    return instance;
  }


  private void buildPermissions(Api api) {
    // /item access list
    accessList.add(new AuthorizationRequest(POST, api.getRouteInstance()));
    accessList.add(new AuthorizationRequest(DELETE, api.getRouteInstance()));
    accessList.add(new AuthorizationRequest(POST,ROUTE_MLAYER_INSTANCE));
    accessList.add(new AuthorizationRequest(DELETE,ROUTE_MLAYER_INSTANCE));
    accessList.add(new AuthorizationRequest(PUT,ROUTE_MLAYER_INSTANCE));
    accessList.add(new AuthorizationRequest(POST,ROUTE_MLAYER_DOMAIN));
    accessList.add(new AuthorizationRequest(PUT,ROUTE_MLAYER_DOMAIN));
    accessList.add(new AuthorizationRequest(DELETE,ROUTE_MLAYER_DOMAIN));
    accessList.add(new AuthorizationRequest(GET,ROUTE_MLAYER_PROVIDER));



  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    return accessList.contains(authRequest);
  }
}
