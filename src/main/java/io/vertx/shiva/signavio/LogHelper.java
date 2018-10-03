package io.vertx.shiva.signavio;

import java.util.Date;
import java.text.SimpleDateFormat;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class LogHelper extends Base{

   
    public LogHelper(MongoClient mongo)
    {
        super(mongo);
    }

    public void log(JsonObject log)
    {
        if (log.getString("Type").equals(Base.LogTypeHelper.Error.logType()))
        {
            Logger logger = LoggerFactory.getLogger(new io.vertx.shiva.ShivaVerticle().getClass());
            logger.error(Json.encodePrettily(log));
        }

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+00:00");                          
        String isoDate = df.format(new Date());
        //System.err.println(isoDate);
        log.put("date", new JsonObject().put("$date", isoDate));
        mongo.insert(CollectionHelper.ASSIGN_FAILED.collection(), log, insertar -> {
           
        });

    }
    
}