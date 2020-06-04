package iudx.catalogue.server.deploy.helper;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import iudx.catalogue.server.onboarder.OnboarderVerticle;

/**
 * The Catalogue Server Onboarder Service Deployer.
 * <h1>Catalogue Server Onboarder Service Deployer</h1>
 * <p>
 * The Onboarder Service Deployer deploys the Onboarder Service Verticle.
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

public class OnboarderServiceDeployer {

  private static final Logger logger = LoggerFactory.getLogger(OnboarderServiceDeployer.class);
  private static Vertx vertx;
  private static ClusterManager mgr;
  private static VertxOptions options;

  /**
   * The main method implements the deploy helper script for deploying the Onboarder Service
   * verticle in the catalogue server.
   * 
   * @param args which is a String array
   */

  public static void main(String[] args) {

    /** Create a reference to HazelcastClusterManager. */

    mgr = new HazelcastClusterManager();
    options = new VertxOptions().setClusterManager(mgr);

    /** Create or Join a Vert.x Cluster. */

    Vertx.clusteredVertx(options, res -> {
      if (res.succeeded()) {
        vertx = res.result();

        /** Deploy the Onboarder Service Verticle. */

        vertx.deployVerticle(new OnboarderVerticle(), OnboarderVerticle -> {
          if (OnboarderVerticle.succeeded()) {
            logger.info("The Onboarder Service is ready");
          } else {
            logger.info("The Onboarder Service failed !");
          }
        });
      } else {
        logger.info("The Vertx Cluster failed !");
      }
    });

  }

}
