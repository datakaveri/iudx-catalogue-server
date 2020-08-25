package iudx.catalogue.server.deploy;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

import io.vertx.core.eventbus.EventBusOptions;

import io.vertx.core.cli.CLI;
import io.vertx.core.cli.Option;
import io.vertx.core.cli.CommandLine;

import iudx.catalogue.server.apiserver.ApiServerVerticle;
import iudx.catalogue.server.authenticator.AuthenticationVerticle;
import iudx.catalogue.server.database.DatabaseVerticle;
import iudx.catalogue.server.validator.ValidatorVerticle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * DeploySingle - Deploy a single non-clustered catalogue instance
 **/
public class DeploySingle {
  private static final Logger LOGGER = LogManager.getLogger(DeploySingle.class);

  private static AbstractVerticle getVerticle(String name) {
    switch (name) {
      case "api":
        return new ApiServerVerticle();
      case "db":
        return new DatabaseVerticle();
      case "val":
        return new ValidatorVerticle();
      case "auth":
        return new AuthenticationVerticle();
    }
    return null;
  }

  public static void recursiveDeploy(Vertx vertx, List<String> modules, int i) {
    if (i >= modules.size()) {
      LOGGER.info("Deployed all");
      return;
    }
    String moduleName = modules.get(i);
    vertx.deployVerticle(getVerticle(moduleName), ar -> {
      if (ar.succeeded()) {
        LOGGER.info("Deployed " + moduleName);
        recursiveDeploy(vertx, modules, i + 1);
      } else {
        LOGGER.fatal("Failed to deploy " + moduleName + " cause:", ar.cause());
      }
    });
  }

  public static void deploy(List<String> modules, String host) {
    EventBusOptions ebOptions = new EventBusOptions();
    VertxOptions options = new VertxOptions().setEventBusOptions(ebOptions);
    Vertx vertx = Vertx.vertx(options);
    recursiveDeploy(vertx, modules, 0);
  }

  public static void main(String[] args) {
    CLI cli = CLI.create("IUDX Cat").setSummary("A CLI to deploy the catalogue")
        .addOption(new Option().setLongName("help").setShortName("h").setFlag(true)
            .setDescription("display help"))
        .addOption(new Option().setLongName("modules").setShortName("m").setMultiValued(true)
            .setRequired(true).setDescription("modules to launch").addChoice("api")
            .addChoice("db").addChoice("auth").addChoice("val"))
        .addOption(new Option().setLongName("host").setShortName("i").setRequired(true)
            .setDescription("public host"));

    StringBuilder usageString = new StringBuilder();
    cli.usage(usageString);
    CommandLine commandLine = cli.parse(Arrays.asList(args), false);
    if (commandLine.isValid() && !commandLine.isFlagEnabled("help")) {
      List<String> modules = new ArrayList<String>(commandLine.getOptionValues("modules"));
      String host = commandLine.getOptionValue("host");
      deploy(modules, host);
    } else {
      System.out.println(usageString);
    }
  }
}

