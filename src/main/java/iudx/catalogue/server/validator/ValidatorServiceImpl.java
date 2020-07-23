package iudx.catalogue.server.validator;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;

/**
 * The Validator Service Implementation.
 *
 * <h1>Validator Service Implementation</h1>
 *
 * <p>The Validator Service implementation in the IUDX Catalogue Server implements the definitions
 * of the {@link iudx.catalogue.server.validator.ValidatorService}.
 *
 * @version 1.0
 * @since 2020-05-31
 */
public class ValidatorServiceImpl implements ValidatorService {

  private static final Logger logger = LoggerFactory.getLogger(ValidatorServiceImpl.class);
  private boolean isValidSchema;
  private Validator resourceValidator;
  private Validator resourceGroupValidator;
  private Validator providerValidator;
  private Validator resourceServerValidator;
  private final RestClient client;
  private Set<String> itemTypes;

  public ValidatorServiceImpl(RestClient client) {
    this.client = client;
    try {
      resourceValidator = new Validator("/resourceItemSchema.json");
      resourceGroupValidator = new Validator("/resourceGroupItemSchema.json");
      resourceServerValidator = new Validator("/resourceServerItemSchema.json");
      providerValidator = new Validator("/providerItemSchema.json");
    } catch (IOException | ProcessingException e) {
      e.printStackTrace();
    }
    itemTypes = new HashSet<String>();
    itemTypes.add(Constants.ITEM_TYPE_RESOURCE);
    itemTypes.add(Constants.ITEM_TYPE_RESOURCE_GROUP);
    itemTypes.add(Constants.ITEM_TYPE_RESOURCE_SERVER);
    itemTypes.add(Constants.ITEM_TYPE_PROVIDER);
  }

  /** {@inheritDoc} */
  @SuppressWarnings("unchecked")
  public ValidatorService validateSchema(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    Set<String> type = new HashSet<String>(new JsonArray().getList());
    try {
      type = new HashSet<String>(request.getJsonArray(Constants.TYPE).getList());
    } catch (Exception e) {
      logger.error("Item type mismatch");
    }
    type.retainAll(itemTypes);
    String itemType = type.toString().replaceAll("\\[", "").replaceAll("\\]", "");

    System.out.println("itemType: " + itemType);
    if (itemType.equalsIgnoreCase(Constants.ITEM_TYPE_RESOURCE)) {
      isValidSchema = resourceValidator.validate(request.toString());
    } else if (itemType.equalsIgnoreCase(Constants.ITEM_TYPE_RESOURCE_GROUP)) {
      isValidSchema = resourceGroupValidator.validate(request.toString());
    } else if (itemType.equalsIgnoreCase(Constants.ITEM_TYPE_RESOURCE_SERVER)) {
      isValidSchema = resourceServerValidator.validate(request.toString());
    } else if (itemType.equalsIgnoreCase(Constants.ITEM_TYPE_PROVIDER)) {
      isValidSchema = providerValidator.validate(request.toString());
    } else {
      isValidSchema = false;
    }
    if (isValidSchema) {
      handler.handle(
          Future.succeededFuture(new JsonObject().put(Constants.STATUS, Constants.SUCCESS)));
    } else {
      handler.handle(
          Future.failedFuture(new JsonObject().put(Constants.STATUS, Constants.FAILED).toString()));
    }
    return null;
  }

