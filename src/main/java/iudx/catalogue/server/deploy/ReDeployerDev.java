package iudx.catalogue.server.deploy;


import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import iudx.catalogue.server.apiserver.ApiServerVerticle;
import iudx.catalogue.server.authenticator.AuthenticationVerticle;
import iudx.catalogue.server.database.DatabaseVerticle;
import iudx.catalogue.server.validator.ValidatorVerticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;


public class ReDeployerDev extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(ReDeployerDev.class);
  private static Vertx vertx;
  private static ClusterManager mgr;
  private static VertxOptions options;

  @Override
  public void start(Promise<Void> promise) throws Exception {

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
            LOGGER.info("The Database Service is ready");

            /* Deploy the Authentication Service Verticle. */

            vertx.deployVerticle(new AuthenticationVerticle(), authenticationVerticle -> {
              if (authenticationVerticle.succeeded()) {
                LOGGER.info("The Authentication Service is ready");

                /* Deploy the Validator Service Verticle. */

                vertx.deployVerticle(new ValidatorVerticle(), validatorVerticle -> {
                  if (validatorVerticle.succeeded()) {
                    LOGGER.info("The Validator Service is ready");


                    /* Deploy the Api Server Verticle. */

                    vertx.deployVerticle(new ApiServerVerticle(), apiServerVerticle -> {
                      if (apiServerVerticle.succeeded()) {
                        LOGGER.info("The Catalogue API Server is ready at 8443");
                        LOGGER.info("Check /apis/ for supported APIs");
                        promise.complete();
                      } else {
                        LOGGER.info("The Catalogue API Server startup failed !");
                      }
                    });

                  } else {
                    LOGGER.info("The Validator Service failed !");
                  }
                });

              } else {
                LOGGER.info("The Authentication Service failed !");
              }
            });

          } else {
            LOGGER.info("The Database Service failed !");
          }
        });
      } else {
        LOGGER.info("The Vertx Cluster failed !");
      }
    });

  }
}

