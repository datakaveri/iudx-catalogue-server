package iudx.catalogue.server.nlpsearch;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.serviceproxy.ServiceBinder;

import static iudx.catalogue.server.util.Constants.*;

/**
 * The NLPSearch Verticle.
 * <h1>A NLPSearch Verticle</h1>
 * <p>
 * The NLPSearch Verticle implementation in the the IUDX Catalogue Server exposes the
 * {@link iudx.catalogue.server.nlpsearch.NLPSearchService} over the Vert.x Event Bus.
 * </p>
 * 
 * @version 1.0
 * @since 2020-05-31
 */

public class NLPSearchVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(NLPSearchVerticle.class);
  private NLPSearchService NlpSearch;
  private String nlpServiceUrl;
  private int nlpServicePort;

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, registers the
   * service with the Event bus against an address, publishes the service with the service discovery
   * interface.
   * 
   * @throws Exception which is a startup exception
   */

  @Override
  public void start() throws Exception {

    nlpServiceUrl = config().getString("nlpServiceUrl");
    nlpServicePort = config().getInteger("nlpServicePort");
    NlpSearch = new NLPSearchServiceImpl(createWebClient(vertx, config()),
                                          nlpServiceUrl, nlpServicePort);

    new ServiceBinder(vertx).setAddress(NLP_SERVICE_ADDRESS)
      .register(NLPSearchService.class, NlpSearch);
  }

  static WebClient createWebClient(Vertx vertx, JsonObject config) {
    return createWebClient(vertx, config, false);
  }

  /**
   * Helper function to create a WebClient to talk to the nlpsearch server.
   * @param vertx the vertx instance
   * @param properties the properties field of the verticle
   * @param testing a bool which is used to disable client side ssl checks for testing purposes
   * @return a web client initialized with the relevant client certificate
   */
  static WebClient createWebClient(Vertx vertx, JsonObject config, boolean testing) {
    /* Initialize properties from the config file */
    WebClientOptions webClientOptions = new WebClientOptions();
    if (testing) webClientOptions.setTrustAll(true).setVerifyHost(false);
    return WebClient.create(vertx, webClientOptions);
  }
}
