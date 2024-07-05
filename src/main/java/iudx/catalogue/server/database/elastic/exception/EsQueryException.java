package iudx.catalogue.server.database.elastic.exception;


import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.elastic.ResponseUrn;

public class EsQueryException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final ResponseUrn urn;
  private final String message;

  public EsQueryException(final String message) {
    this(ResponseUrn.BAD_REQUEST_URN, message);
  }

  public EsQueryException(final ResponseUrn urn, final String message) {
    super(message);
    this.urn = urn;
    this.message = message;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.put(TYPE, urn.getUrn());
    json.put(TITLE, urn.getMessage());
    json.put(DETAIL, message);
    return json;
  }
}
