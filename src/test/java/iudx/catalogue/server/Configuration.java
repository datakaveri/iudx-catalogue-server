package iudx.catalogue.server;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.FileInputStream;
import org.apache.commons.io.IOUtils;



public class Configuration {

  private static final Logger LOGGER = LogManager.getLogger(Configuration.class);


  public static JsonObject getConfiguration(String filePath) {

    try(FileInputStream inputStream = new FileInputStream(filePath)) {     
    String confFile = IOUtils.toString(inputStream);
    JsonObject conf = new JsonObject(confFile);
    return conf;
    } catch (Exception e) {
      return new JsonObject();
    }
  }

  /**
   * This is to read the config.json file from fileSystem to load configuration.
   * 
   * @param moduleIndex
   * @param vertx
   * @return module JsonObject
   */
  public static JsonObject getConfiguration(String filePath, int index) {

    try(FileInputStream inputStream = new FileInputStream(filePath)) {     
    String confFile = IOUtils.toString(inputStream);
    JsonObject conf = new JsonObject(confFile).getJsonArray("modules").getJsonObject(index);
    return conf;
    } catch (Exception e) {
      return new JsonObject();
    }
  }

}
