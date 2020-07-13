package iudx.catalogue.server.apiserver.util;

import io.vertx.core.MultiMap;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * QueryMapper class to convert NGSILD query into json object for the purpose of information
 * exchange among different verticals.
 *
 */
public class QueryMapper {

  private static final Logger logger = LoggerFactory.getLogger(QueryMapper.class);

  /**
   * Converts the query parameters to jsonObject and jsonArray.
   *
   * @param queryParameters of the request.
   * @return jsonObject of queryParameters
   */
  public static JsonObject map2Json(MultiMap queryParameters) {

    JsonObject jsonBody = new JsonObject();

    ArrayList<String> excepAttribute = new ArrayList<String>();
    excepAttribute.add(Constants.COORDINATES);
    excepAttribute.add(Constants.OFFSET);
    excepAttribute.add(Constants.LIMIT);
    excepAttribute.add(Constants.MAX_DISTANCE);
    excepAttribute.add(Constants.Q_VALUE);

    Pattern regPatternMatchString = Pattern.compile("[\\w]+[^\\,]*(?:\\.*[\\w])");
    Pattern regPatternText = Pattern.compile("^[\\*]{0,1}[A-Za-z ]+[\\*]{0,1}");

    for (Entry<String, String> entry : queryParameters.entries()) {

      String paramValue = entry.getValue().replaceAll("^\"|\"$", "").trim();
      if (!paramValue.startsWith("[") && !paramValue.endsWith("]")) {
        if (!excepAttribute.contains(entry.getKey())) {
          jsonBody.put(entry.getKey(), paramValue);
        } else if (excepAttribute.contains(entry.getKey()) && !entry.getKey().equals("q")) {
          jsonBody.put(entry.getKey(), Integer.parseInt(paramValue));
        } else if (entry.getKey().equals(Constants.Q_VALUE)
            && !regPatternText.matcher(paramValue).matches()) {
          logger.info("Invalid text string");
          return null;
        } else {
          jsonBody.put(entry.getKey(), paramValue);
        }
      } else {
        Matcher matcher = regPatternMatchString.matcher(entry.getValue());
        if (matcher.find() && !excepAttribute.contains(entry.getKey())) {
          String replacedValue = paramValue.replaceAll("[\\w]+[^\\,]*(?:\\.*[\\w])", "\"$0\"");
          jsonBody.put(entry.getKey(), new JsonArray(replacedValue));
        } else if (excepAttribute.contains(entry.getKey())) {
          try {
            jsonBody.put(entry.getKey(), new JsonArray(paramValue));
          } catch (DecodeException decodeException) {
            logger.error("Invalid Json value ".concat(decodeException.getMessage()));
            return null;
          }
        }
      }
    }

    if (jsonBody.containsKey(Constants.GEOPROPERTY)) {
      jsonBody.put(Constants.SEARCH_TYPE, Constants.GEO_SEARCH);
    } else if (jsonBody.containsKey(Constants.Q_VALUE)) {
      jsonBody.put(Constants.SEARCH_TYPE, Constants.TEXT_SEARCH);
    } else if (jsonBody.containsKey(Constants.PROPERTY)) {
      if (jsonBody.getJsonArray(Constants.PROPERTY).getString(0).equals(Constants.TAGS)) {
        jsonBody.put(Constants.SEARCH_TYPE, Constants.TAGS_SEARCH);
      } else {
        jsonBody.put(Constants.SEARCH_TYPE, Constants.ATTRIBUTE_SEARCH);
      }
    }

    return jsonBody;
  }
}
