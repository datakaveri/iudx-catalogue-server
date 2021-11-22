package iudx.catalogue.server.apiserver.util;

import static iudx.catalogue.server.apiserver.util.Constants.HEADER_CONTENT_TYPE;
import static iudx.catalogue.server.apiserver.util.Constants.MIME_APPLICATION_JSON;
import static iudx.catalogue.server.util.Constants.FAILED;
import static iudx.catalogue.server.util.Constants.INSERT;
import static iudx.catalogue.server.util.Constants.REQUEST_POST;
import static iudx.catalogue.server.util.Constants.UPDATE;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.util.Constants.*;

public class ExceptionHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LogManager.getLogger(ExceptionHandler.class);

  @Override
  public void handle(RoutingContext routingContext) {

    Throwable failure = routingContext.failure();
    if (failure instanceof DecodeException) {
      handleDecodeException(routingContext);
      return;
    } else if (failure instanceof ClassCastException) {
      handleClassCastException(routingContext);
      return;
    } else {
      routingContext.response()
                    .setStatusCode(400)
                    .putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                    .end(new RespBuilder()
                              .withType(TYPE_INVALID_SYNTAX)
                              .withTitle(TITLE_INVALID_SYNTAX)
                              .getResponse());
    }
  }


  /**
   * Handles the JsonDecode Exception.
   * 
   * @param routingContext
   */
  private void handleDecodeException(RoutingContext routingContext) {

    LOGGER.error("Error: Invalid Json payload; " + routingContext.failure().getLocalizedMessage());
    String response = "";

    if (routingContext.request().uri().startsWith(ROUTE_ITEMS)) {
       response = new RespBuilder()
           .withType(TYPE_FAIL)
           .withResult()
           .getResponse();
    } else if (routingContext.request().uri().startsWith(ROUTE_SEARCH)) {
      response = new JsonObject().put(STATUS, FAILED)
                                 .put(DESC, "Invalid Json Format")
                                 .encode();
    } else {
      response = new RespBuilder()
                            .withType(TYPE_INVALID_SYNTAX)
                            .withTitle(TITLE_INVALID_SYNTAX)
                            .getResponse();
    }

  String INTERNAL_ERROR_RESP = new RespBuilder()
                                          .withType(TYPE_INTERNAL_SERVER_ERROR)
                                          .withTitle(TITLE_INTERNAL_SERVER_ERROR)
                                          .getResponse();
    
    routingContext.response()
                  .setStatusCode(500)
                  .putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                  .end(INTERNAL_ERROR_RESP);
    
    routingContext.next();

  }

  /**
   * Handles the exception from casting a object to different object.
   * 
   * @param routingContext
   */
  private void handleClassCastException(RoutingContext routingContext) {

    LOGGER.error("Error: Invalid request payload; " + 
        routingContext.failure().getLocalizedMessage());
    
    routingContext.response()
                  .setStatusCode(400)
                  .putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                  .end(
                      new JsonObject()
                      .put(TYPE, TYPE_FAIL)
                      .put(TITLE, "Invalid payload")
                      .encode());

    routingContext.next();
  }
}
