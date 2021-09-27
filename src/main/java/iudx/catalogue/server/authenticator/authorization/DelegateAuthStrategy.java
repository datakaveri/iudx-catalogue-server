package iudx.catalogue.server.authenticator.authorization;

import static iudx.catalogue.server.authenticator.authorization.Method.DELETE;
import static iudx.catalogue.server.authenticator.authorization.Method.POST;
import static iudx.catalogue.server.authenticator.authorization.Method.PUT;
import static iudx.catalogue.server.authenticator.authorization.Api.ITEM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import iudx.catalogue.server.authenticator.model.JwtData;

public class DelegateAuthStrategy implements AuthorizationStratergy{
  private static final Logger LOGGER = LogManager.getLogger(DelegateAuthStrategy.class);

  static Map<String, List<AuthorizationRequest>> delegateAuthorizationRules = new HashMap<>();
  static {
    // /item access rules
    List<AuthorizationRequest> accessList = new ArrayList<>();
    accessList.add(new AuthorizationRequest(POST, ITEM));
    accessList.add(new AuthorizationRequest(PUT, ITEM));
    accessList.add(new AuthorizationRequest(DELETE, ITEM));
    delegateAuthorizationRules.put(IudxAccess.API.getAccess(), accessList);
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    return true;
  }
}
