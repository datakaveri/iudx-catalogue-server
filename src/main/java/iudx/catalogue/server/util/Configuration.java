package iudx.catalogue.server.util;

import io.vertx.core.json.JsonObject;
import org.apache.commons.io.IOUtils;
import java.io.FileInputStream;

public class Configuration {

  /**
   * This is to read the config.json file from fileSystem to load configuration.
   * 
   * @param filePath
   * @return dbIndex
   */
  public static String getDbIndex() {

    String filePath = "./configs/config-test.json";

    try(FileInputStream inputStream = new FileInputStream(filePath)) {     
    String confFile = IOUtils.toString(inputStream);
    String indexName = new JsonObject(confFile).getString("databaseIndex");
    return indexName;
    } catch (Exception e) {
      return null;
    }
  }
}
