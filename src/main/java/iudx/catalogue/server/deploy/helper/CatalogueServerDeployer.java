package iudx.catalogue.server.deploy.helper;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import iudx.catalogue.server.apiserver.ApiServerVerticle;
import iudx.catalogue.server.authenticator.AuthenticationVerticle;
import iudx.catalogue.server.database.DatabaseVerticle;
import iudx.catalogue.server.validator.ValidatorVerticle;

/**
 * The Catalogue Server Deployer.
 * <h1>Catalogue Server Deployer</h1>
 * <p>
 * The Catalogue Server deploys the API Server, Database, Onboarder, Authentication, Validator
 * Verticles.
 * </p>
 * 
 * @see io.vertx.core.Vertx
 * @see io.vertx.core.AbstractVerticle
 * @see io.vertx.core.http.HttpServer
 * @see io.vertx.ext.web.Router
 * @see io.vertx.servicediscovery.ServiceDiscovery
 * @see io.vertx.servicediscovery.types.EventBusService
 * @see io.vertx.spi.cluster.hazelcast.HazelcastClusterManager
 * @version 1.0
 * @since 2020-05-31
 */

public class CatalogueServerDeployer {

  private static final Logger logger = LoggerFactory.getLogger(CatalogueServerDeployer.class);
  private static Vertx vertx;
  private static ClusterManager mgr;
  private static VertxOptions options;

  /**
   * The main method implements the deploy helper script for deploying the the catalogue server.
   * 
   * @param args which is a String array
   */

  public static void main(String[] args) {

    /* Create a reference to HazelcastClusterManager. */

    mgr = new HazelcastClusterManager();
    options = new VertxOptions().setClusterManager(mgr);

    /* Create or Join a Vert.x Cluster. */

    Vertx.clusteredVertx(options, res -> {
      if (res.succeeded()) {
        vertx = res.result();

        /* Deploy the Database Service Verticle. */

        vertx.deployVerticle(new DatabaseVerticle(), databaseVerticle -> {
          if (databaseVerticle.succeeded()) {
            logger.info("The Database Service is ready");

            /* Deploy the Authentication Service Verticle. */

            vertx.deployVerticle(new AuthenticationVerticle(), authenticationVerticle -> {
              if (authenticationVerticle.succeeded()) {
                logger.info("The Authentication Service is ready");

                /* Deploy the Validator Service Verticle. */

                vertx.deployVerticle(new ValidatorVerticle(), validatorVerticle -> {
                  if (validatorVerticle.succeeded()) {
                    logger.info("The Validator Service is ready");

                        /* Deploy the Api Server Verticle. */

                        vertx.deployVerticle(new ApiServerVerticle(), apiServerVerticle -> {
                          if (apiServerVerticle.succeeded()) {
                            logger.info("The Catalogue API Server is ready at 8443");
                            logger.info("Check /apis/ for supported APIs");
                          } else {
                            logger.info("The Catalogue API Server startup failed !");
                          }
                        });
                  } else {
                    logger.info("The Validator Service failed !");
                  }
                });

              } else {
                logger.info("The Authentication Service failed !");
              }
            });

          } else {
            logger.info("The Database Service failed !");
          }
        });
      } else {
        logger.info("The Vertx Cluster failed !");
      }
    });

  }

}
