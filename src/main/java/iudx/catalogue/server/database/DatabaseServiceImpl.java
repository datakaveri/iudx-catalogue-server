package iudx.catalogue.server.database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * The Database Service Implementation.
 * <h1>Database Service Implementation</h1>
 * <p>
 * The Database Service implementation in the IUDX Catalogue Server implements the definitions of
 * the {@link iudx.catalogue.server.database.DatabaseService}.
 * </p>
 * 
 * @version 1.0
 * @since 2020-05-31
 */

public class DatabaseServiceImpl implements DatabaseService {

  private static final Logger logger = LoggerFactory.getLogger(DatabaseServiceImpl.class);

  /**
   * {@inheritDoc}
   */

  @Override
  public DatabaseService searchQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    // TODO: Stub code, to be removed
    JsonObject result = null;
    String resCircle =
        "{\"status\":\"success\",\"totalHits\":200,\"limit\":1,\"offset\":100,\"results\":"
            + "[{\"id\":\"rbccps.org\\/aa9d66a000d94a78895de8d4c0b3a67f3450e531\\/rs.varanasi."
            + "iudx.org.in\\/varanasi-aqm\\/EM_01_0103_01\",\"tags\":[\"environment\","
            + "\"air quality\","
            + "\"air\",\"aqi\",\"aqm\",\"climo\",\"climate\",\"pollution\",\"so2\",\"co2\","
            + "\"co\",\"no\"" + ",\"no2\",\"pm2.5\","
            + "\"pm25\",\"lux\",\"pm10\",\"humidity\",\"temperature\",\"ozone\",\"o3\","
            + "\"noise\",\"light\",\"uv\"]}]}";

    String resPolygon =
        "{\"status\":\"success\",\"totalHits\":200,\"limit\":1,\"offset\":100,\"results\":"
            + "[{\"id\":"
            + "\"rbccps.org\\/aa9d66a000d94a78895de8d4c0b3a67f3450e531\\/rs.varanasi.iudx.org."
            + "in\\/" + "varanasi-aqm"
            + "\\/EM_01_0103_01\",\"tags\":{\"type\":\"Property\",\"value\":[\"environment\","
            + "\"air quality\",\"air\","
            + "\"aqi\",\"aqm\",\"climo\",\"climate\",\"pollution\",\"so2\",\"co2\",\"co\",\"no\","
            + "\"no2\",\"pm2.5\","
            + "\"pm25\",\"lux\",\"pm10\",\"humidity\",\"temperature\",\"ozone\",\"o3\",\"noise\","
            + "\"light\",\"uv\"]}}]}";

    if ("Point".equals(request.getString("geometry"))) {
      result = new JsonObject(resCircle);

    } else if ("Polygon".equals(request.getString("geometry"))) {
      result = new JsonObject(resPolygon);
    }

