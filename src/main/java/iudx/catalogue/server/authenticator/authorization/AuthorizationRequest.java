package iudx.catalogue.server.authenticator.authorization;

import java.util.Objects;

public class AuthorizationRequest {

  private final Method method;
  private final String api;
  private final String itemType;

  public AuthorizationRequest(final Method method, final String api, String itemType) {
    this.method = method;
    this.api = api;
    this.itemType = itemType;
  }

  @Override
  public String toString() {
    return "AuthorizationRequest{"
            + "method=" + method
            + ", api='" + api + '\''
            + ", itemType='" + itemType + '\''
            + '}';
  }

  public Method getMethod() {
    return method;
  }

  public String getApi() {
    return api;
  }

  public String getItemType() {
    return itemType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AuthorizationRequest that = (AuthorizationRequest) o;
    return getMethod() == that.getMethod()
        && getApi().equals(that.getApi())
        && getItemType().equals(that.getItemType());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getMethod(), getApi(), getItemType());
  }
}
