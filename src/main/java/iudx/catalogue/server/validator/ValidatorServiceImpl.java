package iudx.catalogue.server.validator;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

import static iudx.catalogue.server.validator.Constants.*;

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

  private static final Logger LOGGER = LogManager.getLogger(ValidatorServiceImpl.class);
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
    itemTypes.add(ITEM_TYPE_RESOURCE);
    itemTypes.add(ITEM_TYPE_RESOURCE_GROUP);
    itemTypes.add(ITEM_TYPE_RESOURCE_SERVER);
    itemTypes.add(ITEM_TYPE_PROVIDER);
  }

  /** {@inheritDoc} */
  @SuppressWarnings("unchecked")
  public ValidatorService validateSchema(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    LOGGER.debug("Info: Reached Validator service validate schema");
    Set<String> type = new HashSet<String>(new JsonArray().getList());

    try {
      type = new HashSet<String>(request.getJsonArray(TYPE).getList());
    } catch (Exception e) {
      LOGGER.error("Item type mismatch");
    }
    type.retainAll(itemTypes);
    String itemType = type.toString().replaceAll("\\[", "").replaceAll("\\]", "");
    LOGGER.debug("Info: itemType: " + itemType);

    switch(itemType) {

      case ITEM_TYPE_RESOURCE:
        isValidSchema = resourceValidator.validate(request.toString());
        break;
      case ITEM_TYPE_RESOURCE_GROUP:
        isValidSchema = resourceGroupValidator.validate(request.toString());
        break;
      case ITEM_TYPE_RESOURCE_SERVER:
        isValidSchema = resourceServerValidator.validate(request.toString());
        break;
      case ITEM_TYPE_PROVIDER:
        isValidSchema = providerValidator.validate(request.toString());
        break;
      default:
        isValidSchema = false;
        break;
    }

    if (isValidSchema) {
      handler.handle(
          Future.succeededFuture(new JsonObject().put(STATUS, SUCCESS)));
    } else {
      handler.handle(
          Future.failedFuture(new JsonObject().put(STATUS, FAILED).toString()));
    }
    return null;
  }

  /**
   * Verify if link present in item is a valid one
   **/
  Future<Boolean> verifyLink(String link) {
    Promise<Boolean> promise = Promise.promise();
    Request checkResourceGroup =
      new Request(REQUEST_GET, CAT_INDEX + FILTER_PATH);
    JsonObject checkQuery = new JsonObject();
    checkQuery.put(SOURCE, "[\"" + ID + "\"]")
      .put(QUERY_KEY, new JsonObject()
          .put(TERM, new JsonObject().put(ID_KEYWORD, link)));
    LOGGER.debug("Info: Query constructed: " + checkQuery.toString());
    checkResourceGroup.setJsonEntity(checkQuery.toString());
    client.performRequestAsync(checkResourceGroup, new ResponseListener() {
      @Override
      public void onSuccess(Response response) {
        int statusCode = response.getStatusLine().getStatusCode();
        LOGGER.debug("Info: status code: " + statusCode);
        if (statusCode != 200 && statusCode != 204) {
          promise.fail(NON_EXISTING_LINK_MSG + link);
          return;
        }
        try {
          JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));
          LOGGER.debug("Info: Got response ");
          LOGGER.debug("Info:" + responseJson.toString());
          if (responseJson.getJsonObject(HITS)
              .getJsonObject(TOTAL)
              .getInteger(VALUE) == 1) {
            promise.complete(true);
            LOGGER.debug("Info: resource group validated");
          } else {
                promise.fail(NON_EXISTING_LINK_MSG + link);
              }
          } catch (ParseException | IOException e) {
            LOGGER.error("DB ERROR:\n");
            e.printStackTrace();
            promise.fail(NON_EXISTING_LINK_MSG + link);
          }
        }

        @Override
        public void onFailure(Exception e) {
          LOGGER.info("DB request has failed. ERROR:\n");
          e.printStackTrace();
          promise.fail(NON_EXISTING_LINK_MSG + link);
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
      type = new HashSet<String>(request.getJsonArray(TYPE).getList());
    } catch (Exception e) {
      LOGGER.error("Item type mismatch");
    }
    type.retainAll(itemTypes);
    String itemType = type.toString().replaceAll("\\[", "").replaceAll("\\]", "");
    LOGGER.debug("Info: itemType: " + itemType);

    /** Validate if Resource */
    if (itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE)) {
      String resourceGroup = request.getString(RESOURCE_GROUP_KEY);
      String id = resourceGroup + "/" + request.getString(NAME);
      LOGGER.debug("Info: id generated: " + id);
      request.put(ID, id).put(ITEM_STATUS,
          ACTIVE)
          .put(ITEM_CREATED_AT, getUtcDatetimeAsString());

      LOGGER.debug("Info: Starting verification");
      LOGGER.debug("Info: Verifying resourceGroup " + resourceGroup);
      Future<Boolean> verifiedResource = verifyLink(resourceGroup);
      verifiedResource.onComplete( res -> {
        if (res.succeeded()) {
          handler.handle(Future.succeededFuture(request));
        } else {
          handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
        }
      });
    }
    /** Validate if Provider */
    else if (itemType.equalsIgnoreCase(ITEM_TYPE_PROVIDER)) {
      handler.handle(
          Future.succeededFuture(new JsonObject().put(STATUS, SUCCESS)));
    }
    /** Validate if ResourceGroup */
    else if (itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_GROUP)) {
      String resourceServer = request.getString(RESOURCE_SERVER_KEY);
      String[] domain = resourceServer.split("/");
      String provider = request.getString(PROVIDER_KEY);
      String name = request.getString(NAME);
      String id = provider + "/" + domain[2] + "/" + name;
      LOGGER.debug("Info: id generated: " + id);
      request.put(ID, id).put(ITEM_STATUS, ACTIVE)
          .put(ITEM_CREATED_AT, getUtcDatetimeAsString());

      Future<Boolean> verifiedProvider = verifyLink(provider);
      verifiedProvider.onComplete( res -> {
        if (res.succeeded()) {
          handler.handle(Future.succeededFuture(request));
          Future<Boolean> verifiedResourceServer = verifyLink(resourceServer);
          verifiedResourceServer.onComplete( rres -> {
            if (rres.succeeded()) {
              handler.handle(Future.succeededFuture(request));
            } else {
              handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
            }
          });
        } else {
          handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
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
