package iudx.catalogue.server.database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.IOException;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;

import static iudx.catalogue.server.database.Constants.*;

/**
 * The Database Service Implementation.
 *
 * <h1>Database Service Implementation</h1>
 *
 * <p>
 * The Database Service implementation in the IUDX Catalogue Server implements the definitions of
 * the {@link iudx.catalogue.server.database.DatabaseService}.
 *
 * @version 1.0
 * @since 2020-05-31
 */
public class DatabaseServiceImpl implements DatabaseService {

  private static final Logger LOGGER = LogManager.getLogger(DatabaseServiceImpl.class);
  private final RestClient client;

  public DatabaseServiceImpl(RestClient client) {
    this.client = client;
  }



  @Override
  public DatabaseService searchQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    /* Initialize elastic clients and JsonObjects */
    Request elasticRequest;
    JsonObject errorJson = new JsonObject();
    request.put(SEARCH, true);
    // TODO: Stub code, to be removed

    // if (!request.containsKey("instanceId")) {
    // errorJson.put(STATUS, FAILED).put(DESCRIPTION, "No instanceId
    // found");
    // handler.handle(Future.failedFuture(errorJson.toString()));
    // return null;
    // }

    /* Validate the Request */
    if (!request.containsKey(SEARCH_TYPE)) {
      errorJson.put(STATUS, FAILED).put(DESCRIPTION,
          NO_SEARCH_TYPE_FOUND);
      handler.handle(Future.failedFuture(errorJson.toString()));
      return null;
    }
    /* Construct an elastic client request with index to query */
    elasticRequest =
        new Request(REQUEST_GET, CAT_SEARCH_INDEX + FILTER_PATH);
    /* Construct the query to be made */
    JsonObject query = queryDecoder(request);
    if (query.containsKey(ERROR)) {
      LOGGER.info("Query returned with an error");
      errorJson.put(STATUS, FAILED).put(DESCRIPTION,
          query.getString(ERROR));
      handler.handle(Future.failedFuture(errorJson.toString()));
      return null;
    }
    LOGGER.info("Query constructed: " + query.toString());
    /* Set the elastic client with the query to perform */
    elasticRequest.setJsonEntity(query.toString());
    /* Execute the query */
    client.performRequestAsync(elasticRequest, new ResponseListener() {
      @Override
      public void onSuccess(Response response) {
        LOGGER.info("Successful DB request");
        JsonArray dbResponse = new JsonArray();
        JsonObject dbResponseJson = new JsonObject();
        try {
          int statusCode = response.getStatusLine().getStatusCode();
          /* Validate the response */
          if (statusCode != 200 && statusCode != 204) {
            handler.handle(Future.failedFuture("Status code is not 2xx"));
            return;
          }
          JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));
          if (responseJson.getJsonObject(HITS).getJsonObject(TOTAL)
              .getInteger(VALUE) == 0) {
            errorJson.put(STATUS, FAILED).put(DESCRIPTION,
                EMPTY_RESPONSE);
            handler.handle(Future.failedFuture(errorJson.toString()));
            return;
          }
          JsonArray responseHits =
              responseJson.getJsonObject(HITS).getJsonArray(HITS);
          /* Construct the client response, remove the _source field */
          for (Object json : responseHits) {
            JsonObject jsonTemp = (JsonObject) json;
            dbResponse.add(jsonTemp.getJsonObject(SOURCE));
          }
          dbResponseJson.put(STATUS, SUCCESS)
              .put(TOTAL_HITS, responseJson.getJsonObject(HITS)
                  .getJsonObject(TOTAL).getInteger(VALUE))
              .put(RESULT, dbResponse);
          /* Send the response */
          handler.handle(Future.succeededFuture(dbResponseJson));
        } catch (IOException e) {
          LOGGER.info("DB ERROR:\n");
          e.printStackTrace();
          /* Handle request error */
          errorJson.put(STATUS, FAILED).put(DESCRIPTION,
              DATABASE_ERROR);
          handler.handle(Future.failedFuture(errorJson.toString()));
        }
      }

