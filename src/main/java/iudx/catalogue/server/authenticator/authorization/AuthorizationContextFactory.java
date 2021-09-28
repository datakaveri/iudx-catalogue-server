package iudx.catalogue.server.authenticator.authorization;

public class AuthorizationContextFactory {

  public static AuthorizationStratergy create(String role) {
    switch (role) {
      case "provider": {
        return new ProviderAuthStrategy();
      }
      case "delegate": {
        return new DelegateAuthStrategy();
      }
      default:
        throw new IllegalArgumentException(role + "role is not defined in IUDX");
    }
  }
}
