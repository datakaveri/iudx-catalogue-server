package iudx.catalogue.server.authenticator.authorization;

import java.util.stream.Stream;

public enum IudxRole {

  PROVIDER("provider"),
  DELEGATE("delegate");

  private final String role;

  IudxRole(String role) {
    this.role = role;
  }

  public String getRole() {
    return this.role;
  }

  /**
   * Returns the IudxRole corresponding to the given role string.
   * @param role the role string
   * @return the IudxRole corresponding to the given role string,
   *         or null if no match is found
   */
  public static IudxRole fromRole(final String role) {
    return Stream.of(values())
            .filter(v -> v.role.equalsIgnoreCase(role))
            .findAny()
            .orElse(null);
  }
}