      @Override
      public void onFailure(Exception e) {
        LOGGER.info("DB request has failed. ERROR:\n");
        e.printStackTrace();
        /* Handle request error */
        errorJson.put(STATUS, FAILED).put(DESCRIPTION,
            DATABASE_ERROR);
        handler.handle(Future.failedFuture(errorJson.toString()));
      }
    });
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService countQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    JsonObject errorJson = new JsonObject();
    LOGGER.info("Inside countQuery<DatabaseService> block-------- " + request.toString());
    request.put(SEARCH, false);
    // if (!request.containsKey("instanceId")) {
    // errorJson.put(STATUS, FAILED).put(DESCRIPTION, "No instanceId
    // found");
    // handler.handle(Future.failedFuture(errorJson.toString()));
    // return null;
    // }
    /* Validate the Request */
    if (!request.containsKey(SEARCH_TYPE)) {
      errorJson.put(STATUS, FAILED).put(DESCRIPTION,
          NO_SEARCH_TYPE_FOUND);
      handler.handle(Future.failedFuture(errorJson.toString()));
      return null;
    }
    Request elasticRequest = new Request("GET", CAT_COUNT_INDEX);
    JsonObject query = queryDecoder(request);
    if (query.containsKey("Error")) {
      LOGGER.info("Query returned with an error");
      errorJson.put(STATUS, FAILED).put(DESCRIPTION,
          query.getString(ERROR));
      handler.handle(Future.failedFuture(errorJson.toString()));
      return null;
    }
    LOGGER.info("Query constructed: " + query.toString());
    elasticRequest.setJsonEntity(query.toString());
    client.performRequestAsync(elasticRequest, new ResponseListener() {
      @Override
      public void onSuccess(Response response) {
        LOGGER.info("Successful DB request");
        try {
          int statusCode = response.getStatusLine().getStatusCode();
          if (statusCode != 200 && statusCode != 204) {
            handler.handle(Future.failedFuture("Status code is not 2xx"));
            return;
          }
          JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));
          handler.handle(Future.succeededFuture(
              new JsonObject().put(COUNT, responseJson.getInteger(COUNT))));
        } catch (IOException e) {
          LOGGER.info("DB ERROR:\n");
          e.printStackTrace();
          /* Handle request error */
          errorJson.put(STATUS, FAILED).put(DESCRIPTION,
              DATABASE_ERROR);
          handler.handle(Future.failedFuture(errorJson.toString()));
        }
      }

      @Override
      public void onFailure(Exception e) {
        LOGGER.info("DB request has failed. ERROR:\n");
        e.printStackTrace();
        /* Handle request error */
        errorJson.put(STATUS, FAILED).put(DESCRIPTION,
            DATABASE_ERROR);
        handler.handle(Future.failedFuture(errorJson.toString()));
      }
    });
    return this;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService createItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    Request checkExisting;
    JsonObject checkQuery = new JsonObject();
    JsonObject errorJson = new JsonObject();
    String id = request.getString("id");

    errorJson.put(STATUS, FAILED).put(RESULTS,
        new JsonArray().add(new JsonObject().put(ID, id)
            .put(METHOD, INSERT).put(STATUS, FAILED)));

    checkExisting = new Request(REQUEST_GET, CAT_SEARCH_INDEX);

    checkQuery.put(SOURCE, "[\"\"]").put(QUERY_KEY,
        new JsonObject().put(TERM, new JsonObject().put(ID_KEYWORD, id)));
    LOGGER.info("Query constructed: " + checkQuery.toString());
    checkExisting.setJsonEntity(checkQuery.toString());

    client.performRequestAsync(checkExisting, new ResponseListener() {
      @Override
      public void onSuccess(Response response) {
        LOGGER.info("Successful DB request");
        int statusCode = response.getStatusLine().getStatusCode();
        LOGGER.info("status code: " + statusCode);
        if (statusCode != 200 && statusCode != 204) {
          handler.handle(Future.failedFuture("Status code is not 2xx"));
          return;
        }
        try {
          JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));
          if (responseJson.getJsonObject(HITS).getJsonObject(TOTAL)
              .getInteger(VALUE) > 0) {
            LOGGER.info("Item already exists.");
            handler.handle(Future.failedFuture(errorJson.toString()));
            return;
          } else {
            Request createRequest = new Request(REQUEST_POST, CAT_DOC);
            createRequest.setJsonEntity(request.toString());
            client.performRequestAsync(createRequest, new ResponseListener() {
              @Override
              public void onSuccess(Response response) {
                int statusCode = response.getStatusLine().getStatusCode();
                LOGGER.info("status code: " + statusCode);
                if (statusCode != 200 && statusCode != 201 && statusCode != 204) {
                  handler.handle(Future.failedFuture("Status code is not 2xx"));
                  return;
                }
                LOGGER.info("Successful DB request: Item Created");
                JsonObject responseJson = new JsonObject();
                responseJson.put(STATUS, SUCCESS).put(RESULTS,
                    new JsonArray().add(new JsonObject().put(ID, id)
                        .put(METHOD, INSERT)
                        .put(STATUS, SUCCESS)));
                handler.handle(Future.succeededFuture(responseJson));
              }

              @Override
              public void onFailure(Exception e) {
                LOGGER.info("DB request has failed. ERROR:\n");
                e.printStackTrace();
                /* Handle request error */
                handler.handle(Future.failedFuture(errorJson.toString()));
              }
            });
          }
        } catch (ParseException | IOException e) {
          LOGGER.info("DB ERROR:\n");
          e.printStackTrace();
          /* Handle request error */
          handler.handle(Future.failedFuture(errorJson.toString()));
        }
      }

      @Override
      public void onFailure(Exception e) {
        LOGGER.info("DB request has failed. ERROR:\n");
        e.printStackTrace();
        /* Handle request error */
        handler.handle(Future.failedFuture(errorJson.toString()));
      }
    });

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService updateItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    Request checkExisting;
    JsonObject checkQuery = new JsonObject();
    JsonObject errorJson = new JsonObject();
    String id = request.getString("id");
    errorJson.put(STATUS, FAILED).put(RESULTS,
        new JsonArray().add(new JsonObject().put(ID, id)
            .put(METHOD, UPDATE).put(STATUS, FAILED)));
    checkExisting = new Request(REQUEST_GET, CAT_SEARCH_INDEX);
    checkQuery.put(SOURCE, "[\"\"]").put(QUERY_KEY,
        new JsonObject().put(TERM, new JsonObject().put(ID_KEYWORD, id)));
    LOGGER.info("Query constructed: " + checkQuery.toString());
    checkExisting.setJsonEntity(checkQuery.toString());
    client.performRequestAsync(checkExisting, new ResponseListener() {
      @Override
      public void onSuccess(Response response) {
        LOGGER.info("Successful DB request");
        int statusCode = response.getStatusLine().getStatusCode();
        LOGGER.info("status code: " + statusCode);
        if (statusCode != 200 && statusCode != 204) {
          handler.handle(Future.failedFuture("Status code is not 2xx"));
          return;
        }
        try {
          Request updateRequest;
          JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));
          if (responseJson.getJsonObject(HITS).getJsonObject(TOTAL)
              .getInteger(VALUE) == 0) {
            LOGGER.info("Item Doesn't Exist in the Database");
            updateRequest = new Request(REQUEST_POST, CAT_DOC);
            LOGGER.info("Creating New Item");
          } else {
            LOGGER.info("Item found");
            String docId = responseJson.getJsonObject(HITS).getJsonArray(HITS)
                .getJsonObject(0).getString(DOC_ID);
            updateRequest = new Request(REQUEST_PUT, CAT_DOC + "/" + docId);
          }
          updateRequest.setJsonEntity(request.toString());
          client.performRequestAsync(updateRequest, new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
              int statusCode = response.getStatusLine().getStatusCode();
              LOGGER.info("status code: " + statusCode);
              if (statusCode != 200 && statusCode != 204 && statusCode != 201) {
                handler.handle(Future.failedFuture("Status code is not 2xx"));
                return;
              }
              LOGGER.info("Successful DB request: Item Updated");
              JsonObject responseJson = new JsonObject();
              responseJson.put(STATUS, SUCCESS).put(RESULTS,
                  new JsonArray().add(
                      new JsonObject().put(ID, id).put(METHOD, UPDATE)
                          .put(STATUS, SUCCESS)));
              handler.handle(Future.succeededFuture(responseJson));
            }

            @Override
            public void onFailure(Exception e) {
              LOGGER.info("DB request has failed. ERROR:\n");
              e.printStackTrace();
              /* Handle request error */
              handler.handle(Future.failedFuture(errorJson.toString()));
            }
          });

        } catch (ParseException | IOException e) {
          LOGGER.info("DB ERROR:\n");
          e.printStackTrace();
          /* Handle request error */
          handler.handle(Future.failedFuture(errorJson.toString()));
        }
      }

      @Override
      public void onFailure(Exception e) {
        LOGGER.info("DB request has failed. ERROR:\n");
        e.printStackTrace();
        /* Handle request error */
        handler.handle(Future.failedFuture(errorJson.toString()));
      }
    });

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService deleteItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    Request checkExisting;
    JsonObject checkQuery = new JsonObject();
    JsonObject errorJson = new JsonObject();
    String id = request.getString("id");
    errorJson.put(STATUS, FAILED).put(RESULTS,
        new JsonArray().add(new JsonObject().put(ID, id)
            .put(METHOD, DELETE).put(STATUS, FAILED)));
    checkExisting = new Request(REQUEST_GET, CAT_SEARCH_INDEX);
    checkQuery.put(SOURCE, "[\"\"]").put(QUERY_KEY,
        new JsonObject().put(TERM, new JsonObject().put(ID_KEYWORD, id)));
    checkExisting.setJsonEntity(checkQuery.toString());
    client.performRequestAsync(checkExisting, new ResponseListener() {
      @Override
      public void onSuccess(Response response) {
        int statusCode = response.getStatusLine().getStatusCode();
        LOGGER.info("status code: " + statusCode);
        if (statusCode != 200 && statusCode != 204) {
          handler.handle(Future.failedFuture("Status code is not 2xx"));
          return;
        }
        LOGGER.info("Successful DB request");
        try {
          LOGGER.info("Inside try block of deleteItem");
          Request updateRequest;
          JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));
          LOGGER.info("\n\n\n Response is \n\n\n");
          LOGGER.info(responseJson);
          if (responseJson.getJsonObject(HITS).getJsonObject(TOTAL)
              .getInteger(VALUE) == 0) {
            LOGGER.info("Item Doesn't exist");
            handler.handle(Future.failedFuture(errorJson.toString()));
            return;
          }
          LOGGER.info("Item Found");
          String docId = responseJson.getJsonObject(HITS).getJsonArray(HITS)
              .getJsonObject(0).getString(DOC_ID);
          updateRequest = new Request(REQUEST_DELETE, CAT_DOC + "/" + docId);
          updateRequest.setJsonEntity(request.toString());
          client.performRequestAsync(updateRequest, new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
              int statusCode = response.getStatusLine().getStatusCode();
              LOGGER.info("status code: " + statusCode);
              if (statusCode != 200 && statusCode != 204) {
                handler.handle(Future.failedFuture("Status code is not 2xx"));
                return;
              }
              LOGGER.info("Successful DB request: Item Deleted");
              JsonObject responseJson = new JsonObject();
              responseJson.put(STATUS, SUCCESS).put(RESULTS,
                  new JsonArray().add(
                      new JsonObject().put(ID, id).put(METHOD, DELETE)
                          .put(STATUS, SUCCESS)));
              handler.handle(Future.succeededFuture(responseJson));
            }

            @Override
            public void onFailure(Exception e) {
              LOGGER.info("DB request has failed. ERROR:\n");
              e.printStackTrace();
              /* Handle request error */
              handler.handle(Future.failedFuture(errorJson.toString()));
            }
          });

        } catch (ParseException | IOException e) {
          LOGGER.info("DB ERROR:\n");
          e.printStackTrace();
          /* Handle request error */
          handler.handle(Future.failedFuture(errorJson.toString()));
        }
      }

      @Override
      public void onFailure(Exception e) {
        LOGGER.info("DB request has failed. ERROR:\n");
        e.printStackTrace();
        /* Handle request error */
        handler.handle(Future.failedFuture(errorJson.toString()));
      }
    });

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use

    String result =
        "{ \"status\": \"success\", \"totalHits\": 100," + "\"limit\": 10, \"offset\": 100,"
            + "\"results\": [" + "{ \"id\": \"abc/123\", \"tags\": [ \"a\", \"b\"] } ] }";

    String errResult = " { \"status\": \"invalidValue\", \"results\": [] }";

    if (request.getString("id").contains("/")) {
      handler.handle(Future.succeededFuture(new JsonObject(result)));
    } else {
      handler.handle(Future.succeededFuture(new JsonObject(errResult)));
    }

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listTags(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use
    String result = "{ \"status\": \"success\", \"results\": [ \"environment\", \"civic\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listDomains(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use
    String result = "{ \"status\": \"success\", \"results\": [ \"environment\", \"civic\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listCities(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use
    String result = "{ \"status\": \"success\", \"results\": [ \"Pune\", \"Varanasi\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listResourceServers(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use
    String result = "{ \"status\": \"success\", \"results\": [ \"server-1\", \"server-2\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listProviders(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use [was not part of master code]
    String result = "{ \"status\": \"success\", \"results\": [ \"pr-1\", \"pr-2\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listResourceGroups(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use [was not part of master code]
    String result = "{ \"status\": \"success\", \"results\": [ \"rg-1\", \"rg-2\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listResourceRelationship(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use [was not part of master code]
    String result = "{ \"status\": \"success\", \"results\": [ \"rg-1\", \"rg-2\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listResourceGroupRelationship(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use [was not part of master code]
    String result = "{ \"status\": \"success\", \"results\": [ { \"id\": \"abc/123\" }] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listProviderRelationship(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    String result = "{ \"status\": \"success\", \"results\": [ \"rg-1\", \"rg-2\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listResourceServerRelationship(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    String result = "{ \"status\": \"success\", \"results\": [ \"rg-1\", \"rg-2\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listTypes(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    String result = "{ \"status\": \"success\", \"results\": [ \"rg-1\", \"rg-2\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService getCities(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    // TODO Auto-generated method stub

    String result = "{\n" + "\"status\": \"success\",\n" + "\"results\": [{\n"
        + "\"__instance-id\" : \"ui-test.iudx.org.in\",\n" + "\"configurations\" : {\n"
        + "\"smart_city_name\" : \"PSCDCL\",\n"
        + "\"map_default_view_lat_lng\" : [ 18.5644, 73.7858 ]\n" + "}\n" + "}, {\n"
        + "\"__instance-id\" : \"covid-19.iudx.org.in\",\n" + "\"configurations\" : {\n"
        + "\"smart_city_name\" : \"COVID-19\",\n"
        + "\"map_default_view_lat_lng\" : [ 18.5644, 73.7858 ]\n" + "}\n" + "}, {\n"
        + "\"__instance-id\" : \"pudx.catalogue.iudx.org.in\",\n" + "\"configurations\" " + ": {\n"
        + "\"smart_city_name\" : \"PSCDCL\",\n"
        + "\"map_default_view_lat_lng\" : [ 18.5644, 73.7858 ]\n" + "}\n" + "}, {\n"
        + "\"__instance-id\" : \"varanasi.iudx.org.in\",\n" + "\"configurations\" : {\n"
        + "\"smart_city_name\" : \"VSCL\",\n"
        + "\"map_default_view_lat_lng\" : [ 25.3176, 82.9739 ]\n" + "}\n" + "}]}";

    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService setCities(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    // TODO Auto-generated method stub

    String result = "{\n" + "    \"status\": \"success\",\n" + "    \"results\": [\n"
        + "        {\n" + "            \"instanceID\": \"ui-test.iudx.org.in\",\n"
        + "            \"configurations\": {\n"
        + "                \"smart_city_name\": \"PSCDCL\",\n"
        + "                \"map_default_view_lat_lng\": [\n" + "                    18.5644"
        + ",\n" + "                    73.7858\n" + "                ]\n" + "            }\n"
        + "        },\n" + "        {\n" + "            \"instanceID\": \"covid-19.iudx.org.i"
        + "n\"," + "\n" + "            \"configurations\": {\n"
        + "                \"smart_city_name\": \"COVID-19\",\n"
        + "                \"map_default_view_lat_lng\": [\n" + "                    18.5644,"
        + "\n" + "                    73.7858\n" + "                ]\n" + "            }\n"
        + "        },\n" + "        {\n"
        + "            \"instanceID\": \"pudx.catalogue.iudx.org.in\",\n"
        + "            \"configurations\": {\n"
        + "                \"smart_city_name\": \"PSCDCL\",\n"
        + "                \"map_default_view_lat_lng\": [\n" + "                    18.5644,"
        + "\n" + "                    73.7858\n" + "                ]\n" + "            }\n"
        + "        },\n" + "        {\n" + "            \"instanceID\": \"varanasi.iudx.org.i"
        + "n\",\n" + "            \"configurations\": {\n"
        + "                \"smart_city_name\": \"VSC" + "L\",\n"
        + "                \"map_default_view_lat_lng\": [\n" + "                    25.3176,\n"
        + "                    82.9739\n" + "                ]\n" + "            }\n"
        + "        }\n" + "    ]\n" + "}";

    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService updateCities(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO Auto-generated method stub

    String result = "{\n" + "\"status\": \"success\",\n" + "\"results\": [\n" + "{\n"
        + "\"instanceID\" : \"ui-test.iudx.org.in\",\n" + "\"configurations\" : {\n"
        + "\"smart_city_name\" : \"PSCDCL\",\n"
        + "\"map_default_view_lat_lng\" : [ 18.5644, 73.7858 ]\n" + "}\n" + "}, {\n"
        + "\"instanceID\" : \"covid-19.iudx.org.in\",\n" + "\"configurations\" : {\n"
        + "\"smart_city_name\" : \"COVID-19\",\n"
        + "\"map_default_view_lat_lng\" : [ 18.5644, 73.7858 ]\n" + "}\n" + "}, {\n"
        + "\"instanceID\" : \"pudx.catalogue.iudx.org.in\",\n" + "\"configurations\" : {\n"
        + "\"smart_city_name\" : \"PSCDCL\",\n"
        + "\"map_default_view_lat_lng\" : [ 18.5644, 73.7858 ]\n" + "}\n" + "}, {\n"
        + "\"instanceID\" : \"varanasi.iudx.org.in\",\n" + "\"configurations\" : {\n"
        + "\"smart_city_name\" : \"VSCL\",\n"
        + "\"map_default_view_lat_lng\" : [ 25.3176, 82.9739 ]\n" + "}\n" + "}]}";

    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService getConfig(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    // TODO Auto-generated method stub

    String result = "{\n" + "\"status\": \"success\",\n" + "\"results\": [{\n"
        + "\"__instance-id\" : \"varanasi.iudx.org.in\",\n" + "\"configurations\" : {\n"
        + "\"smart_city_iudx_logo\" : \"../assets/img/iudx_varanasi.jpeg\",\n"
        + "\"smart_city_name\" : \"VSCL\",\n" + "\"smart_city_url\" : \"#\",\n"
        + "\"resoure_server_base_URL\" : \"https://rs.varanasi.iudx.org.in/resource-server/vscl/v1"
        + "\",\n" + "\"auth_base_URL\" : \"https://auth.iudx.org.in/auth/v1\",\n"
        + "\"api_docs_link\" : \"https://apidocs.iudx.org.in\",\n"
        + "\"resource_server_group_head\" : \"urn:iudx-catalogue-varanasi:\",\n"
        + "\"provider_head\" : \"urn:iudx-catalogue-varanasi:\",\n"
        + "\"map_default_view_lat_lng\" : [ 25.3176, 82.9739 ],\n"
        + "\"map_default_lat_lng_name\" : \"VSCL Office\",\n" + "\"map_default_zoom\" : 12.0,\n"
        + "\"cat_base_URL\" : \"https://varanasi.iudx.org.in/catalogue/v1\"\n" + "},\n"
        + "\"legends\" : {\n"
        + "\"rs.varanasi.iudx.org.in/varanasi-swm-bins\" : \"https://image.flaticon.com/icons/svg/26"
        + "36/2636439.svg\",\n"
        + "\"rs.varanasi.iudx.org.in/varanasi-aqm\" : \"https://image.flaticon.com/icons/svg/1808/"
        + "180" + "8701.svg\",\n" + "\"rs.varanasi.iudx.org.in/varanasi-swm-vehicles\" : \"#\",\n"
        + "\"rs.varanasi.iudx.org.in/varanasi-citizen-app\" : \"#\",\n"
        + "\"rs.varanasi.iudx.org.in/varanasi-iudx-gis\" : \"#\",\n"
        + "\"rs.varanasi.iudx.org.in/varanasi-swm-workers\" : \"#\"\n" + "},\n"
        + "\"global_configuration\" : {\n" + "\"icon_attribution\" : {\n" + "\"author\" : [ {\n"
        + "\"freepik\" : \"https://www.flaticon.com/authors/freepik\"\n" + "}, {\n"
        + "\"smashicons\" : \"https://www.flaticon.com/authors/smashicons\"\n" + "}, {\n"
        + "\"flat-icons\" : \"https://www.flaticon.com/authors/flat-icons\"\n" + "}, {\n"
        + "\"itim2101\" : \"https://www.flaticon.com/authors/itim2101\"\n" + "} ],\n"
        + "\"site\" : \"flaticon.com\",\n" + "\"site_link\" : \"https://flaticon.com\"\n" + "}\n"
        + "}\n" + "}]}";

    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService updateConfig(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO Auto-generated method stub

    String result = "{\n" + "    \"status\": \"success\",\n" + "    \"results\": [\n"
        + "        {\n" + "            \"instance-id\": \"<iudx-instance>:id\",\n"
        + "            \"method\": \"update\",\n" + "            \"status\": \"success\"\n"
        + "        }\n" + "    ]\n" + "}";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService setConfig(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    // TODO Auto-generated method stub

    String result = "{\n" + "    \"status\": \"success\",\n" + "    \"results\": [\n"
        + "        {\n" + "            \"instance-id\": \"<iudx-instance>:id\",\n"
        + "            \"method\": \"insert\",\n" + "            \"status\": \"success\"\n"
        + "        }\n" + "    ]\n" + "}";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService deleteConfig(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO Auto-generated method stub

    String result = "{\n" + "    \"status\": \"success\",\n" + "    \"results\": [\n"
        + "        {\n" + "            \"instance-id\": \"<iudx-instance>:id\",\n"
        + "            \"method\": \"delete\",\n" + "            \"status\": \"success\"\n"
        + "        }\n" + "    ]\n" + "}";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService appendConfig(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO Auto-generated method stub

    String result = "{\n" + "    \"status\": \"success\",\n" + "    \"results\": [\n"
        + "        {\n" + "            \"instance-id\": \"<iudx-instance>:id\",\n"
        + "            \"method\": \"patch\",\n" + "            \"status\": \"success\"\n"
        + "        }\n" + "    ]\n" + "}";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * Decodes and constructs ElasticSearch Search/Count query based on the parameters passed in the
   * request.
   *
   * @param request Json object containing various fields related to query-type.
   * @return JsonObject which contains fully formed ElasticSearch query.
   */
  public JsonObject queryDecoder(JsonObject request) {
    String searchType = request.getString(SEARCH_TYPE);
    JsonObject elasticQuery = new JsonObject();
    Boolean match = false;
    JsonObject boolObject = new JsonObject().put(BOOL_KEY, new JsonObject());
    /* TODO: Pagination for large result set */
    if (request.getBoolean(SEARCH)) {
      elasticQuery.put(SIZE_KEY, 10);
    }
    // Will be used for multi-tenancy
    // String instanceId = request.getString("instanceId");
    JsonArray filterQuery = new JsonArray();
    JsonArray mustQuery = new JsonArray();
    // Will be used for multi-tenancy
    // JsonObject termQuery =
    // new JsonObject().put("term", new JsonObject()
    // .put(INSTANCE_ID_KEY + ".keyword", instanceId));
    // filterQuery.add(termQuery);

    /* Handle the search type */
    if (searchType.matches(GEOSEARCH_REGEX)) {
      LOGGER.info("In geoSearch block---------");
      match = true;
      JsonObject shapeJson = new JsonObject();
      JsonObject geoSearch = new JsonObject();
      String relation;
      JsonArray coordinates;
      /* Construct the search query */
      if (request.containsKey(GEOMETRY)
          && request.getString(GEOMETRY).equalsIgnoreCase(POINT)
          && request.containsKey(GEORELATION)
          && request.containsKey(COORDINATES_KEY)
          && request.containsKey(GEOPROPERTY)
          && request.containsKey(MAX_DISTANCE)) {
        /* Construct the query for Circle */
        coordinates = request.getJsonArray(COORDINATES_KEY);
        int radius = request.getInteger(MAX_DISTANCE);
        // int radius = Integer.parseInt(request.getString(MAX_DISTANCE));
        relation = request.getString(GEORELATION);
        shapeJson
            .put(SHAPE_KEY,
                new JsonObject().put(TYPE_KEY, GEO_CIRCLE)
                    .put(COORDINATES_KEY, coordinates)
                    .put(GEO_RADIUS, radius + DISTANCE_IN_METERS))
            .put(GEO_RELATION_KEY, relation);
      } else if (request.containsKey(GEOMETRY)
          && (request.getString(GEOMETRY).equalsIgnoreCase(POLYGON)
              || request.getString(GEOMETRY).equalsIgnoreCase(LINESTRING))
          && request.containsKey(GEORELATION)
          && request.containsKey(COORDINATES_KEY)
          && request.containsKey(GEOPROPERTY)) {
        /* Construct the query for Line String, Polygon */
        String geometry = request.getString(GEOMETRY);
        relation = request.getString(GEORELATION);
        coordinates = request.getJsonArray(COORDINATES_KEY);
        int length = coordinates.getJsonArray(0).size();
        if (geometry.equalsIgnoreCase(POLYGON)
            && !coordinates.getJsonArray(0).getJsonArray(0).getDouble(0)
                .equals(coordinates.getJsonArray(0).getJsonArray(length - 1).getDouble(0))
            && !coordinates.getJsonArray(0).getJsonArray(0).getDouble(1)
                .equals(coordinates.getJsonArray(0).getJsonArray(length - 1).getDouble(1))) {
          return new JsonObject().put(ERROR, ERROR_INVALID_COORDINATE_POLYGON);
        }
        shapeJson
            .put(SHAPE_KEY, new JsonObject().put(TYPE_KEY, geometry)
                .put(COORDINATES_KEY, coordinates))
            .put(GEO_RELATION_KEY, relation);
      } else if (request.containsKey(GEOMETRY)
          && request.getString(GEOMETRY).equalsIgnoreCase(BBOX)
          && request.containsKey(GEORELATION)
          && request.containsKey(COORDINATES_KEY)
          && request.containsKey(GEOPROPERTY)) {
        /* Construct the query for BBOX */
        relation = request.getString(GEORELATION);
        coordinates = request.getJsonArray(COORDINATES_KEY);
        shapeJson = new JsonObject();
        shapeJson
            .put(SHAPE_KEY,
                new JsonObject().put(TYPE_KEY, GEO_BBOX)
                    .put(COORDINATES_KEY, coordinates))
            .put(GEO_RELATION_KEY, relation);

      } else {
        return new JsonObject().put(ERROR, ERROR_INVALID_GEO_PARAMETER);
      }
      geoSearch.put(GEO_SHAPE_KEY, new JsonObject().put(GEO_KEY, shapeJson));
      filterQuery.add(geoSearch);

    }

    /* Construct the query for text based search */
    if (searchType.matches(TEXTSEARCH_REGEX)) {
      LOGGER.info("Text search block");

      match = true;
      /* validating tag search attributes */
      if (request.containsKey(Q_KEY) && !request.getString(Q_KEY).isBlank()) {

        /* fetching values from request */
        String textAttr = request.getString(Q_KEY);

        /* constructing db queries */
        mustQuery.add(new JsonObject().put(STRING_QUERY_KEY,
            new JsonObject().put(QUERY_KEY, textAttr)));
      }
    }

    /* Construct the query for attribute based search */
    if (searchType.matches(ATTRIBUTE_SEARCH_REGEX)) {
      LOGGER.info("Attribute search block");

      match = true;

      /* validating tag search attributes */
      if (request.containsKey(PROPERTY)
          && !request.getJsonArray(PROPERTY).isEmpty()
          && request.containsKey(VALUE)
          && !request.getJsonArray(VALUE).isEmpty()) {

        /* fetching values from request */
        JsonArray propertyAttrs = request.getJsonArray(PROPERTY);
        JsonArray valueAttrs = request.getJsonArray(VALUE);

        /* For attribute property and values search */
        if (propertyAttrs.size() == valueAttrs.size()) {

          /* Mapping and constructing the value attributes with the property attributes for query */
          for (int i = 0; i < valueAttrs.size(); i++) {
            JsonObject boolQuery = new JsonObject();
            JsonArray shouldQuery = new JsonArray();
            JsonArray valueArray = valueAttrs.getJsonArray(i);

            for (int j = 0; j < valueArray.size(); j++) {
              JsonObject matchQuery = new JsonObject();

              /* Attribute related queries using "match" and without the ".keyword" */
              if (propertyAttrs.getString(i).equals(TAGS)
                  || propertyAttrs.getString(i).equals(DESCRIPTION_ATTR)
                  || propertyAttrs.getString(i).startsWith(LOCATION)) {

                matchQuery.put(propertyAttrs.getString(i), valueArray.getString(j));
                shouldQuery.add(new JsonObject().put(MATCH_KEY, matchQuery));

                /* Attribute related queries using "match" and with the ".keyword" */
              } else {
                /* checking keyword in the query paramters */
                if (propertyAttrs.getString(i).endsWith(KEYWORD_KEY)) {
                  matchQuery.put(propertyAttrs.getString(i), valueArray.getString(j));

                }else {

                  /* add keyword if not avaialble */
                  matchQuery.put(propertyAttrs.getString(i).concat(KEYWORD_KEY),
                      valueArray.getString(j));
                }
                shouldQuery.add(new JsonObject().put(MATCH_KEY, matchQuery));
              }
            }
            mustQuery.add(new JsonObject().put(BOOL_KEY,
                boolQuery.put(SHOULD_KEY, shouldQuery)));
          }
        } else {
          return new JsonObject().put(ERROR, ERROR_INVALID_PARAMETER);
        }
      }
    }

    /* checking the requests for limit attribute */
    if (request.containsKey(LIMIT)) {
      Integer sizeFilter = request.getInteger(LIMIT);
      elasticQuery.put(SIZE, sizeFilter);
    }

    /* checking the requests for offset attribute */
    if (request.containsKey(OFFSET)) {
      Integer offsetFilter = request.getInteger(OFFSET);
      elasticQuery.put(FROM, offsetFilter);
    }

    if (searchType.matches(RESPONSE_FILTER_REGEX)) {
      /* Construct the filter for response */
      LOGGER.info("In responseFilter block---------");
      match = true;
      if (!request.getBoolean(SEARCH)) {
        return new JsonObject().put("Error", COUNT_UNSUPPORTED);
      }
      if (request.containsKey(ATTRIBUTE)) {
        JsonArray sourceFilter = request.getJsonArray(ATTRIBUTE);
        elasticQuery.put(SOURCE, sourceFilter);
      } else if (request.containsKey(FILTER_KEY)) {
        JsonArray sourceFilter = request.getJsonArray(FILTER_KEY);
        elasticQuery.put(SOURCE, sourceFilter);
        elasticQuery.put(SOURCE, sourceFilter);
      } else {
        return new JsonObject().put(ERROR, ERROR_INVALID_RESPONSE_FILTER);
      }
    }

    if (!match) {
      return new JsonObject().put("Error", INVALID_SEARCH);
    } else {
      /* return fully formed elastic query */
      boolObject.getJsonObject(BOOL_KEY).put(FILTER_KEY, filterQuery)
          .put(MUST_KEY, mustQuery);
      return elasticQuery.put(QUERY_KEY, boolObject);
    }
  }
}
