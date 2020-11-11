package iudx.catalogue.server.validator;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import iudx.catalogue.server.database.ElasticClient;
import static iudx.catalogue.server.validator.Constants.*;
import static iudx.catalogue.server.util.Constants.*;

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
  private final ElasticClient client;

  public ValidatorServiceImpl(ElasticClient client) {

    this.client = client;

    try {
      resourceValidator = new Validator("/resourceItemSchema.json");
      resourceGroupValidator = new Validator("/resourceGroupItemSchema.json");
      resourceServerValidator = new Validator("/resourceServerItemSchema.json");
      providerValidator = new Validator("/providerItemSchema.json");
    } catch (IOException | ProcessingException e) {
      e.printStackTrace();
    }

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
    type.retainAll(ITEM_TYPES);
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
      LOGGER.debug("Fail: Invalid Schema");
      handler.handle(
          Future.failedFuture(INVALID_SCHEMA_MSG));
    }
    return this;
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
      handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
    }
    type.retainAll(ITEM_TYPES);
    String itemType = type.toString().replaceAll("\\[", "").replaceAll("\\]", "");
    LOGGER.debug("Info: itemType: " + itemType);


    String checkQuery = "{\"_source\": [\"id\"],"
                        +"\"query\": {\"term\": {\"id.keyword\": \"$1\"}}}";


    /** Validate if Resource */
    if (itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE)) {
      String resourceGroup = request.getString(RESOURCE_GRP);
      String id = resourceGroup + "/" + request.getString(NAME);
      LOGGER.debug("Info: id generated: " + id);
      request.put(ID, id).put(ITEM_STATUS,
          ACTIVE)
          .put(ITEM_CREATED_AT, getUtcDatetimeAsString());

      LOGGER.debug("Info: Verifying resourceGroup " + resourceGroup);
      client.searchGetId(CAT_INDEX_NAME, checkQuery.replace("$1", resourceGroup), checkRes -> {
        if (checkRes.failed()) {
          LOGGER.error("Fail: DB request has failed;" + checkRes.cause().getMessage());
          handler.handle(Future.failedFuture(INTERNAL_SERVER_ERROR));
          return;
        }

        if (checkRes.result().getInteger(TOTAL_HITS) == 1) {
          handler.handle(Future.succeededFuture(request));
        } else {
          LOGGER.error("Fail: ResourceGroup doesn't exist");
          handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
          return;
        }
      });
    }
    /** 
     * Validate if Resource Server
     * TODO: More checks and auth rules
     **/
    else if (itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_SERVER)) {
      handler.handle(Future.succeededFuture(request));
    }
    /** Validate if Provider */
    else if (itemType.equalsIgnoreCase(ITEM_TYPE_PROVIDER)) {
      handler.handle(Future.succeededFuture(request));
    }
    /** Validate if ResourceGroup */
    else if (itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_GROUP)) {
      String resourceServer = request.getString(RESOURCE_SVR);
      String[] domain = resourceServer.split("/");
      String provider = request.getString(PROVIDER);
      String name = request.getString(NAME);
      String id = provider + "/" + domain[2] + "/" + name;
      LOGGER.debug("Info: id generated: " + id);
      request.put(ID, id).put(ITEM_STATUS, ACTIVE)
          .put(ITEM_CREATED_AT, getUtcDatetimeAsString());

      client.searchGetId(CAT_INDEX_NAME,
          checkQuery.replace("$1", provider), providerRes -> {
        if (providerRes.failed()) {
          LOGGER.debug("Fail: DB Error");
          handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
          return;
        }
        if (providerRes.result().getInteger(TOTAL_HITS) == 1) {
          client.searchGetId(CAT_INDEX_NAME,
              checkQuery.replace("$1", resourceServer), serverRes -> {
              if (serverRes.failed()) {
                LOGGER.debug("Fail: DB error");
                handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
                return;
              } 
              if (serverRes.result().getInteger(TOTAL_HITS) == 1) {
                handler.handle(Future.succeededFuture(request));
              } else {
                LOGGER.debug("Fail: Server doesn't exist");
                handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
              }
          });
        } else {
          LOGGER.debug("Fail: Provider doesn't exist");
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
