package iudx.catalogue.server.apiserver.stack;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import netscape.javascript.JSObject;

public interface StackSevice {

  Future<JsonObject> get(String stackId);

  Future<JsonObject> create(JsonObject stackObj);

  Future<JsonObject> update(JsonObject childObj);

  Future<JsonObject> delete(String stackId);
}
