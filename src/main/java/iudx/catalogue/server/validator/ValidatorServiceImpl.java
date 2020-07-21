package iudx.catalogue.server.validator;

import java.io.IOException;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

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

  /** {@inheritDoc} */
  @Override
  public ValidatorService validateItem(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    try {
      resourceValidator = new Validator("/resourceItemSchema.json");
      resourceGroupValidator = new Validator("/resourceGroupItemSchema.json");
      resourceServerValidator = new Validator("/resourceServerItemSchema.json");
      providerValidator = new Validator("/providerItemSchema.json");
    } catch (IOException | ProcessingException e) {
      e.printStackTrace();
    }
    String itemType = request.getJsonArray("type").getString(0);
    System.out.println("itemType: " + itemType);
    if (itemType.equalsIgnoreCase("iudx:resource")) {
      isValidSchema = resourceValidator.validate(request.toString());
    } else if (itemType.equalsIgnoreCase("iudx:Provider")) {
      isValidSchema = providerValidator.validate(request.toString());
    } else if (itemType.equalsIgnoreCase("iudx:resourceGroup")) {
      isValidSchema = resourceGroupValidator.validate(request.toString());
    } else if (itemType.equalsIgnoreCase("iudx:resourceServer")) {
      isValidSchema = resourceServerValidator.validate(request.toString());
    } else {
      isValidSchema = false;
    }
    if (isValidSchema) {
      handler.handle(Future.succeededFuture(new JsonObject().put("status", "success")));
    } else {
      logger.info("invalid Json");
      handler.handle(Future.failedFuture("failed"));
    }
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public ValidatorService validateProvider(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }
}
