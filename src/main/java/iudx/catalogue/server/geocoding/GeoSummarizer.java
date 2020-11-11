package iudx.catalogue.server.geocoding;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.StringBuilder;

import static iudx.catalogue.server.Constants.*;

public class GeoSummarizer {
    private static GeocodingService geoService;
    private static final Logger LOGGER = LogManager.getLogger(GeoSummarizer.class);
    
    public static String summarize(JsonObject doc) {

        StringBuilder sb = new StringBuilder(); 

        /* Reverse Geocoding information */
    if(doc.containsKey("geometry")) {
        JsonObject geometry = doc.getJsonObject("geometry");
        JsonArray pos = geometry.getJsonArray("coordinates");
        String lat = pos.getString(0);
        String lon = pos.getString(1);
        LOGGER.info(lon);
        geoService.reverseGeocoder(lat, lon, reply -> {
            if(reply.succeeded()) {
            // unwrap the result
            sb.append(reply.result().encode());
            }
            else {
            LOGGER.info("Failed to find location");            
            }
        });
      }
  
      /* Geocoding information*/
  
      if(doc.containsKey("location")) {
        JsonObject location = doc.getJsonObject("location");
        String address = location.getString("address");
        geoService.geocoder(address, reply -> {
          if(reply.succeeded()) {
            sb.append(reply);
          }
          else {
            LOGGER.info("Failed to find coordinates");
          }
          });
        }
        return sb.toString();
    }
}