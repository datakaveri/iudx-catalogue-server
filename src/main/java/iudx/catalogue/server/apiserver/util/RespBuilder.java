package iudx.catalogue.server.apiserver.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.util.Constants.*;

public class RespBuilder {
  private JsonObject response = new JsonObject();

  public RespBuilder withType(String type) {
    response.put(TYPE, type);
    return this;
  }

  public RespBuilder withTitle(String title) {
    response.put(TITLE, title);
    return this;
  }

  public RespBuilder withDetail(String detail) {
    response.put(DETAIL, detail);
    return this;
  }

  public RespBuilder withResult(String id, String method, String status) {
    JsonObject resultAttrs = new JsonObject().put(ID, id).put(METHOD, method).put(STATUS, status);
    response.put(RESULTS, new JsonArray().add(resultAttrs));
    return this;
  }

  public RespBuilder withResult(String id, String method, String status, String detail) {
    JsonObject resultAttrs =
        new JsonObject().put(ID, id).put(METHOD, method).put(STATUS, status).put(DETAIL, detail);
    response.put(RESULTS, new JsonArray().add(resultAttrs));
    return this;
  }

  public RespBuilder withResult() {
    response.put(RESULTS, new JsonArray());
    return this;
  }

  public RespBuilder withResult(String resultJsonArray) {
    response.put(RESULTS, new JsonArray(resultJsonArray));
    return this;
  }

  public RespBuilder totalHits(String resultJsonArray) {
    JsonArray jsonArray = new JsonArray(resultJsonArray);
    int size = jsonArray.size();
    response.put(TOTAL_HITS, size);
    return this;
  }

  public JsonObject getJsonResponse() {
    return response;
  }

  public String getResponse() {
    return response.toString();
  }
}
