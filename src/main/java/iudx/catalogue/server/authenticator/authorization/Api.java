package iudx.catalogue.server.authenticator.authorization;

import java.util.stream.Stream;

import static iudx.catalogue.server.authenticator.Constants.INSTANCE_ENDPOINT;
import static iudx.catalogue.server.authenticator.Constants.ITEM_ENDPOINT;

public enum Api {
  ITEM(ITEM_ENDPOINT),
  INSTANCE(INSTANCE_ENDPOINT);

  private final String endpoint;

  Api(String endpoint) {
    this.endpoint = endpoint;
  }

  public String getApiEndpoint() {
    return this.endpoint;
  }

  public static Api fromEndpoint(final String endpoint) {
    return Stream.of(values())
            .filter(v -> v.endpoint.equalsIgnoreCase(endpoint))
            .findAny()
            .orElse(null);
  }
}
