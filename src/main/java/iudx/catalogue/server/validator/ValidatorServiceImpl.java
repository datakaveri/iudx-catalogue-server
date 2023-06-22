package iudx.catalogue.server.validator;

import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.validator.Constants.*;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.ElasticClient;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Validator Service Implementation.
 *
 * <h1>Validator Service Implementation</h1>
 *
 * <p>The Validator Service implementation in the
 * IUDX Catalogue Server implements the definitions of
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
  private Validator ratingValidator;
  private Validator mlayerInstanceValidator;
  private Validator mlayerDomainValidator;
  private Validator mlayerGeoQueryValidator;

  private static final Pattern UUID_PATTERN =
      Pattern.compile(
          ("^[a-zA-Z0-9]{8}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{12}$"));

  /** ES client. */
  static ElasticClient client;

  private String docIndex;
  private boolean isUacInstance;

  /**
   * Constructs a new ValidatorServiceImpl object with the specified ElasticClient and docIndex.
   * @param client the ElasticClient object to use for interacting with the Elasticsearch instance
   * @param docIndex the index name to use for storing documents in Elasticsearch
   */
  public ValidatorServiceImpl(ElasticClient client, String docIndex, boolean isUacInstance) {

    this.client = client;
    this.docIndex = docIndex;
    this.isUacInstance = isUacInstance;
    try {
      resourceValidator = new Validator("/resourceItemSchema.json");
      resourceGroupValidator = new Validator("/resourceGroupItemSchema.json");
      resourceServerValidator = new Validator("/resourceServerItemSchema.json");
      providerValidator = new Validator("/providerItemSchema.json");
      ratingValidator = new Validator("/ratingSchema.json");
      mlayerInstanceValidator = new Validator("/mlayerInstanceSchema.json");
      mlayerDomainValidator = new Validator("/mlayerDomainSchema.json");
      mlayerGeoQueryValidator = new Validator("/mlayerGeoQuerySchema.json");

    } catch (IOException | ProcessingException e) {
      e.printStackTrace();
    }

  }

  /**
   *  {@inheritDoc}
   */
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

    switch (itemType) {

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


  /**
   * {@inheritDoc}
   */
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
                        + "\"query\": {\"term\": {\"id.keyword\": \"$1\"}}}";


    // Validate if Resource
    if (itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE)) {
      validateId(request, handler, isUacInstance);
      if (!isUacInstance && !request.containsKey("id")) {
        String resourceGroupNameUuid = request.getString("resourceGroup")
                + request.getString("name");
        byte[] inputBytes = resourceGroupNameUuid.getBytes(StandardCharsets.UTF_8);
        UUID uuid = UUID.nameUUIDFromBytes(inputBytes);
        request.put("id", uuid.toString());
      }

      request.put(ITEM_STATUS, ACTIVE).put(ITEM_CREATED_AT, getUtcDatetimeAsString());
      String provider = request.getString(PROVIDER);
      String resourceGroup = request.getString(RESOURCE_GRP);

      LOGGER.debug("Info: Verifying resourceGroup and provider " + resourceGroup + provider);
      client.searchGetId(
          RESOURCE_CHECK_QUERY.replace("$1", provider).replace("$2", resourceGroup),
          docIndex,
          providerRes -> {
            if (providerRes.failed()) {
              LOGGER.debug("Fail: DB Error");
              handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
              return;
            }
            if (providerRes.result().getInteger(TOTAL_HITS) == 2) {
              handler.handle(Future.succeededFuture(request));
            } else {
              LOGGER.debug("Fail: Provider or Resource Group does not exist");
              handler.handle(Future.failedFuture("Fail: Provider or Resource Group does not exist"));
            }
          });
    } else if (itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_SERVER)) {
      // Validate if Resource Server TODO: More checks and auth rules
      validateId(request, handler, isUacInstance);
      if (!isUacInstance && !request.containsKey("id")) {
        String providerNameUuid = request.getString("provider") + request.getString("name");
        byte[] inputBytes = providerNameUuid.getBytes(StandardCharsets.UTF_8);
        UUID uuid = UUID.nameUUIDFromBytes(inputBytes);
        request.put("id", uuid.toString());
      }
      request.put(ITEM_STATUS, ACTIVE).put(ITEM_CREATED_AT, getUtcDatetimeAsString());
      String provider = request.getString(PROVIDER);

      client.searchGetId(
          checkQuery.replace("$1", provider),
          docIndex,
          providerRes -> {
            if (providerRes.failed()) {
              LOGGER.debug("Fail: DB Error");
              handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
              return;
            }
            if (providerRes.result().getInteger(TOTAL_HITS) == 1) {
              handler.handle(Future.succeededFuture(request));
            } else {
              LOGGER.debug("Fail: Provider doesn't exist");
              handler.handle(Future.failedFuture("Fail: Provider does not exist"));
            }
          });
    } else if (itemType.equalsIgnoreCase(ITEM_TYPE_PROVIDER)) {
      LOGGER.debug(checkQuery);
      // Validate if Provider
      validateId(request, handler, isUacInstance);
      if (!isUacInstance && !request.containsKey("id")) {
        byte[] inputBytes = request.getString("name").getBytes(StandardCharsets.UTF_8);
        UUID uuid = UUID.nameUUIDFromBytes(inputBytes);
        request.put("id", uuid.toString());
      }

      handler.handle(Future.succeededFuture(request));
    } else if (itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_GROUP)) {
      validateId(request, handler, isUacInstance);
      if (!isUacInstance && !request.containsKey("id")) {
        String providerResourceServerUuid = request.getString("provider")
                + request.getString("resourceServer");
        byte[] inputBytes = providerResourceServerUuid.getBytes(StandardCharsets.UTF_8);
        UUID uuid = UUID.nameUUIDFromBytes(inputBytes);
        request.put("id", uuid.toString());
      }

      request.put(ITEM_STATUS, ACTIVE).put(ITEM_CREATED_AT, getUtcDatetimeAsString());
      String resourceServer = request.getString(RESOURCE_SVR);
      String provider = request.getString(PROVIDER);

      client.searchGetId(
          RESOURCE_GROUP_CHECK_QUERY.replace("$1", provider)
                  .replace("$2", resourceServer), docIndex, providerRes -> {
          if (providerRes.failed()) {
            LOGGER.debug("Fail: DB Error");
            handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
            return;
          }
          if (providerRes.result().getInteger(TOTAL_HITS) == 2) {
            handler.handle(Future.succeededFuture(request));
            } else {
              LOGGER.debug("Fail: Provider doesn't exist");
              handler.handle(Future.failedFuture("Fail: Provider or Resource Server does"
                      + " not exist"));
            }
          });
    }
    return this;
  }

  private boolean isValidUuid(String uuidString) {
    return UUID_PATTERN.matcher(uuidString).matches();
  }

  private void validateId(JsonObject request, Handler<AsyncResult<JsonObject>> handler,
                          boolean isUacInstance) {
    if (request.containsKey("id")) {
      String id = request.getString("id");
      LOGGER.debug("id in the request body: " + id);

      if (!isValidUuid(id)) {
        handler.handle(Future.failedFuture("validation failed. Incorrect id"));
        return;
      }
    } else if (isUacInstance && !request.containsKey("id")) {
      handler.handle(Future.failedFuture("id not found"));
      return;
    }
  }

  /** {@inheritDoc}
   *  @return null, as this method is not implemented in this class
   */
  @Override
  public ValidatorService validateProvider(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    return null;
  }

  @Override
  public ValidatorService validateRating(JsonObject request,
       Handler<AsyncResult<JsonObject>> handler) {

    isValidSchema = ratingValidator.validate(request.toString());

    if (isValidSchema) {
      handler.handle(Future.succeededFuture(new JsonObject().put(STATUS, SUCCESS)));
    } else {
      LOGGER.error("Fail: Invalid Schema");
      handler.handle(Future.failedFuture(INVALID_SCHEMA_MSG));
    }
    return this;
  }

  @Override
  public ValidatorService validateMlayerInstance(JsonObject request,
                                                 Handler<AsyncResult<JsonObject>> handler) {
    isValidSchema = mlayerInstanceValidator.validate(request.toString());
    if (isValidSchema) {
      handler.handle(Future.succeededFuture(new JsonObject()));
    } else {
      LOGGER.error("Fail: Invalid Schema");
      handler.handle(Future.failedFuture(INVALID_SCHEMA_MSG));
    }
    return null;
  }

  @Override
  public ValidatorService validateMlayerDomain(JsonObject request,
                                               Handler<AsyncResult<JsonObject>> handler) {
    isValidSchema = mlayerDomainValidator.validate(request.toString());

    if (isValidSchema) {
      handler.handle(Future.succeededFuture(new JsonObject().put(STATUS, SUCCESS)));
    } else {
      LOGGER.error("Fail: Invalid Schema");
      handler.handle(Future.failedFuture(INVALID_SCHEMA_MSG));
    }
    return this;
  }

  @Override
  public ValidatorService validateMlayerGeoQuery(JsonObject request,
                                                 Handler<AsyncResult<JsonObject>> handler) {
    isValidSchema = mlayerGeoQueryValidator.validate(request.toString());

    if (isValidSchema) {
      handler.handle(Future.succeededFuture(new JsonObject().put(STATUS, SUCCESS)));
    } else {
      LOGGER.error("Fail: Invalid Schema");
      handler.handle(Future.failedFuture(INVALID_SCHEMA_MSG));
    }
    return this;
  }

  /** Generates timestamp with timezone +05:30. */
  public static String getUtcDatetimeAsString() {
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ");
    df.setTimeZone(TimeZone.getTimeZone("IST"));
    final String utcTime = df.format(new Date());
    return utcTime;
  }
}
