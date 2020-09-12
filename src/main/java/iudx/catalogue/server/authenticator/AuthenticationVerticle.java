package iudx.catalogue.server.authenticator;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.serviceproxy.ServiceBinder;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import static iudx.catalogue.server.authenticator.Constants.*;
import static iudx.catalogue.server.Constants.*;

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

  private static final String AUTH_SERVICE_ADDRESS = "iudx.catalogue.authentication.service";

  private static final Logger LOGGER = LogManager.getLogger(AuthenticationVerticle.class);
  private AuthenticationService authentication;
  private final Properties properties = new Properties();

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, registers the
   * service with the Event bus against an address, publishes the service with the service discovery
   * interface.
   * 
   * @throws Exception which is a startup exception
   */

  @Override
  public void start() throws Exception {

    authentication = new AuthenticationServiceImpl(createWebClient(vertx, properties));


    new ServiceBinder(vertx).setAddress(AUTH_SERVICE_ADDRESS)
      .register(AuthenticationService.class, authentication);
  }

  static WebClient createWebClient(Vertx vertx, Properties properties) {
    return createWebClient(vertx, properties, false);
  }

  /**
   * Helper function to create a WebClient to talk to the auth server. Uses the keystore to get the client certificate
   * required to call Auth APIs (has to be class 1). Since it's a pure function, it can be used as a helper in testing
   * initializations also.
   * @param vertx the vertx instance
   * @param properties the properties field of the verticle
   * @param testing a bool which is used to disable client side ssl checks for testing purposes
   * @return a web client initialized with the relevant client certificate
   */
  static WebClient createWebClient(Vertx vertx, Properties properties, boolean testing) {
    /* Initialize properties from the config file */
    try {
      FileInputStream configFile = new FileInputStream(CONFIG_FILE);
      if (properties.isEmpty()) properties.load(configFile);
    } catch (IOException e) {
      LOGGER.error("Could not load properties from config file", e);
      //TODO: Decide if anything else is required beyond logging?
    }

    WebClientOptions webClientOptions = new WebClientOptions();
    if (testing) webClientOptions.setTrustAll(true).setVerifyHost(false);
    webClientOptions
            .setSsl(true)
            .setKeyStoreOptions(new JksOptions()
                    .setPath(properties.getProperty(KEYSTORE_PATH))
                    .setPassword(properties.getProperty(KEYSTORE_PASSWORD)));
    return WebClient.create(vertx, webClientOptions);
  }
}