    if (result != null) {
      handler.handle(Future.succeededFuture(result));
    }

    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DatabaseService countQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }


  /**
   * {@inheritDoc}
   */

  @Override
  public DatabaseService createItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    // TODO: Stub code, to be removed after use
    JsonObject result = new JsonObject("{\"status\": \"success\",\"results\": [{\"id\": \"" + "rb"
        + "ccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx"
        + ".org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live"
        + "\",\"method\": \"insert\",\"status\": \"success\" }]}");

    handler.handle(Future.succeededFuture(result));

    return null;

  }


  /**
   * {@inheritDoc}
   */

  @Override
  public DatabaseService updateItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    // TODO: Stub code, to be removed after use
    JsonObject result = new JsonObject("{\"status\": \"success\",\"results\": [{\"id\": \""
        + "rbccps.org"
        + "/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-swm-vehicle"
        + "s/varanasi-s" + "wm-vehicles-live"
        + "\",\"method\": \"update\",\"status\": \"success\" }]}");

    handler.handle(Future.succeededFuture(result));

    return null;
  }


  /**
   * {@inheritDoc}
   */

  @Override
  public DatabaseService deleteItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    // TODO: Stub code, to be removed after use
    JsonObject result = new JsonObject("{\"status\": \"success\",\"results\": [{\"id\": \"" + "rb"
        + "ccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.or"
        + "g.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live"
        + "\",\"method\": \"delete\",\"status\": \"success\" }]}");

    handler.handle(Future.succeededFuture(result));

    return null;
  }


  /**
   * {@inheritDoc}
   */

  @Override
  public DatabaseService listItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    // TODO: Stub code, to be removed after use
    JsonObject result = new JsonObject("{\n" + "        \"status\": \"success\",\n"
        + "        \"results\": [{\n"
        + "                        \"id\": \"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/"
        + "rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_01\",\n"
        + "                        \"tags\": {\n"
        + "                                \"type\": \"Property\",\n"
        + "                                \"value\": [\"environment\", \"air quality\", \"air\", "
        + "\"aqi\", \"aqm\", \"climo\", \"climate\", \"pollution\", \"so2\", \"co2\", \"co\","
        + " \"no\", "
        + "\"no2\", \"pm2.5\", \"pm25\", \"lux\", \"pm10\", \"humidity\", \"temperature\", "
        + "\"ozone\"," + " \"o3\", \"noise\", \"light\", \"uv\"]\n" + "                        }\n"
        + "                }\n" + "        ]\n" + "}");

    JsonObject errResult = new JsonObject(
        "{\n" + "        \"status\": \"invalidValue\",\n" + "        \"results\": [   ]\n" + "}");

    if (request.getString("id").contains("/")) {
      handler.handle(Future.succeededFuture(result));
    } else {
      handler.handle(Future.succeededFuture(errResult));
    }

    return null;
  }


  /**
   * {@inheritDoc}
   */

  @Override
  public DatabaseService listTags(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    // TODO: Stub code, to be removed after use
    JsonObject result = new JsonObject("{\n" + "        \"status\": \"success\",\n"
        + "        \"results\": [\"environment\", \"air quality\", \"air\", \"aqi\", \"aqm\", "
        + "\"climo\", \"climate\", \"pollution\", \"so2\", \"co2\", \"co\", \"no\", \"no2\", "
        + "\"pm2.5\", \"pm25\", \"lux\", \"pm10\", \"humidity\", \"temperature\", \"ozone\", "
        + "\"o3\", \"noise\", \"light\", \"uv\"]                      \n" + "}");

    handler.handle(Future.succeededFuture(result));

    return null;
  }


  /**
   * {@inheritDoc}
   */

  @Override
  public DatabaseService listDomains(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    // TODO: Stub code, to be removed after use
    JsonObject result = new JsonObject("{\n" + "        \"status\": \"success\",\n"
        + "        \"results\": [\"<iudx-voc-iri>:Environment\", \"<iudx-voc-iri>:Civic\", "
        + "\"<iudx-voc-iri>:Water\", \"<iudx-voc-iri>:Streetlighting\"]                      \n"
        + "}");

    handler.handle(Future.succeededFuture(result));

    return null;
  }


  /**
   * {@inheritDoc}
   */

  @Override
  public DatabaseService listCities(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    // TODO: Stub code, to be removed after use
    JsonObject result = new JsonObject("{\n" + "        \"status\": \"success\",\n"
        + "        \"results\": [\"<iudx-iri>:Varanasi\", \"<iudx-iri>:Pune\"]\n" + "}");

    handler.handle(Future.succeededFuture(result));

    return null;
  }


  /**
   * {@inheritDoc}
   */

  @Override
  public DatabaseService listResourceServers(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    // TODO: Stub code, to be removed after use
    JsonObject result = new JsonObject("{\n" + "        \"status\": \"success\",\n"
        + "        \"results\": [\" <iudx>:rs1\", \"<iudx>:rs2\"]\n" + "}");

    handler.handle(Future.succeededFuture(result));

    return null;
  }


  /**
   * {@inheritDoc}
   */

  @Override
  public DatabaseService listProviders(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    // TODO: Stub code, to be removed after use [was not part of master code]
    JsonObject result = new JsonObject("{\n" + "        \"status\": \"success\",\n"
        + "        \"results\": [ \"<iudx>:p1\", \"<iudx>:p2\"]\n" + "}");

    handler.handle(Future.succeededFuture(result));

    return null;
  }


  /**
   * {@inheritDoc}
   */

  @Override
  public DatabaseService listResourceGroups(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    // TODO: Stub code, to be removed after use [was not part of master code]
    JsonObject result = new JsonObject("{\n" + "        \"status\": \"success\",\n"
        + "        \"results\": [  \"<iudx>:rg1\", \"<iudx>:rg2\"]\n" + "}");

    handler.handle(Future.succeededFuture(result));

    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DatabaseService listResourceRelationship(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }


  /**
   * {@inheritDoc}
   */

  @Override
  public DatabaseService listResourceGroupRelationship(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }


  /**
   * {@inheritDoc}
   */

  @Override
  public DatabaseService listProviderRelationship(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }


  /**
   * {@inheritDoc}
   */

  @Override
  public DatabaseService listResourceServerRelationship(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }


  /**
   * {@inheritDoc}
   */

  @Override
  public DatabaseService listTypes(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * The queryDecoder implements the query decoder module.
   * 
   * @param request which is a JsonObject
   * @return JsonObject which is a JsonObject
   */

  public JsonObject queryDecoder(JsonObject request) {

    return null;
  }

}
