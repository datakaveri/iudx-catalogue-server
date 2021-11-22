package iudx.catalogue.server.authenticator.authorization;

import static iudx.catalogue.server.authenticator.authorization.Api.ITEM;
import static iudx.catalogue.server.authenticator.authorization.Method.DELETE;
import static iudx.catalogue.server.authenticator.authorization.Method.POST;
import static iudx.catalogue.server.authenticator.authorization.Method.PUT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import iudx.catalogue.server.authenticator.model.JwtData;

public class AdminAuthStrategy implements AuthorizationStratergy{
  private static final Logger LOGGER = LogManager.getLogger(ProviderAuthStrategy.class);

  static List<AuthorizationRequest> accessList = new ArrayList<>();
  static {
    // /item access list
    accessList.add(new AuthorizationRequest(POST, Api.INSTANCE));
    accessList.add(new AuthorizationRequest(DELETE, Api.INSTANCE));
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    return accessList.contains(authRequest);
  }
}
