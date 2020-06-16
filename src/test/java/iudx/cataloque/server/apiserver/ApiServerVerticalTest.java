package iudx.cataloque.server.apiserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.Duration;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import iudx.catalogue.server.apiserver.ApiServerVerticle;
import iudx.catalogue.server.authenticator.AuthenticationVerticle;
import iudx.catalogue.server.database.DatabaseVerticle;
import iudx.catalogue.server.onboarder.OnboarderVerticle;
import iudx.catalogue.server.validator.ValidatorVerticle;

@ExtendWith(VertxExtension.class)
public class ApiServerVerticalTest {

  private static final Logger logger = LoggerFactory.getLogger(ApiServerVerticalTest.class);
  private static Vertx vertx;


  @BeforeEach
  public void setUp(VertxTestContext testContext) {
    ClusterManager mgr = new HazelcastClusterManager();
    VertxOptions options = new VertxOptions().setClusterManager(mgr);

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

                    /* Deploy the Onboarder Service Verticle. */

                    vertx.deployVerticle(new OnboarderVerticle(), onboarderVerticle -> {
                      if (onboarderVerticle.succeeded()) {
                        logger.info("The Onboarder Service is ready");

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
                        logger.info("The Onboarder Service failed !");
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
    testContext.completed();


    // String[] args = null;

    // CatalogueServerDeployer.main(args);
  }

  @AfterEach
  public void finish(VertxTestContext testContext) {
    System.out.println("after");
    vertx.close(testContext.succeeding(response -> {
      testContext.completeNow();
    }));
  }


  @Test
  @DisplayName("Testing Addition operation of Calculator 200")
  public void testAdd200(io.vertx.reactivex.core.Vertx vertx, VertxTestContext testContext) {
    Awaitility.waitAtMost(Duration.ofMinutes(1L)).await();

    WebClient client = WebClient.create(vertx);

    client.get(8443, "localhost", "/iudx/cat/v1/domains").as(BodyCodec.string())
        .send(testContext.succeeding(response -> testContext.verify(() -> {
          // JsonObject res = new JsonObject(response.body());
          assertEquals(response.statusCode(), 200);
          // assertEquals(res.getInteger("addition_result"), 30);
          testContext.completeNow();
        })));

  }
}
