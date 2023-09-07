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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

  private static final Logger LOGGER = LogManager.getLogger(ValidatorServiceImpl.class);
  /** ES client. */
  static ElasticClient client;

  private boolean isValidSchema;
  private Validator resourceValidator;
  private Validator resourceGroupValidator;
  private Validator providerValidator;
  private Validator resourceServerValidator;
  private Validator cosItemValidator;
  private Validator ownerItemSchema;
  private Validator ratingValidator;
  private Validator mlayerInstanceValidator;
  private Validator mlayerDomainValidator;
  private Validator mlayerGeoQueryValidator;
  private String docIndex;
  private boolean isUacInstance;

  /**
   * Constructs a new ValidatorServiceImpl object with the specified ElasticClient and docIndex.
   *
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
      cosItemValidator = new Validator("/cosItemSchema.json");
      ownerItemSchema = new Validator("/ownerItemSchema.json");
      ratingValidator = new Validator("/ratingSchema.json");
      mlayerInstanceValidator = new Validator("/mlayerInstanceSchema.json");
      mlayerDomainValidator = new Validator("/mlayerDomainSchema.json");
      mlayerGeoQueryValidator = new Validator("/mlayerGeoQuerySchema.json");

    } catch (IOException | ProcessingException e) {
      e.printStackTrace();
    }
  }

  /** Generates timestamp with timezone +05:30. */
  public static String getUtcDatetimeAsString() {
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ");
    df.setTimeZone(TimeZone.getTimeZone("IST"));
    final String utcTime = df.format(new Date());
    return utcTime;
  }

  private static String getItemType(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    Set<String> type = new HashSet<String>(new JsonArray().getList());
    try {
      type = new HashSet<String>(request.getJsonArray(TYPE).getList());
    } catch (Exception e) {
      LOGGER.error("Item type mismatch");
      handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
    }
    type.retainAll(ITEM_TYPES);
    String itemType = type.toString().replaceAll("\\[", "").replaceAll("\\]", "");
    return itemType;
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  public ValidatorService validateSchema(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    LOGGER.debug("Info: Reached Validator service validate schema");

    String itemType = getItemType(request, handler);
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
      case ITEM_TYPE_COS:
        isValidSchema = cosItemValidator.validate(request.toString());
        break;
      case ITEM_TYPE_OWNER:
        isValidSchema = ownerItemSchema.validate(request.toString());
        break;
      default:
        isValidSchema = false;
        break;
    }

    if (isValidSchema) {
      handler.handle(Future.succeededFuture(new JsonObject().put(STATUS, SUCCESS)));
    } else {
      LOGGER.debug("Fail: Invalid Schema");
      handler.handle(Future.failedFuture(INVALID_SCHEMA_MSG));
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public ValidatorService validateItem(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    String itemType = getItemType(request, handler);
    LOGGER.debug("Info: itemType: " + itemType);

    String checkQuery =
        "{\"_source\": [\"id\"]," + "\"query\": {\"term\": {\"id.keyword\": \"$1\"}}}";

    // Validate if Resource
    if (itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE)) {
      validateResource(request, handler);
    } else if (itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_SERVER)) {
      // Validate if Resource Server TODO: More checks and auth rules
      validateResourceServer(request, handler, checkQuery);
    } else if (itemType.equalsIgnoreCase(ITEM_TYPE_PROVIDER)) {
      validateProvider(request, handler, checkQuery);
    } else if (itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_GROUP)) {
      validateResourceGroup(request, handler, checkQuery);
    } else if (itemType.equalsIgnoreCase(ITEM_TYPE_COS)) {
      validateCosItem(request, handler, checkQuery);
    } else if (itemType.equalsIgnoreCase(ITEM_TYPE_OWNER)) {
      validateOwnerItem(request, handler);
    }
    return this;
  }


  private void validateResourceGroup(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler, String checkQuery) {
    validateId(request, handler, isUacInstance);
    if (!isUacInstance && !request.containsKey(ID)) {
      String providerResourceServerUuid = request.getString(NAME) + request.getString(PROVIDER);
      byte[] inputBytes = providerResourceServerUuid.getBytes(StandardCharsets.UTF_8);
      UUID uuid = UUID.nameUUIDFromBytes(inputBytes);
      request.put(ID, uuid.toString());
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
  }

  private void validateProvider(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler, String checkQuery) {
    LOGGER.debug(checkQuery);
    // Validate if Provider
    validateId(request, handler, isUacInstance);
    if (!isUacInstance && !request.containsKey(ID)) {
      String uuidInputString = request.getString(NAME) + request.getString(RESOURCE_SVR);
      byte[] inputBytes = uuidInputString.getBytes(StandardCharsets.UTF_8);
      UUID uuid = UUID.nameUUIDFromBytes(inputBytes);
      request.put(ID, uuid.toString());
    }

    request.put(ITEM_STATUS, ACTIVE).put(ITEM_CREATED_AT, getUtcDatetimeAsString());
    String resourceServer = request.getString(RESOURCE_SVR);
    client.searchGetId(
        checkQuery.replace("$1", resourceServer),
        docIndex,
        res -> {
          if (res.failed()) {
            LOGGER.debug("Fail: DB Error");
            handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
            return;
          }
          if (res.result().getInteger(TOTAL_HITS) == 1) {
            handler.handle(Future.succeededFuture(request));
          } else {
            LOGGER.debug("Fail: Resource Server doesn't exist");
            handler.handle(Future.failedFuture("Fail: Resource Server does not exist"));
          }
        });
  }

  private void validateResourceServer(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler, String checkQuery) {
    validateId(request, handler, isUacInstance);
    if (!isUacInstance && !request.containsKey(ID)) {
      String ameUuid = request.getString(NAME) + request.getString(COS_ITEM);
      byte[] inputBytes = ameUuid.getBytes(StandardCharsets.UTF_8);
      UUID uuid = UUID.nameUUIDFromBytes(inputBytes);
      request.put(ID, uuid.toString());
    }

    request.put(ITEM_STATUS, ACTIVE).put(ITEM_CREATED_AT, getUtcDatetimeAsString());
    String cos = request.getString(COS_ITEM);
    client.searchGetId(
        checkQuery.replace("$1", cos),
        docIndex,
        res -> {
          if (res.failed()) {
            LOGGER.debug("Fail: DB Error");
            handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
            return;
          }
          if (res.result().getInteger(TOTAL_HITS) == 1) {
            handler.handle(Future.succeededFuture(request));
          } else {
            LOGGER.debug("Fail: Cos doesn't exist");
            handler.handle(Future.failedFuture("Fail: Cos does not exist"));
          }
        });
  }

  private void validateResource(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    validateId(request, handler, isUacInstance);
    if (!isUacInstance && !request.containsKey("id")) {
      String resourceGroupNameUuid = request.getString("resourceGroup") + request.getString("name");
      byte[] inputBytes = resourceGroupNameUuid.getBytes(StandardCharsets.UTF_8);
      UUID uuid = UUID.nameUUIDFromBytes(inputBytes);
      request.put("id", uuid.toString());
    }

    request.put(ITEM_STATUS, ACTIVE).put(ITEM_CREATED_AT, getUtcDatetimeAsString());
    String provider = request.getString(PROVIDER);
    String resourceGroup = request.getString(RESOURCE_GRP);
    String resourceServer = request.getString(RESOURCE_SVR);

    client.searchGetId(
        RESOURCE_CHECK_QUERY
            .replace("$1", resourceServer)
            .replace("$2", provider)
            .replace("$3", resourceGroup),
        docIndex,
        providerRes -> {
          if (providerRes.failed()) {
            LOGGER.debug("Fail: DB Error");
            handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
            return;
          }
          if (providerRes.result().getInteger(TOTAL_HITS) == 3) {
            handler.handle(Future.succeededFuture(request));
          } else {
            LOGGER.debug("Fail: RS or Provider or Resource Group does not exist");
            handler.handle(
                Future.failedFuture(
                    "Fail: Resource Server or Provider or Resource Group does not exist"));
          }
        });
  }

  private void validateCosItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler, String checkQuery) {
    validateId(request, handler, isUacInstance);
    if (!isUacInstance && !request.containsKey(ID)) {
      String cosId = request.getString(NAME);
      byte[] inputBytes = cosId.getBytes(StandardCharsets.UTF_8);
      UUID uuid = UUID.nameUUIDFromBytes(inputBytes);
      request.put(ID, uuid.toString());
    }
    request.put(ITEM_STATUS, ACTIVE).put(ITEM_CREATED_AT, getUtcDatetimeAsString());

    String owner = request.getString(OWNER);
    client.searchGetId(
        checkQuery.replace("$1", owner),
        docIndex,
        res -> {
          if (res.failed()) {
            LOGGER.debug("Fail: DB Error");
            handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
            return;
          }
          if (res.result().getInteger(TOTAL_HITS) == 1) {
            handler.handle(Future.succeededFuture(request));
          } else {
            LOGGER.debug("Fail: Cos doesn't exist");
            handler.handle(Future.failedFuture("Fail: Cos does not exist"));
          }
        });
  }

  private void validateOwnerItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    validateId(request, handler, isUacInstance);
    if(!isUacInstance && !request.containsKey(ID)) {
      String ownerId = request.getString(NAME);
      byte[] inputBytes = ownerId.getBytes(StandardCharsets.UTF_8);
      UUID uuid = UUID.nameUUIDFromBytes(inputBytes);
      request.put(ID, uuid.toString());
    }
    request.put(ITEM_STATUS, ACTIVE).put(ITEM_CREATED_AT, getUtcDatetimeAsString());

    handler.handle(Future.succeededFuture(request));
  }

  private boolean isValidUuid(String uuidString) {
    return UUID_PATTERN.matcher(uuidString).matches();
  }

  private void validateId(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler, boolean isUacInstance) {
    if (request.containsKey("id")) {
      String id = request.getString("id");
      LOGGER.debug("id in the request body: " + id);

      if (!isValidUuid(id)) {
        handler.handle(Future.failedFuture("validation failed. Incorrect id"));
      }
    } else if (isUacInstance && !request.containsKey("id")) {
      handler.handle(Future.failedFuture("mandatory id field not present in request body"));
    }
  }

  @Override
  public ValidatorService validateRating(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

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
  public ValidatorService validateMlayerInstance(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
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
  public ValidatorService validateMlayerDomain(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
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
  public ValidatorService validateMlayerGeoQuery(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    isValidSchema = mlayerGeoQueryValidator.validate(request.toString());

    if (isValidSchema) {
      handler.handle(Future.succeededFuture(new JsonObject().put(STATUS, SUCCESS)));
    } else {
      LOGGER.error("Fail: Invalid Schema");
      handler.handle(Future.failedFuture(INVALID_SCHEMA_MSG));
    }
    return this;
  }

  @Override
  public ValidatorService validateMlayerDatasetId(
      String datasetId, Handler<AsyncResult<JsonObject>> handler) {

    if (isValidUuid(datasetId)) {
      handler.handle(Future.succeededFuture(new JsonObject().put(STATUS, SUCCESS)));
    } else {
      LOGGER.error("Fail: Invalid Schema");
      handler.handle(Future.failedFuture(INVALID_SCHEMA_MSG));
    }
    return this;
  }
}
