package iudx.catalogue.server.validator;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * The Validator Service Implementation.
 * <h1>Validator Service Implementation</h1>
 * <p>
 * The Validator Service implementation in the IUDX Catalogue Server implements the definitions of
 * the {@link iudx.catalogue.server.validator.ValidatorService}.
 * </p>
 * 
 * @version 1.0
 * @since 2020-05-31
 */

public class ValidatorServiceImpl implements ValidatorService {

  private static final Logger logger = LoggerFactory.getLogger(ValidatorServiceImpl.class);

  /**
   * {@inheritDoc}
   */

  @Override
  public ValidatorService validateItem(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public ValidatorService validateProvider(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

}
