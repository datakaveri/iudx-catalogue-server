package iudx.catalogue.server.apiserver.stack;

import static iudx.catalogue.server.apiserver.stack.StackConstants.*;
import static iudx.catalogue.server.database.elastic.query.Queries.buildCheckStackExistenceQuery;
import static iudx.catalogue.server.database.elastic.query.Queries.buildGetStackQuery;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class QueryBuilder {

  Query getQuery(String stackId) {
    return buildGetStackQuery(stackId);
  }

  String getPatchQuery(JsonObject request) {
    StringBuilder query =
        new StringBuilder(
            PATCH_QUERY
                .replace("$1", request.getString("rel"))
                .replace("$2", request.getString("href")));
    if (request.containsKey("type")) {
      StringBuilder type = new StringBuilder(TYPE.replace("$1", request.getString("type")));
      query.append(',');
      query.append(type);
    }
    if (request.containsKey("title")) {
      StringBuilder title = new StringBuilder(TITLE.replace("$1", request.getString("title")));
      query.append(',');
      query.append(title);
    }
    query.append(CLOSED_QUERY);
    return query.toString();
  }

  Query getQuery4CheckExistence(JsonObject request) {
    JsonObject json = getHref(request.getJsonArray("links"));
    Query query = buildCheckStackExistenceQuery(json.getString("self"), json.getString("root"));
    return query;
  }

  private JsonObject getHref(JsonArray links) {
    JsonObject json = new JsonObject();
    links.stream()
        .map(JsonObject.class::cast)
        .forEach(
            child -> {
              if (child.getString("rel").equalsIgnoreCase("self")) {
                json.put("self", child.getString("href"));
              }
              if (child.getString("rel").equalsIgnoreCase("root")) {
                json.put("root", child.getString("href"));
              }
            });

    return json;
  }
}
