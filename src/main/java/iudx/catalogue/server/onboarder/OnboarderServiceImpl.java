package iudx.catalogue.server.onboarder;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * The Onboarder Service Implementation.
 * <h1>Onboarder Service Implementation</h1>
 * <p>
 * The Onboarder Service implementation in the IUDX Resource Catalogue implements the
 * definitions of the {@link iudx.catalogue.server.onboarder.OnboarderService}.
 * </p>
 * 
 * @version 1.0
 * @since 2020-05-31
 */

public class OnboarderServiceImpl implements OnboarderService {

  private static final Logger logger = LoggerFactory.getLogger(OnboarderServiceImpl.class);
  
  /**
   * {@inheritDoc}
   */
  
  @Override
  public OnboarderService registerAdaptor(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  
  @Override
  public OnboarderService updateAdaptor(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  
  @Override
  public OnboarderService deleteAdaptor(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  
  @Override
  public OnboarderService listAdaptor(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

}
