package iudx.catalogue.server.authenticator.authorization;

import iudx.catalogue.server.util.Api;

public class AuthorizationContextFactory {

  public static AuthorizationStratergy create(String role, Api api) {
    switch (role) {
      case "consumer": {
        return new ConsumerAuthStrategy(api);
      }
      case "provider": {
        return new ProviderAuthStrategy(api);
      }
      case "delegate": {
        return new DelegateAuthStrategy(api);
      }
      case "admin": {
        return new AdminAuthStrategy(api);
      }
      default:
        throw new IllegalArgumentException(role + "role is not defined in IUDX");
    }
  }
}
