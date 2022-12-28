package iudx.catalogue.server.authenticator.authorization;

import iudx.catalogue.server.util.Api;

public class AuthorizationContextFactory {

  public static AuthorizationStratergy create(String role, Api api) {
    switch (role) {
      case "consumer": {
        return ConsumerAuthStrategy.getInstance(api);
      }
      case "provider": {
        return ProviderAuthStrategy.getInstance(api);
      }
      case "delegate": {
        return DelegateAuthStrategy.getInstance(api);
      }
      case "admin": {
        return AdminAuthStrategy.getInstance(api);
      }
      default:
        throw new IllegalArgumentException(role + "role is not defined in IUDX");
    }
  }
}
