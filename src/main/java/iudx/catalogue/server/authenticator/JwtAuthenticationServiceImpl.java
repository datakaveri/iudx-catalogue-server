package iudx.catalogue.server.authenticator;

import static iudx.catalogue.server.auditing.util.Constants.*;
import static iudx.catalogue.server.authenticator.Constants.*;
import static iudx.catalogue.server.authenticator.Constants.METHOD;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_INSTANCE;
import static iudx.catalogue.server.util.Constants.ITEM_TYPE_RESOURCE_SERVER;
import static iudx.catalogue.server.util.Constants.PROVIDER;
import static iudx.catalogue.server.util.Constants.PROVIDER_KC_ID;

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
 *
 * <h1>JWT Authentication Service Implementation</h1>
 *
 * <p>The JWT Authentication Service implementation in the IUDX Catalogue Server implements the
 * definitions of the {@link iudx.catalogue.server.authenticator.AuthenticationService}.
 *
 * @version 1.0
 * @since 2021-09-23
 */
public class JwtAuthenticationServiceImpl implements AuthenticationService {
  private static final Logger LOGGER = LogManager.getLogger(JwtAuthenticationServiceImpl.class);

  final JWTAuth jwtAuth;
  final String audience;
  private Api api;
  private String copAdmin;

  JwtAuthenticationServiceImpl(
      Vertx vertx, final JWTAuth jwtAuth, final JsonObject config, final Api api) {
    this.jwtAuth = jwtAuth;
    this.audience = config.getString("host");
    this.copAdmin = config.getString(COP_ADMIN);
    this.api = api;
  }

  Future<JwtData> decodeJwt(String jwtToken) {
    Promise<JwtData> promise = Promise.promise();

    TokenCredentials credentials = new TokenCredentials(jwtToken);

    jwtAuth
        .authenticate(credentials)
        .onSuccess(
            user -> {
              JwtData jwtData = new JwtData(user.principal());
              promise.complete(jwtData);
            })
        .onFailure(
            err -> {
              LOGGER.error("failed to decode/validate jwt token : " + err.getMessage());
              promise.fail("failed");
            });
    return promise.future();
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

  Future<Boolean> isValidId(JwtData jwtData, String provider) {
    Promise<Boolean> promise = Promise.promise();

    String jwtId = "";
    if (jwtData.getRole().equalsIgnoreCase(PROVIDER)) {

      jwtId = jwtData.getSub();
    } else if (jwtData.getRole().equalsIgnoreCase("delegate")) {
      // TODO: add logic for delegate once delegate token is updated
    }
    LOGGER.debug(provider);
    LOGGER.debug(jwtId);

    if (provider.equalsIgnoreCase(jwtId)) {
      promise.complete(true);
    } else {
      LOGGER.error("Incorrect sub value in jwt");
      promise.fail("Incorrect sub value in jwt");
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
   * Validates the user's access to the given API endpoint based on the user's role and the HTTP
   * method used.
   *
   * @param jwtData the user's JWT data
   * @param authenticationInfo a JsonObject containing the HTTP method
   * @return a Future containing a JsonObject with user information if the access is allowed, or a
   *     failure if the access is denied
   */
  public Future<JsonObject> validateAccess(
      JwtData jwtData, JsonObject authenticationInfo, String itemType) {
    LOGGER.trace("validateAccess() started");
    Promise<JsonObject> promise = Promise.promise();

    Method method = Method.valueOf(authenticationInfo.getString(METHOD));
    String api = authenticationInfo.getString(API_ENDPOINT);

    AuthorizationRequest authRequest = new AuthorizationRequest(method, api);

    AuthorizationStratergy authStrategy =
        AuthorizationContextFactory.create(jwtData.getRole(), this.api);
    LOGGER.debug("strategy: " + authStrategy.getClass().getSimpleName());

    JwtAuthorization jwtAuthStrategy = new JwtAuthorization(authStrategy);
    LOGGER.debug("endpoint: " + authenticationInfo.getString(API_ENDPOINT));

    if (jwtAuthStrategy.isAuthorized(authRequest, jwtData)) {
      // Don't allow access to delete resource server item for anyone except Admin
      if (itemType.equals(ITEM_TYPE_RESOURCE_SERVER)
          && method.equals(Method.DELETE)
          && !authStrategy.getClass().getSimpleName().equalsIgnoreCase("AdminAuthStrategy")) {
        JsonObject result = new JsonObject().put("401", "no access provided to endpoint");
        promise.fail(result.toString());
      }
      LOGGER.debug("User access is allowed");
      JsonObject response = new JsonObject();
      // adding user id, user role and iid to response for auditing purpose
      response.put(USER_ROLE, jwtData.getRole()).put(USER_ID, jwtData.getSub());
      promise.complete(response);
    } else {
      LOGGER.error("user access denied");
      JsonObject result = new JsonObject().put("401", "no access provided to endpoint");
      promise.fail(result.toString());
    }
    return promise.future();
  }

  @Override
  public AuthenticationService tokenInterospect(
      JsonObject request, JsonObject authenticationInfo, Handler<AsyncResult<JsonObject>> handler) {
    String endPoint = authenticationInfo.getString(API_ENDPOINT);
    String id = authenticationInfo.getString(ID);
    String provider = authenticationInfo.getString(PROVIDER_KC_ID);
    String token = authenticationInfo.getString(TOKEN);
    String method = authenticationInfo.getString(METHOD);
    String itemType =
        method.equalsIgnoreCase(Method.DELETE.toString())
            ? authenticationInfo.getString(ITEM_TYPE)
            : "";

    Future<JwtData> jwtDecodeFuture = decodeJwt(token);

    ResultContainer result = new ResultContainer();
    boolean skipProviderIdCheck =
        endPoint.equalsIgnoreCase(api.getRouteInstance())
            || endPoint.equalsIgnoreCase(RATINGS_ENDPOINT)
            || itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_SERVER);

    jwtDecodeFuture
        .compose(
            decodeHandler -> {
              result.jwtData = decodeHandler;
              return isValidAudienceValue(result.jwtData);
            })
        .compose(
            audienceHandler -> {
              if (skipProviderIdCheck) {
                LOGGER.debug("here xxxxxxxxx");
                return Future.succeededFuture(true);
              } else {
                return isValidId(result.jwtData, provider);
              }
            })
        .compose(
            validIdHandler -> {
              return isValidEndpoint(endPoint);
            })
        .compose(
            validEndpointHandler -> {
              // verify admin if Resource Server item is to be deleted
              if (itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_SERVER)
                  || itemType.equalsIgnoreCase(ITEM_TYPE_INSTANCE)) {
                return Util.isValidAdmin(copAdmin, result.jwtData);
              } else {
                return Future.succeededFuture(true);
              }
            })
        .compose(
            validAdmin -> {
              return validateAccess(result.jwtData, authenticationInfo, itemType);
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

  // class to contain intermediate data for token introspection
  final class ResultContainer {
    JwtData jwtData;
  }
}
