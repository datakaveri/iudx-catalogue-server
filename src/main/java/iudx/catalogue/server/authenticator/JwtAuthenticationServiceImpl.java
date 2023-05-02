package iudx.catalogue.server.authenticator;

import static iudx.catalogue.server.auditing.util.Constants.*;
import static iudx.catalogue.server.authenticator.Constants.*;
import static iudx.catalogue.server.authenticator.Constants.METHOD;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import iudx.catalogue.server.authenticator.authorization.AuthorizationContextFactory;
import iudx.catalogue.server.authenticator.authorization.AuthorizationRequest;
import iudx.catalogue.server.authenticator.authorization.AuthorizationStratergy;
import iudx.catalogue.server.authenticator.authorization.JwtAuthorization;
import iudx.catalogue.server.authenticator.authorization.Method;
import iudx.catalogue.server.authenticator.model.JwtData;
import iudx.catalogue.server.util.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * The JWT Authentication Service Implementation.
 * <h1>JWT Authentication Service Implementation</h1>
 * <p>
 * The JWT Authentication Service implementation in the
 * IUDX Catalogue Server implements the definitions
 * of the {@link iudx.catalogue.server.authenticator.AuthenticationService}.
 * </p>
 *
 * @version 1.0
 * @since 2021-09-23
 */

public class JwtAuthenticationServiceImpl implements AuthenticationService {
  private static final Logger LOGGER = LogManager.getLogger(JwtAuthenticationServiceImpl.class);

  final JWTAuth jwtAuth;
  final String audience;
  private Api api;

  JwtAuthenticationServiceImpl(Vertx vertx, final JWTAuth jwtAuth, final JsonObject config,
                               final Api api) {
    this.jwtAuth = jwtAuth;
    this.audience = config.getString("host");
    this.api = api;
  }

  Future<JwtData> decodeJwt(String jwtToken) {
    Promise<JwtData> promise = Promise.promise();

    TokenCredentials credentials =  new TokenCredentials(jwtToken);

    jwtAuth.authenticate(credentials)
            .onSuccess(user -> {
              JwtData jwtData = new JwtData(user.principal());
              promise.complete(jwtData);
            }).onFailure(err -> {
              LOGGER.error("failed to decode/validate jwt token : " + err.getMessage());
              promise.fail("failed");
            });
    return promise.future();
  }

  // class to contain intermediate data for token introspection
  final class ResultContainer {
    JwtData jwtData;
  }

  Future<Boolean> isValidAudienceValue(JwtData jwtData) {
    Promise<Boolean> promise = Promise.promise();

    LOGGER.debug("Audience in jwt is: " + jwtData.getAud());
    if (audience != null && audience.equalsIgnoreCase(jwtData.getAud())) {
      promise.complete(true);
    } else {
      LOGGER.error("Incorrect audience value in jwt");
      promise.fail("Incorrect audience value in jwt");
    }
    return promise.future();
  }

  Future<Boolean> isValidId(JwtData jwtData, String id) {
    Promise<Boolean> promise = Promise.promise();

    // id is the (substring [1] of Iid with delimiter as ':' )'s substring [1] with delimiter as '/'
    String jwtId = "";
    if (jwtData.getIid().contains("/")) {
      jwtId = (jwtData.getIid().split(":")[1]).split("/")[0] + "/" + (jwtData.getIid()
              .split(":")[1]).split("/")[1];
    } else {
      jwtId = jwtData.getIid().split(":")[1];
    }

    if (id.equalsIgnoreCase(jwtId)) {
      promise.complete(true);
    } else {
      LOGGER.error("Incorrect id value in jwt");
      promise.fail("Incorrect id value in jwt");
    }
    return promise.future();
  }

