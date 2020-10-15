package iudx.catalogue.server.deploy;


import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import iudx.catalogue.server.apiserver.ApiServerVerticle;
import iudx.catalogue.server.authenticator.AuthenticationVerticle;
import iudx.catalogue.server.database.DatabaseVerticle;
import iudx.catalogue.server.validator.ValidatorVerticle;
import io.vertx.core.DeploymentOptions;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;


public class ReDeployerDev extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(ReDeployerDev.class);
  private static VertxOptions options;

  @Override
  public void start(Promise<Void> promise) throws Exception {


    /* Deploy the Database Service Verticle. */
    /* Get configs. Note the order matters. Don't change it */
    JsonObject dbConfig = config().getJsonArray("modules").getJsonObject(0);
    JsonObject authConfig = config().getJsonArray("modules").getJsonObject(1);
    JsonObject valConfig = config().getJsonArray("modules").getJsonObject(2);
    JsonObject apiConfig = config().getJsonArray("modules").getJsonObject(3);

    vertx.deployVerticle(new DatabaseVerticle(), new DeploymentOptions().setConfig(dbConfig),
        databaseVerticle -> {
      if (databaseVerticle.succeeded()) {
        LOGGER.info("The Database Service is ready");

        /* Deploy the Authentication Service Verticle. */

        vertx.deployVerticle(new AuthenticationVerticle(), new DeploymentOptions().setConfig(authConfig),
            authenticationVerticle -> {
          if (authenticationVerticle.succeeded()) {
            LOGGER.info("The Authentication Service is ready");

            /* Deploy the Validator Service Verticle. */

            vertx.deployVerticle(new ValidatorVerticle(), new DeploymentOptions().setConfig(valConfig),
                validatorVerticle -> {
              if (validatorVerticle.succeeded()) {
                LOGGER.info("The Validator Service is ready");


                /* Deploy the Api Server Verticle. */

                vertx.deployVerticle(new ApiServerVerticle(), new DeploymentOptions().setConfig(apiConfig),
                    apiServerVerticle -> {
                  if (apiServerVerticle.succeeded()) {
                    LOGGER.info("The Catalogue API Server is ready");
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
  }
}

