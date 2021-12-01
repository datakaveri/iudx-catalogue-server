package iudx.catalogue.server.authenticator;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.ext.auth.jwt.JWTAuth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;

import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.auth.PubSecKeyOptions;


import static iudx.catalogue.server.authenticator.Constants.*;
import static iudx.catalogue.server.util.Constants.*;

/**
 * The Authentication Verticle.
 * <h1>Authentication Verticle</h1>
 * <p>
 * The Authentication Verticle implementation in the the IUDX Catalogue Server exposes the
 * {@link iudx.catalogue.server.authenticator.AuthenticationService} over the Vert.x Event Bus.
 * </p>
 * 
 * @version 1.0
 * @since 2020-05-31
 */

public class AuthenticationVerticle extends AbstractVerticle {


  private static final Logger LOGGER = LogManager.getLogger(AuthenticationVerticle.class);
  private AuthenticationService jwtAuthenticationService;
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;
  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, registers the
   * service with the Event bus against an address, publishes the service with the service discovery
   * interface.
   * 
   * @throws Exception which is a startup exception
   */

  @Override
  public void start() throws Exception {
    binder = new ServiceBinder(vertx);
    LOGGER.debug("Info: Auth type set to JWT Auth");


    JWTAuthOptions jwtAuthOptions = new JWTAuthOptions();
    jwtAuthOptions.addPubSecKey(
        new PubSecKeyOptions()
            .setAlgorithm("ES256")
            .setBuffer("-----BEGIN PUBLIC KEY-----\n" +
                "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE8BKf2HZ3wt6wNf30SIsbyjYPkkTS\n" +
                "GGyyM2/MGF/zYTZV9Z28hHwvZgSfnbsrF36BBKnWszlOYW0AieyAUKaKdg==\n" +
                "-----END PUBLIC KEY-----\n" +
                ""));
    /* Default jwtIgnoreExpiry is false. If set through config, then that value is taken */
    boolean jwtIgnoreExpiry = config().getBoolean("jwtIgnoreExpiry") == null ? false
        : config().getBoolean("jwtIgnoreExpiry");
    if (jwtIgnoreExpiry) {
      jwtAuthOptions.getJWTOptions().setIgnoreExpiration(true);
      LOGGER.warn("JWT ignore expiration set to true, do not set IgnoreExpiration in production!!");
    }
    JWTAuth jwtAuth = JWTAuth.create(vertx, jwtAuthOptions);

    jwtAuthenticationService = new JwtAuthenticationServiceImpl(vertx, jwtAuth, config());

    consumer = binder.setAddress(AUTH_SERVICE_ADDRESS)
      .register(AuthenticationService.class, jwtAuthenticationService);
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}

