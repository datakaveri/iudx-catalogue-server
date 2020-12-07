package iudx.catalogue.server.apiserver.util;

import io.vertx.core.MultiMap;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.common.collect.Range;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
          jsonBody.put(entry.getKey(), Double.valueOf(paramValue).intValue());
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
      jsonBody.put(SEARCH_TYPE, jsonBody.getString(SEARCH_TYPE, "") + SEARCH_TYPE_GEO);
    }

    /* adding search type for text related search */
    if (jsonBody.containsKey(Q_VALUE)) {
      jsonBody.put(SEARCH_TYPE, jsonBody.getString(SEARCH_TYPE, "") + SEARCH_TYPE_TEXT);
    }

    /* Tag related search are to be considered as attribute search and are being merged as one */
    if (jsonBody.containsKey(PROPERTY)) {

      jsonBody.put(SEARCH_TYPE, jsonBody.getString(SEARCH_TYPE, "") + SEARCH_TYPE_ATTRIBUTE);
    }

    /* adding response filter */
    if (jsonBody.containsKey(FILTER)) {
      jsonBody.put(SEARCH_TYPE, jsonBody.getString(SEARCH_TYPE, "") + RESPONSE_FILTER);
    }

    LOGGER.debug("Info: Json Query Mapped: " + jsonBody);

    return jsonBody;
  }


  /**
   * Validates the request parameters, headers to compliance with default values.
   * 
   * @param JsonObject requestBody
   * @return JsonObject having success and failure status
   */
  public static JsonObject validateQueryParam(JsonObject requestBody) {

    LOGGER.debug("Info: Validating attributes limits and  constraints");
    JsonObject errResponse = new JsonObject().put(STATUS, FAILED);

    /* Validating GeoSearch limits */
    String searchType = requestBody.getString(SEARCH_TYPE, "");
    if (searchType.contains(SEARCH_TYPE_GEO)) {

      /* Checking limits and precision of coordinate attributes */
      if (requestBody.containsKey(COORDINATES)) {

        Pattern pattern = Pattern.compile("[\\w]+[^\\,]*(?:\\.*[\\w])");
        Matcher matcher = pattern.matcher(requestBody.getJsonArray(COORDINATES).toString());
        List<String> coordinatesValues =
            matcher.results().map(MatchResult::group).collect(Collectors.toList());

        if (coordinatesValues.size() <= COORDINATES_SIZE * 2) {
          for (String value : coordinatesValues) {

            Double tempValue = Double.parseDouble(value);
            if (Double.isFinite(tempValue)) {

              boolean isPrecise = (BigDecimal.valueOf(tempValue).scale() <= COORDINATES_PRECISION);

              if (isPrecise == Boolean.FALSE) {
                LOGGER.error("Error: Overflow coordinate precision");
                return errResponse.put(DESC,
                    "The max point of 'coordinates' precision is " + COORDINATES_PRECISION);
              }
            } else {
              LOGGER.error("Error: Overflow coordinate value");
              return errResponse.put(DESC, "Unable to parse 'coordinates'; value is " + tempValue);
            }
          }
        } else {
          LOGGER.error("Error: Overflow coordinate values");
          return errResponse.put(DESC,
              "The max number of 'coordinates' value is " + COORDINATES_SIZE);
        }
      }

      /* Validating maxDistance attribute for positive integer */
      if (requestBody.containsKey(MAX_DISTANCE)
          && !Range.closed(0, MAXDISTANCE_LIMIT).contains(requestBody.getInteger(MAX_DISTANCE))) {
        LOGGER.error("Error: maxDistance should range between 0-10000m");
        return errResponse.put(DESC,
            "The 'maxDistance' should range between 0-10000m");
      }
    }

    /* Validating text search limits */
    if (searchType.contains(SEARCH_TYPE_TEXT)) {

      String searchString = requestBody.getString(Q_VALUE);
      if (searchString.length() > STRING_SIZE) {
        LOGGER.error("Error: 'q' must be " + STRING_SIZE + " in char");
        return errResponse.put(DESC,
            "The max string(q) size supported is " + STRING_SIZE);
      }
    }

    /* Validating AttributeSearch limits */
    if (searchType.contains(SEARCH_TYPE_ATTRIBUTE)) {
      
      /* Checking the number of property and value within the request */
      if (requestBody.getJsonArray(PROPERTY).size() <= PROPERTY_SIZE) {
        JsonArray values = requestBody.getJsonArray(VALUE);

        if (values.size() <= VALUE_SIZE) {
          for (Object value : values) {
            JsonArray nestedValue = (JsonArray) value;
            if (nestedValue.size() > VALUE_SIZE) {
              LOGGER.error("Error: The value query param has exceeded the limit");
              return errResponse.put(DESC, "The max number of 'value' should be " + VALUE_SIZE);
            }
          }
        } else {
          LOGGER.error("Error: The value query param has exceeded the limit");
          return errResponse.put(DESC, "The max number of 'value' should be " + VALUE_SIZE);
        }
      } else {
        LOGGER.error("Error: The property query param has exceeded the limit");
        return errResponse.put(DESC, "The max number of 'property' should be " + PROPERTY_SIZE);
      }
    }

    /* Validating ResponseFilter limits */
    if (searchType.contains(RESPONSE_FILTER)) {
      if (requestBody.getJsonArray(FILTER, new JsonArray()).size() > FILTER_VALUE_SIZE) {
        LOGGER.error("Error: The filter in query param has exceeded the limit");
        return errResponse.put(DESC, "The max number of 'filter' should be " + FILTER_VALUE_SIZE);
      }
    }

    /* Validating length of instance header */
    if (requestBody.containsKey(INSTANCE)) {
      String instance = requestBody.getString(INSTANCE, "");
      if (instance != null && instance.length() > ID_SIZE) {
        LOGGER.error("Error: The instance length has exceeded the limit");
        return errResponse.put(DESC, "The max length of 'instance' should be " + ID_SIZE);
      }
    }

    /* Validating length of limit param */
    if (requestBody.containsKey(LIMIT)) {
      Integer limit = requestBody.getInteger(LIMIT, 0);
      if (limit <= 0 || limit > FILTER_PAGINATION_SIZE) {
        LOGGER.error("Error: The limit query param has exceeded the limit");
        return errResponse.put(DESC, "The limit should be between 1 to " + FILTER_PAGINATION_SIZE);
      }
    }

    /* Validating length of offset param */
    if (requestBody.containsKey(OFFSET)) {
      Integer offset = requestBody.getInteger(OFFSET, 0);
      if (offset < 0 || offset > OFFSET_PAGINATION_SIZE) {
        return errResponse.put(DESC, "The offset should be between 0 to " + OFFSET_PAGINATION_SIZE);
      }
    }

    return new JsonObject().put(STATUS, SUCCESS);
  }
}
