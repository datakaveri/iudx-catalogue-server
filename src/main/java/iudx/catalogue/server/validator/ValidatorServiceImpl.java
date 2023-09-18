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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
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
  private Validator mlayerDatasetValidator;
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
      mlayerDatasetValidator = new Validator("/mlayerDatasetSchema.json");

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

  String getReturnTypeForValidation(JsonObject result) {
    LOGGER.debug(result);
    return result.getJsonArray(RESULTS).stream()
        .map(JsonObject.class::cast)
        .map(r -> r.getString(TYPE))
        .collect(Collectors.toList())
        .toString();
  }

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
  @SuppressWarnings("unchecked")
  @Override
  public ValidatorService validateItem(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    String itemType = getItemType(request, handler);
    LOGGER.debug("Info: itemType: " + itemType);

    // Validate if Resource
    if (itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE)) {
      validateResource(request, handler);
    } else if (itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_SERVER)) {
      // Validate if Resource Server TODO: More checks and auth rules
      validateResourceServer(request, handler);
    } else if (itemType.equalsIgnoreCase(ITEM_TYPE_PROVIDER)) {
      validateProvider(request, handler);
    } else if (itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_GROUP)) {
      validateResourceGroup(request, handler);
    } else if (itemType.equalsIgnoreCase(ITEM_TYPE_COS)) {
      validateCosItem(request, handler);
    } else if (itemType.equalsIgnoreCase(ITEM_TYPE_OWNER)) {
      validateOwnerItem(request, handler);
    }
    return this;
  }

  private void validateResourceGroup(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    validateId(request, handler, isUacInstance);
    if (!isUacInstance && !request.containsKey(ID)) {
      UUID uuid = UUID.randomUUID();
      request.put(ID, uuid.toString());
    }

    request.put(ITEM_STATUS, ACTIVE).put(ITEM_CREATED_AT, getUtcDatetimeAsString());
    String provider = request.getString(PROVIDER);
    String checkQuery =
        ITEM_EXISTS_QUERY
            .replace("$1", provider)
            .replace("$2", ITEM_TYPE_RESOURCE_GROUP)
            .replace("$3", request.getString(NAME));
    client.searchAsync(
        checkQuery,
        docIndex,
        res -> {
          if (res.failed()) {
            LOGGER.debug("Fail: DB Error");
            handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
            return;
          }
          String returnType = getReturnTypeForValidation(res.result());
          LOGGER.debug(returnType);
          if (res.result().getInteger(TOTAL_HITS) < 1 || !returnType.contains(ITEM_TYPE_PROVIDER)) {
            LOGGER.debug("Provider does not exist");
            handler.handle(Future.failedFuture("Fail: Provider item doesn't exist"));
          } else if (res.result().getInteger(TOTAL_HITS) > 1) {
            LOGGER.debug("RG already exists");
            handler.handle(Future.failedFuture("Fail: Resource Group item already exists"));
          } else {
            handler.handle(Future.succeededFuture(request));
          }
        });
  }

  private void validateProvider(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    // Validate if Provider
    validateId(request, handler, isUacInstance);
    if (!isUacInstance && !request.containsKey(ID)) {
      UUID uuid = UUID.randomUUID();
      request.put(ID, uuid.toString());
    }

    request.put(ITEM_STATUS, ACTIVE).put(ITEM_CREATED_AT, getUtcDatetimeAsString());
    String resourceServer = request.getString(RESOURCE_SVR);
    String checkQuery =
        ITEM_EXISTS_QUERY
            .replace("$1", resourceServer)
            .replace("$2", ITEM_TYPE_PROVIDER)
            .replace("$3", request.getString(NAME));
    client.searchAsync(
        checkQuery,
        docIndex,
        res -> {
          if (res.failed()) {
            LOGGER.debug("Fail: DB Error");
            handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
            return;
          }
          String returnType = getReturnTypeForValidation(res.result());
          LOGGER.debug(returnType);

          if (res.result().getInteger(TOTAL_HITS) < 1
              || !returnType.contains(ITEM_TYPE_RESOURCE_SERVER)) {
            LOGGER.debug("RS does not exist");
            handler.handle(Future.failedFuture("Fail: Resource Server item doesn't exist"));
          } else if (res.result().getInteger(TOTAL_HITS) > 1) {
            LOGGER.debug("Provider already exists");
            handler.handle(Future.failedFuture("Fail: Provider item already exists"));
          } else {
            handler.handle(Future.succeededFuture(request));
          }
        });
  }

  private void validateResourceServer(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    validateId(request, handler, isUacInstance);
    if (!isUacInstance && !request.containsKey(ID)) {
      UUID uuid = UUID.randomUUID();
      request.put(ID, uuid.toString());
    }

    request.put(ITEM_STATUS, ACTIVE).put(ITEM_CREATED_AT, getUtcDatetimeAsString());
    String cos = request.getString(COS_ITEM);
    String checkQuery =
        ITEM_EXISTS_QUERY
            .replace("$1", cos)
            .replace("$2", ITEM_TYPE_RESOURCE_SERVER)
            .replace("$3", request.getString(NAME));
    LOGGER.debug(checkQuery);
    client.searchAsync(
        checkQuery,
        docIndex,
        res -> {
          if (res.failed()) {
            LOGGER.debug("Fail: DB Error");
            handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
            return;
          }
          String returnType = getReturnTypeForValidation(res.result());
          LOGGER.debug(returnType);

          if (res.result().getInteger(TOTAL_HITS) < 1 || !returnType.contains(ITEM_TYPE_COS)) {
            LOGGER.debug("Cos does not exist");
            handler.handle(Future.failedFuture("Fail: Cos item doesn't exist"));
          } else if (res.result().getInteger(TOTAL_HITS) > 1) {
            LOGGER.debug("RS already exists");
            handler.handle(Future.failedFuture("Fail: Resource Server item already exists"));
          } else {
            handler.handle(Future.succeededFuture(request));
          }
        });
  }

  private void validateResource(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    validateId(request, handler, isUacInstance);
    if (!isUacInstance && !request.containsKey("id")) {
      UUID uuid = UUID.randomUUID();
      request.put("id", uuid.toString());
    }

    request.put(ITEM_STATUS, ACTIVE).put(ITEM_CREATED_AT, getUtcDatetimeAsString());
    String provider = request.getString(PROVIDER);
    String resourceGroup = request.getString(RESOURCE_GRP);
    String resourceServer = request.getString(RESOURCE_SVR);

    String checkQuery =
        RESOURCE_ITEM_EXISTS_QUERY
            .replace("$1", resourceServer)
            .replace("$2", provider)
            .replace("$3", resourceGroup)
            .replace("$4", request.getString(NAME));
    LOGGER.debug(checkQuery);

    client.searchAsync(
        checkQuery,
        docIndex,
        res -> {
          if (res.failed()) {
            LOGGER.debug("Fail: DB Error");
            handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
            return;
          }
          String returnType = getReturnTypeForValidation(res.result());
          LOGGER.debug(returnType);

          if (res.result().getInteger(TOTAL_HITS) < 3
              && !returnType.contains(ITEM_TYPE_RESOURCE_SERVER)) {
            LOGGER.debug("RS does not exist");
            handler.handle(Future.failedFuture("Fail: Resource Server item doesn't exist"));
          } else if (res.result().getInteger(TOTAL_HITS) < 3
              && !returnType.contains(ITEM_TYPE_PROVIDER)) {
            LOGGER.debug("Provider does not exist");
            handler.handle(Future.failedFuture("Fail: Provider item doesn't exist"));
          } else if (res.result().getInteger(TOTAL_HITS) < 3
              && !returnType.contains(ITEM_TYPE_RESOURCE_GROUP)) {
            LOGGER.debug("RG does not exist");
            handler.handle(Future.failedFuture("Fail: Resource Group item doesn't exist"));
          } else if (res.result().getInteger(TOTAL_HITS) > 3) {
            handler.handle(Future.failedFuture("Fail: Resource item already exists"));
          } else {
            handler.handle(Future.succeededFuture(request));
          }
        });
  }

  private void validateCosItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    validateId(request, handler, isUacInstance);
    if (!isUacInstance && !request.containsKey(ID)) {
      UUID uuid = UUID.randomUUID();
      request.put(ID, uuid.toString());
    }
    request.put(ITEM_STATUS, ACTIVE).put(ITEM_CREATED_AT, getUtcDatetimeAsString());

    String owner = request.getString(OWNER);
    String checkQuery =
        ITEM_EXISTS_QUERY
            .replace("$1", owner)
            .replace("$2", ITEM_TYPE_COS)
            .replace("$3", request.getString(NAME));
    LOGGER.debug(checkQuery);
    client.searchAsync(
        checkQuery,
        docIndex,
        res -> {
          if (res.failed()) {
            LOGGER.debug("Fail: DB Error");
            handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
            return;
          }
          String returnType = getReturnTypeForValidation(res.result());
          LOGGER.debug(returnType);
          if (res.result().getInteger(TOTAL_HITS) < 1 || !returnType.contains(ITEM_TYPE_OWNER)) {
            LOGGER.debug("Owner does not exist");
            handler.handle(Future.failedFuture("Fail: Owner item doesn't exist"));
          } else if (res.result().getInteger(TOTAL_HITS) > 1) {
            LOGGER.debug("Cos already exists");
            handler.handle(Future.failedFuture("Fail: Cos item already exists"));
          } else {
            handler.handle(Future.succeededFuture(request));
          }
        });
  }

  private void validateOwnerItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    validateId(request, handler, isUacInstance);
    if (!isUacInstance && !request.containsKey(ID)) {
      UUID uuid = UUID.randomUUID();
      request.put(ID, uuid.toString());
    }
    request.put(ITEM_STATUS, ACTIVE).put(ITEM_CREATED_AT, getUtcDatetimeAsString());
    String checkQuery = OWNER_ITEM_EXISTS_QUERY.replace("$1", request.getString(NAME));
    LOGGER.debug(checkQuery);
    client.searchGetId(
        checkQuery,
        docIndex,
        res -> {
          if (res.failed()) {
            LOGGER.debug("Fail: DB Error");
            handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
            return;
          }
          if (res.result().getInteger(TOTAL_HITS) != 0) {
            handler.handle(Future.failedFuture("Fail: Owner item already exists"));
          } else {
            handler.handle(Future.succeededFuture(request));
          }
        });
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
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    isValidSchema = mlayerDatasetValidator.validate(request.toString());

    if (isValidSchema) {
      handler.handle(Future.succeededFuture(new JsonObject().put(STATUS, SUCCESS)));
    } else {
      LOGGER.error("Fail: Invalid Schema");
      handler.handle(Future.failedFuture(INVALID_SCHEMA_MSG));
    }
    return this;
  }
}
