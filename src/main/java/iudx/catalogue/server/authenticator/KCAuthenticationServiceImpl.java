package iudx.catalogue.server.authenticator;

import static iudx.catalogue.server.authenticator.Constants.*;
import static iudx.catalogue.server.util.Constants.ID;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.JWTProcessor;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.authenticator.model.JwtData;
import iudx.catalogue.server.util.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.ParseException;

/**
 * The KC(Keycloak) Authentication Service Implementation.
 *
 * <h1>KC Authentication Service Implementation</h1>
 *
 * <p>The KC Authentication Service Implementation in the DX Catalogue Server implements the
 * definitions of the {@link iudx.catalogue.server.authenticator.AuthenticationService}.
 *
 * @version 1.0
 * @since 2023-06-14 }
 */
public class KCAuthenticationServiceImpl implements AuthenticationService {
  private static final Logger LOGGER = LogManager.getLogger(KCAuthenticationServiceImpl.class);

  final JWTProcessor<SecurityContext> jwtProcessor;
  private Api api;

  public KCAuthenticationServiceImpl(
      final JWTProcessor<SecurityContext> jwtProcessor, final Api api) {
    this.jwtProcessor = jwtProcessor;
    this.api = api;
  }

  Future<JwtData> decodeKCToken(String token) {
    Promise<JwtData> promise = Promise.promise();
    try {
      JWTClaimsSet claimsSet = jwtProcessor.process(token, null);
      String stringjson = claimsSet.toString();
      JwtData jwtData = new JwtData(new JsonObject(stringjson));
      LOGGER.debug(jwtData);
      promise.complete(jwtData);
    } catch (Exception e) {
      LOGGER.error(e.getMessage());
      promise.fail(e.getMessage());
    }

    return promise.future();
  }

  @Override
  public AuthenticationService tokenInterospect(
      JsonObject request, JsonObject authenticationInfo, Handler<AsyncResult<JsonObject>> handler) {
    String endpoint = authenticationInfo.getString(API_ENDPOINT);
//    String id = authenticationInfo.getString(ID);
    String token = authenticationInfo.getString(TOKEN);
    Future<JwtData> decodeTokenFuture = decodeKCToken(token);

    ResultContainer result = new ResultContainer();

    decodeTokenFuture
        .compose(
            decodeHandler -> {
              result.jwtData = decodeHandler;
              return isValidEndpoint(endpoint);
            })
        .onComplete(
            completeHandler -> {
              if (completeHandler.succeeded()) {
                handler.handle(Future.succeededFuture());
              } else {
                handler.handle(Future.failedFuture(completeHandler.cause().getMessage()));
              }
            });
    return this;
  }

  Future<Boolean> isValidEndpoint(String endpoint) {
    Promise<Boolean> promise = Promise.promise();

    LOGGER.debug(endpoint);
    if (endpoint.equals(api.getRouteItems()) || endpoint.equals(api.getRouteInstance())) {
      promise.complete(true);
    } else {
      LOGGER.error("Unauthorized access to endpoint {}", endpoint);
      promise.fail("Unauthorized access to endpoint " + endpoint);
    }
    return promise.future();
  }

  final class ResultContainer {
    JwtData jwtData;
  }
}