  /** {@inheritDoc} */
  @SuppressWarnings("unchecked")
  @Override
  public ValidatorService validateItem(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    Set<String> type = new HashSet<String>(new JsonArray().getList());
    try {
      type = new HashSet<String>(request.getJsonArray(Constants.TYPE).getList());
    } catch (Exception e) {
      logger.error("Item type mismatch");
    }
    type.retainAll(itemTypes);
    String itemType = type.toString().replaceAll("\\[", "").replaceAll("\\]", "");
    System.out.println("itemType: " + itemType);
    if (itemType.equalsIgnoreCase(Constants.ITEM_TYPE_RESOURCE)) {
      String resourceGroup = request.getString(Constants.RESOURCE_GROUP_KEY);
      String id = resourceGroup + "/" + request.getString(Constants.NAME);
      logger.info("id generated: " + id);
      request
          .put(Constants.ID, id)
          .put(Constants.ITEM_STATUS, Constants.ACTIVE)
          .put(Constants.ITEM_CREATED_AT, getUtcDatetimeAsString());

      Request checkResourceGroup =
          new Request(Constants.REQUEST_GET, Constants.CAT_TEST_SEARCH_INDEX);
      JsonObject checkQuery = new JsonObject();
      checkQuery
          .put(Constants.SOURCE, "[\"\"]")
          .put(
              Constants.QUERY_KEY,
              new JsonObject()
                  .put(Constants.TERM, new JsonObject().put(Constants.ID_KEYWORD, resourceGroup)));
      logger.info("Query constructed: " + checkQuery.toString());
      checkResourceGroup.setJsonEntity(checkQuery.toString());
      client.performRequestAsync(
          checkResourceGroup,
          new ResponseListener() {

            @Override
            public void onSuccess(Response response) {
              int statusCode = response.getStatusLine().getStatusCode();
              logger.info("status code: " + statusCode);
              if (statusCode != 200 && statusCode != 204) {
                handler.handle(Future.failedFuture("Status code is not 2xx"));
                return;
              }
              try {
                JsonObject responseJson =
                    new JsonObject(EntityUtils.toString(response.getEntity()));
                if (responseJson
                        .getJsonObject(Constants.HITS)
                        .getJsonObject(Constants.TOTAL)
                        .getInteger(Constants.VALUE)
                    > 0) {
                  logger.info("resource group validated");
                  handler.handle(Future.succeededFuture(request));
                  return;
                } else {
                  logger.info("invalid resource group");
                  handler.handle(
                      Future.failedFuture(
                          new JsonObject().put(Constants.STATUS, Constants.FAILED).toString()));
                }
              } catch (ParseException | IOException e) {
                logger.info("DB ERROR:\n");
                e.printStackTrace();
                /* Handle request error */
                handler.handle(
                    Future.failedFuture(
                        new JsonObject().put(Constants.STATUS, Constants.FAILED).toString()));
              }
            }

            @Override
            public void onFailure(Exception e) {
              logger.info("DB request has failed. ERROR:\n");
              e.printStackTrace();
              handler.handle(
                  Future.failedFuture(
                      new JsonObject().put(Constants.STATUS, Constants.FAILED).toString()));
            }
          });
    } else if (itemType.equalsIgnoreCase(Constants.ITEM_TYPE_PROVIDER)) {
      handler.handle(
          Future.succeededFuture(new JsonObject().put(Constants.STATUS, Constants.SUCCESS)));
    } else if (itemType.equalsIgnoreCase(Constants.ITEM_TYPE_RESOURCE_GROUP)) {
      String resourceServer = request.getString(Constants.RESOURCE_SERVER_KEY);
      String[] domain = resourceServer.split("/");
      String provider = request.getString(Constants.PROVIDER_KEY);
      String name = request.getString(Constants.NAME);
      String id = provider + "/" + domain[2] + "/" + name;
      logger.info("id generated: " + id);
      request
          .put(Constants.ID, id)
          .put(Constants.ITEM_STATUS, Constants.ACTIVE)
          .put(Constants.ITEM_CREATED_AT, getUtcDatetimeAsString());
      Request checkResourceServer =
          new Request(Constants.REQUEST_GET, Constants.CAT_TEST_SEARCH_INDEX);
      JsonObject checkQuery = new JsonObject();
      checkQuery
          .put(Constants.SOURCE, "[\"\"]")
          .put(
              Constants.QUERY_KEY,
              new JsonObject()
                  .put(Constants.TERM, new JsonObject().put(Constants.ID_KEYWORD, resourceServer)));
      logger.info("Query constructed: " + checkQuery.toString());
      checkResourceServer.setJsonEntity(checkQuery.toString());
      client.performRequestAsync(
          checkResourceServer,
          new ResponseListener() {

            @Override
            public void onSuccess(Response response) {
              int statusCode = response.getStatusLine().getStatusCode();
              logger.info("status code: " + statusCode);
              if (statusCode != 200 && statusCode != 204) {
                handler.handle(Future.failedFuture("Status code is not 2xx"));
                return;
              }
              try {
                JsonObject responseJson =
                    new JsonObject(EntityUtils.toString(response.getEntity()));
                if (responseJson
                        .getJsonObject(Constants.HITS)
                        .getJsonObject(Constants.TOTAL)
                        .getInteger(Constants.VALUE)
                    > 0) {
                  logger.info("ResourceServer validated");
                  Request checkProvider =
                      new Request(Constants.REQUEST_GET, Constants.CAT_TEST_SEARCH_INDEX);
                  JsonObject checkQuery = new JsonObject();
                  checkQuery
                      .put(Constants.SOURCE, "[\"\"]")
                      .put(
                          Constants.QUERY_KEY,
                          new JsonObject()
                              .put(
                                  Constants.TERM,
                                  new JsonObject().put(Constants.ID_KEYWORD, provider)));
                  logger.info("Query constructed: " + checkQuery.toString());
                  checkProvider.setJsonEntity(checkQuery.toString());
                  client.performRequestAsync(
                      checkProvider,
                      new ResponseListener() {

                        @Override
                        public void onSuccess(Response response) {
                          int statusCode = response.getStatusLine().getStatusCode();
                          logger.info("status code: " + statusCode);
                          if (statusCode != 200 && statusCode != 204) {
                            handler.handle(Future.failedFuture("Status code is not 2xx"));
                            return;
                          }
                          try {
                            JsonObject responseJson =
                                new JsonObject(EntityUtils.toString(response.getEntity()));
                            if (responseJson
                                    .getJsonObject(Constants.HITS)
                                    .getJsonObject(Constants.TOTAL)
                                    .getInteger(Constants.VALUE)
                                > 0) {
                              logger.info("Provider validated");
                              handler.handle(Future.succeededFuture(request));
                              return;
                            } else {
                              logger.info("invalid provider");
                              handler.handle(
                                  Future.failedFuture(
                                      new JsonObject()
                                          .put(Constants.STATUS, Constants.FAILED)
                                          .toString()));
                            }
                          } catch (ParseException | IOException e) {
                            logger.info("DB ERROR:\n");
                            e.printStackTrace();
                            /* Handle request error */
                            handler.handle(
                                Future.failedFuture(
                                    new JsonObject()
                                        .put(Constants.STATUS, Constants.FAILED)
                                        .toString()));
                          }
                        }

                        @Override
                        public void onFailure(Exception e) {
                          logger.info("DB request has failed. ERROR:\n");
                          e.printStackTrace();
                          handler.handle(
                              Future.failedFuture(
                                  new JsonObject()
                                      .put(Constants.STATUS, Constants.FAILED)
                                      .toString()));
                        }
                      });
                } else {
                  logger.info("invalid ResourceServer");
                  handler.handle(
                      Future.failedFuture(
                          new JsonObject().put(Constants.STATUS, Constants.FAILED).toString()));
                }
              } catch (ParseException | IOException e) {
                logger.info("DB ERROR:\n");
                e.printStackTrace();
                /* Handle request error */
                handler.handle(
                    Future.failedFuture(
                        new JsonObject().put(Constants.STATUS, Constants.FAILED).toString()));
              }
            }

            @Override
            public void onFailure(Exception e) {
              logger.info("DB request has failed. ERROR:\n");
              e.printStackTrace();
              handler.handle(
                  Future.failedFuture(
                      new JsonObject().put(Constants.STATUS, Constants.FAILED).toString()));
            }
          });
    } else if (itemType.equalsIgnoreCase(Constants.ITEM_TYPE_RESOURCE_SERVER)) {
      handler.handle(
          Future.succeededFuture(new JsonObject().put(Constants.STATUS, Constants.SUCCESS)));
    }
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public ValidatorService validateProvider(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /** Generates timestamp with timezone +05:30. */
  public static String getUtcDatetimeAsString() {
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ");
    df.setTimeZone(TimeZone.getTimeZone("IST"));
    final String utcTime = df.format(new Date());
    return utcTime;
  }
}
