package iudx.catalogue.server.mlayer.vocabulary;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataModel {
  private static final Logger LOGGER = LogManager.getLogger(DataModel.class);

  private final WebClient webClient;

  public DataModel(int port, Vertx vertx) {
    WebClientOptions options = new WebClientOptions().setDefaultPort(port);
    this.webClient = WebClient.create(vertx, options);
  }

  public Future<JsonObject> getDataModelInfo(String base) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject classToSubclass = new JsonObject();
    String property = "[type]";
    String value = "[[iudx:ResourceGroup]]";
    LOGGER.debug("base:" + base);

    webClient
        .get(base + "/search")
        .addQueryParam("property", property)
        .addQueryParam("value", value)
        .send(
            ar -> {
              if (ar.succeeded()) {
                LOGGER.debug("Search request successful");
                LOGGER.debug(ar.result());
                HttpResponse<Buffer> response = ar.result();
                Buffer responseBody = response.body();
                LOGGER.debug("res: " + responseBody.toString());
                JsonObject json = responseBody.toJsonObject();
                JsonArray resultsArray = json.getJsonArray("results");
                String dmBasePath = resultsArray.getJsonObject(0).getString("@context");
                LOGGER.debug("dmBasePath: " + dmBasePath);

                if (resultsArray.isEmpty()) {
                  promise.complete(classToSubclass);
                  return;
                }

                AtomicInteger pendingRequests = new AtomicInteger(resultsArray.size());

                for (Object resultObj : resultsArray) {
                  if (!(resultObj instanceof JsonObject)) {
                    LOGGER.error(
                        "Unexpected type in results list: " + resultObj.getClass().getSimpleName());
                    if (pendingRequests.decrementAndGet() == 0) {
                      promise.complete(classToSubclass);
                    }
                    continue;
                  }

                  JsonObject result = (JsonObject) resultObj;
                  JsonArray typeArray = result.getJsonArray("type");

                  if (typeArray == null || typeArray.size() < 2) {
                    LOGGER.error("Invalid type array in result: " + result.encode());
                    if (pendingRequests.decrementAndGet() == 0) {
                      promise.complete(classToSubclass);
                    }
                    continue;
                  }

                  String type = typeArray.getString(1);
                  String classId = type.split(":")[1];
                  String dmUrl = dmBasePath + classId + ".jsonld";

                  webClient
                      .getAbs(dmUrl)
                      .send(
                          dmAr -> {
                            if (dmAr.succeeded()) {
                              HttpResponse<Buffer> dmResponse = dmAr.result();
                              Buffer dmBody = dmResponse.body();

                              if (dmBody == null) {
                                LOGGER.error("No response body received for URL: " + dmUrl);
                              } else if (!dmResponse
                                  .headers()
                                  .get("content-type")
                                  .contains("application/json")) {
                                LOGGER.error("Invalid content-type received for URL: " + dmUrl);
                              } else {
                                JsonObject dmJson;
                                try {
                                  dmJson = dmBody.toJsonObject();
                                } catch (Exception e) {
                                  LOGGER.error(
                                      "Failed to parse JSON response from URL: " + dmUrl, e);
                                  dmJson = null;
                                }

                                if (dmJson != null) {
                                  JsonArray graph = dmJson.getJsonArray("@graph");

                                  if (graph != null) {
                                    for (Object obj : graph) {
                                      if (obj instanceof JsonObject) {
                                        JsonObject jsonObj = (JsonObject) obj;
                                        if (("iudx:" + classId).equals(jsonObj.getString("@id"))) {
                                          JsonObject subClassOfObj =
                                              jsonObj.getJsonObject("rdfs:subClassOf");
                                          if (subClassOfObj != null) {
                                            String subClassIdStr = subClassOfObj.getString("@id");
                                            if (subClassIdStr != null
                                                && subClassIdStr.contains(":")) {
                                              String subClassId = subClassIdStr.split(":")[1];
                                              classToSubclass.put(
                                                  result.getString("id"), subClassId);
                                            } else {
                                              LOGGER.error(
                                                  "Invalid @id in rdfs:subClassOf for class ID: "
                                                      + classId);
                                            }
                                          } else {
                                            LOGGER.error(
                                                "Missing rdfs:subClassOf for class ID: " + classId);
                                          }
                                          break;
                                        }
                                      }
                                    }
                                  } else {
                                    LOGGER.error(
                                        "Invalid graph array in response for URL: " + dmUrl);
                                  }
                                }
                              }
                            } else {
                              LOGGER.error(
                                  "Failed to fetch data model for URL: " + dmUrl, dmAr.cause());
                            }
                            if (pendingRequests.decrementAndGet() == 0) {
                              promise.complete(classToSubclass);
                            }
                          });
                }
              } else {
                LOGGER.debug("Search request failed");
                promise.complete(classToSubclass);
              }
            });

    return promise.future();
  }
}
