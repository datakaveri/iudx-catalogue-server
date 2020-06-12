package iudx.catalogue.server.apiserver;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ResponseHandler {

  private final String status;
  private JsonArray results = new JsonArray();

  public ResponseHandler(String status, JsonArray results) {
    super();
    this.status = status;
    this.results = results;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.put("status", status);
    json.put("results", results);
    return json;
  }

  public String toJsonString() {
    return toJson().toString();
  }

  public static class Builder {
    private String status;
    private JsonArray results = new JsonArray();

    public Builder() {}

    public Builder withStatus(String status) {
      this.status = status;
      return this;
    }

    public Builder withResults(JsonArray results) {
      if (results == null) {
        this.results = new JsonArray();
      } else {
        this.results = results;
      }
      return this;
    }

    public ResponseHandler build() {
      return new ResponseHandler(status, results);
    }

  }

}