  Future<Boolean> isValidEndpoint(String endPoint) {
    Promise<Boolean> promise = Promise.promise();

    LOGGER.debug("Endpoint in JWt is : " + endPoint);
    if (endPoint.equals(api.getRouteItems())
        || endPoint.equals(api.getRouteInstance())
        || endPoint.equals(RATINGS_ENDPOINT)
        || endPoint.equals(MLAYER_INSTANCE_ENDPOINT)
        || endPoint.equals(MLAYER_DOMAIN_ENDPOINT)) {
      promise.complete(true);
    } else {
      LOGGER.error("Incorrect endpoint in jwt");
      promise.fail("Incorrect endpoint in jwt");
    }
    return promise.future();
  }

  /**
   * Validates the user's access to the given API endpoint based
   * on the user's role and the HTTP method used.
   * @param jwtData the user's JWT data
   * @param authenticationInfo a JsonObject containing the HTTP method
   * @return a Future containing a JsonObject with user information if the access is allowed,
   *         or a failure if the access is denied
   */
  public Future<JsonObject> validateAccess(JwtData jwtData, JsonObject authenticationInfo) {
    LOGGER.trace("validateAccess() started");
    Promise<JsonObject> promise = Promise.promise();

    Method method = Method.valueOf(authenticationInfo.getString(METHOD));
    String api = authenticationInfo.getString(API_ENDPOINT);

    AuthorizationRequest authRequest = new AuthorizationRequest(method, api);

    AuthorizationStratergy authStrategy = AuthorizationContextFactory.create(jwtData.getRole(),
            this.api);
    LOGGER.debug("strategy: " + authStrategy.getClass().getSimpleName());

    JwtAuthorization jwtAuthStrategy = new JwtAuthorization(authStrategy);
    LOGGER.debug("endpoint: " + authenticationInfo.getString(API_ENDPOINT));

    if (jwtAuthStrategy.isAuthorized(authRequest, jwtData)) {
      LOGGER.debug("User access is allowed");
      JsonObject response = new JsonObject();
      // adding user id, user role and iid to response for auditing purpose
      response
              .put(USER_ROLE, jwtData.getRole())
              .put(USER_ID, jwtData.getSub());
      if (jwtData.getIid().contains("/")) {
        response.put(IID, (jwtData.getIid().split(":")[1]).split("/")[0]
            + "/" + (jwtData.getIid().split(":")[1]).split("/")[1]);
      } else {
        response.put(IID, jwtData.getIid().split(":")[1]);
      }
      promise.complete(response);
    } else {
      LOGGER.error("user access denied");
      JsonObject result = new JsonObject().put("401", "no access provided to endpoint");
      promise.fail(result.toString());
    }
    return promise.future();
  }


  @Override
  public AuthenticationService tokenInterospect(JsonObject request, JsonObject authenticationInfo,
                                                Handler<AsyncResult<JsonObject>> handler) {
    String endPoint = authenticationInfo.getString(API_ENDPOINT);
    String id = authenticationInfo.getString(ID);
    String token = authenticationInfo.getString(TOKEN);

    Future<JwtData> jwtDecodeFuture = decodeJwt(token);

    ResultContainer result = new ResultContainer();
    boolean skipResourceIdCheck =
            endPoint.equalsIgnoreCase(RATINGS_ENDPOINT);

    jwtDecodeFuture
        .compose(
            decodeHandler -> {
              result.jwtData = decodeHandler;
              return isValidAudienceValue(result.jwtData);
            })
        .compose(
            audienceHandler -> {
              if (skipResourceIdCheck) {
                return Future.succeededFuture(true);
              } else {
                return isValidId(result.jwtData, id);
              }
            })
        .compose(
            validIdHandler -> {
              LOGGER.debug(isValidEndpoint(endPoint).succeeded());
              return isValidEndpoint(endPoint);
            })
        .compose(
            validEndpointHandler -> {
              LOGGER.debug(validateAccess(result.jwtData, authenticationInfo).succeeded());
              return validateAccess(result.jwtData, authenticationInfo);
            })
        .onComplete(
            completeHandler -> {
              if (completeHandler.succeeded()) {
                handler.handle(Future.succeededFuture(completeHandler.result()));
              } else {
                handler.handle(Future.failedFuture(completeHandler.cause().getMessage()));
              }
            });
    return this;
  }
}
