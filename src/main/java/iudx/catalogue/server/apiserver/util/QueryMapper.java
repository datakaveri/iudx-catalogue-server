package iudx.catalogue.server.apiserver.util;

import io.vertx.core.MultiMap;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.util.Constants.*;

/**
 * QueryMapper class to convert NGSILD query into json object for the purpose of debugrmation
 * exchange among different verticals.
 *
 */
public class QueryMapper {

  private static final Logger LOGGER = LogManager.getLogger(QueryMapper.class);

  /**
   * Converts the query parameters to jsonObject and jsonArray.
   *
   * @param queryParameters of the request.
   * @return jsonObject of queryParameters
   */
  public static JsonObject map2Json(MultiMap queryParameters) {

    JsonObject jsonBody = new JsonObject();

    ArrayList<String> excepAttribute = new ArrayList<String>();
    excepAttribute.add(COORDINATES);
    excepAttribute.add(OFFSET);
    excepAttribute.add(LIMIT);
    excepAttribute.add(MAX_DISTANCE);
    excepAttribute.add(Q_VALUE);

    Pattern regPatternMatchString = Pattern.compile("[\\w]+[^\\,]*(?:\\.*[\\w])");
    Pattern regPatternText = Pattern.compile("^[\\*]{0,1}[A-Za-z0-9\\-\\_ ]+[\\*]{0,1}");

    LOGGER.debug("In query mapper");

    for (Entry<String, String> entry : queryParameters.entries()) {

      String paramValue = entry.getValue().replaceAll("^\"|\"$", "").trim();
      if (!paramValue.startsWith("[") && !paramValue.endsWith("]")) {
        if (!excepAttribute.contains(entry.getKey())) {
          jsonBody.put(entry.getKey(), paramValue);
        } else if (excepAttribute.contains(entry.getKey()) && !entry.getKey().equals("q")) {
          jsonBody.put(entry.getKey(), Integer.parseInt(paramValue));
        } else if (entry.getKey().equals(Q_VALUE)
            && !regPatternText.matcher(paramValue).matches()) {
          LOGGER.debug("Error: Invalid text string");
          return null;
        } else {
          jsonBody.put(entry.getKey(), paramValue);
        }
      } else {
        try {
          Matcher matcher = regPatternMatchString.matcher(entry.getValue());
          if (matcher.find() && !excepAttribute.contains(entry.getKey())) {
            String replacedValue = paramValue.replaceAll("[\\w]+[^\\,]*(?:\\.*[\\w])", "\"$0\"");
            jsonBody.put(entry.getKey(), new JsonArray(replacedValue));
          } else if (excepAttribute.contains(entry.getKey())) {
            try {
              jsonBody.put(entry.getKey(), new JsonArray(paramValue));
            } catch (DecodeException decodeException) {
              LOGGER.error("Info: Invalid Json value " + decodeException.getMessage());
              return null;
            }
          }
        } catch (Exception e) {
          LOGGER.error("Info: Invalid Json value ");
          return null;
        }
      }
    }

    /* adding search type for geo related search */
    if (jsonBody.containsKey(GEOMETRY)) {
      jsonBody.put(SEARCH_TYPE,
          jsonBody.getString(SEARCH_TYPE, "") + SEARCH_TYPE_GEO);
    }

    /* adding search type for text related search */
    if (jsonBody.containsKey(Q_VALUE)) {
      jsonBody.put(SEARCH_TYPE,
          jsonBody.getString(SEARCH_TYPE, "") + SEARCH_TYPE_TEXT);
    }

    /* Tag related search are to be considered as attribute search and are being merged as one */
    if (jsonBody.containsKey(PROPERTY)) {

      jsonBody.put(SEARCH_TYPE,
          jsonBody.getString(SEARCH_TYPE, "") + SEARCH_TYPE_ATTRIBUTE);
    }

    /* adding response filter */
    if (jsonBody.containsKey(FILTER)) {
      jsonBody.put(SEARCH_TYPE,
          jsonBody.getString(SEARCH_TYPE, "") + RESPONSE_FILTER);
    }

    LOGGER.debug("Info: Json Query Mapped: " + jsonBody);

    return jsonBody;
  }
}
