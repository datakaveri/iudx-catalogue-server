package iudx.catalogue.server.database;

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
    JsonObject resultAttrs = new JsonObject().put(ID, id)
      .put(METHOD, method)
      .put(STATUS, status);
    response.put(RESULTS, new JsonArray().add(resultAttrs));
    return this;
  }

  public RespBuilder withResult(String id, String method, String status, String detail) {
    JsonObject resultAttrs = new JsonObject().put(ID, id)
      .put(METHOD, method)
      .put(STATUS, status)
      .put(DETAIL, detail);
    response.put(RESULTS, new JsonArray().add(resultAttrs));
    return this;
  }
  public RespBuilder withResult(String id, String detail) {
    JsonObject resultAttrs = new JsonObject().put(ID,id)
            .put(DETAIL, detail);
    response.put(RESULTS,new JsonArray().add(resultAttrs));
    return this;
  }

  public RespBuilder withResult(String id) {
    JsonObject resultAttrs = new JsonObject().put(ID, id);
    response.put(RESULTS, new JsonArray().add(resultAttrs));
    return this;
  }

  public RespBuilder withResult() {
    response.put(RESULTS, new JsonArray());
    return this;
  }

  public RespBuilder withResult(JsonArray results) {
    response.put(RESULTS, results);
    return this;
  }

  public RespBuilder withResult(JsonObject results) {
    response.put(RESULTS, results);
    return this;
  }
  public JsonObject getJsonResponse() {
    return response;
  }

  public String getResponse() {
    return response.toString();
  }
}
