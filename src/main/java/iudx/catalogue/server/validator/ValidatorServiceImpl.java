package iudx.catalogue.server.validator;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.Promise;
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
 * <p>
 * The Validator Service implementation in the IUDX Catalogue Server implements the definitions of
 * the {@link iudx.catalogue.server.validator.ValidatorService}.
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

  /** ES client */
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
  public ValidatorService validateSchema(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    Set<String> type = new HashSet<String>(new JsonArray().getList());

    try {
      type = new HashSet<String>(request.getJsonArray(Constants.TYPE).getList());
    } catch (Exception e) {
      logger.error("Item type mismatch");
    }
    type.retainAll(itemTypes);
    String itemType = type.toString().replaceAll("\\[", "").replaceAll("\\]", "");
    logger.info("itemType: " + itemType);

    switch(itemType) {

      case Constants.ITEM_TYPE_RESOURCE:
        isValidSchema = resourceValidator.validate(request.toString());
        break;
      case Constants.ITEM_TYPE_RESOURCE_GROUP:
        isValidSchema = resourceGroupValidator.validate(request.toString());
        break;
      case Constants.ITEM_TYPE_RESOURCE_SERVER:
        isValidSchema = resourceServerValidator.validate(request.toString());
        break;
      case Constants.ITEM_TYPE_PROVIDER:
        isValidSchema = providerValidator.validate(request.toString());
        break;
      default:
        isValidSchema = false;
        break;
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

  Future<Boolean> verifyLink(String link) {
    Promise<Boolean> promise = Promise.promise();
      Request checkResourceGroup =
          new Request(Constants.REQUEST_GET, Constants.CAT_INDEX + Constants.FILTER_PATH);
      JsonObject checkQuery = new JsonObject();
      checkQuery.put(Constants.SOURCE, "[\"" + Constants.ID + "\"]")
        .put(Constants.QUERY_KEY, new JsonObject()
        .put(Constants.TERM, new JsonObject().put(Constants.ID_KEYWORD, link)));
      logger.info("Query constructed: " + checkQuery.toString());
      checkResourceGroup.setJsonEntity(checkQuery.toString());
      client.performRequestAsync(checkResourceGroup, new ResponseListener() {
        @Override
        public void onSuccess(Response response) {
          int statusCode = response.getStatusLine().getStatusCode();
          logger.info("status code: " + statusCode);
          if (statusCode != 200 && statusCode != 204) {
            promise.fail(Constants.NON_EXISTING_LINK_MSG + link);
            return;
          }
          try {
            JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));
            if (responseJson.getJsonObject(Constants.HITS)
                .getJsonObject(Constants.TOTAL)
                .getInteger(Constants.VALUE) == 1) {
                  promise.complete(true);
                  logger.info("resource group validated");
              } else {
                promise.fail(Constants.NON_EXISTING_LINK_MSG + link);
              }
          } catch (ParseException | IOException e) {
            logger.info("DB ERROR:\n");
            e.printStackTrace();
            promise.fail(Constants.NON_EXISTING_LINK_MSG + link);
          }
        }

        @Override
        public void onFailure(Exception e) {
          logger.info("DB request has failed. ERROR:\n");
          e.printStackTrace();
          promise.fail(Constants.NON_EXISTING_LINK_MSG + link);
          return;
        }
      });
      return promise.future();
  }

  /** {@inheritDoc} */
  @SuppressWarnings("unchecked")
  @Override
  public ValidatorService validateItem(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    Set<String> type = new HashSet<String>(new JsonArray().getList());
    try {
      type = new HashSet<String>(request.getJsonArray(Constants.TYPE).getList());
    } catch (Exception e) {
      logger.error("Item type mismatch");
    }
    type.retainAll(itemTypes);
    String itemType = type.toString().replaceAll("\\[", "").replaceAll("\\]", "");
    logger.info("itemType: " + itemType);

    /** Validate if Resource */
    if (itemType.equalsIgnoreCase(Constants.ITEM_TYPE_RESOURCE)) {
      String resourceGroup = request.getString(Constants.RESOURCE_GROUP_KEY);
      String id = resourceGroup + "/" + request.getString(Constants.NAME);
      logger.info("id generated: " + id);
      request.put(Constants.ID, id).put(Constants.ITEM_STATUS,
          Constants.ACTIVE)
          .put(Constants.ITEM_CREATED_AT, getUtcDatetimeAsString());

      Future<Boolean> verifiedResource = verifyLink(resourceGroup);
      verifiedResource.onComplete( res -> {
        if (res.succeeded()) {
          handler.handle(Future.succeededFuture(request));
        } else {
          handler.handle(Future.failedFuture(Constants.VALIDATION_FAILURE_MSG));
        }
      });
    }
    /** Validate if Provider */
    else if (itemType.equalsIgnoreCase(Constants.ITEM_TYPE_PROVIDER)) {
      handler.handle(
          Future.succeededFuture(new JsonObject().put(Constants.STATUS, Constants.SUCCESS)));
    }
    /** Validate if ResourceGroup */
    else if (itemType.equalsIgnoreCase(Constants.ITEM_TYPE_RESOURCE_GROUP)) {
      String resourceServer = request.getString(Constants.RESOURCE_SERVER_KEY);
      String[] domain = resourceServer.split("/");
      String provider = request.getString(Constants.PROVIDER_KEY);
      String name = request.getString(Constants.NAME);
      String id = provider + "/" + domain[2] + "/" + name;
      logger.info("id generated: " + id);
      request.put(Constants.ID, id).put(Constants.ITEM_STATUS, Constants.ACTIVE)
          .put(Constants.ITEM_CREATED_AT, getUtcDatetimeAsString());

      Future<Boolean> verifiedProvider = verifyLink(provider);
      verifiedProvider.onComplete( res -> {
        if (res.succeeded()) {
          handler.handle(Future.succeededFuture(request));
          Future<Boolean> verifiedResourceServer = verifyLink(resourceServer);
          verifiedResourceServer.onComplete( rres -> {
            if (rres.succeeded()) {
              handler.handle(Future.succeededFuture(request));
            } else {
              handler.handle(Future.failedFuture(Constants.VALIDATION_FAILURE_MSG));
            }
          });
        } else {
          handler.handle(Future.failedFuture(Constants.VALIDATION_FAILURE_MSG));
        }
      });
    }
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public ValidatorService validateProvider(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
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
