package iudx.catalogue.server.auditing.util;

import static iudx.catalogue.server.auditing.util.Constants.*;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ResponseBuilder {
  private String status;
  private JsonObject response;

  /** Initialise the object with Success or Failure. */
  public ResponseBuilder(String status) {
    this.status = status;
    response = new JsonObject();
  }

  /**
   * Sets the error type and title of the response based on the given status code.
   * @param statusCode The status code to set the error type and title.
   * @return The response builder instance with the error type and title set.
   */
  public ResponseBuilder setTypeAndTitle(int statusCode) {
    response.put(ERROR_TYPE, statusCode);
    if (SUCCESS.equalsIgnoreCase(status)) {
      response.put(TITLE, SUCCESS);
    } else if (FAILED.equalsIgnoreCase(status)) {
      response.put(TITLE, FAILED);
    }
    return this;
  }

  public ResponseBuilder setJsonArray(JsonArray jsonArray) {
    response.put(RESULTS, jsonArray);
    return this;
  }
}
