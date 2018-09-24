package io.vertx.shiva;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

import java.util.List;

import org.apache.cxf.jaxrs.client.WebClient;


public class SatiVerticle extends AbstractVerticle {

    public static final String COLLECTION = "signavio";
    private MongoClient mongo;

    /**
    * @param fut the future
    */
    @Override
    public void start(Future<Void> fut) {
        //System.err.println(Json.encodePrettily(config()));
        mongo = MongoClient.createShared(vertx, config());
        long timerID = vertx.setPeriodic(1000, id -> {
            initShiva();
            //System.out.println("And every second this is printed");
        });
          
        System.out.println("First this is printed");
    }

    @Override
    public void stop() {
       
    }

    public void initShiva()
    {
        JsonObject query = new JsonObject()             
        .put("complete", false);
        mongo.find("abmb_tracker", query, ar -> {
            if (ar.succeeded()) {
                //System.err.println(Json.encodePrettily(ar.result()));
                if (ar.result().size()>0)
                    for (JsonObject cases : ar.result()) {
                        String url = "http://127.0.0.1:8484/api/v1/task/assign/" + cases.getString("caseid");
                        //System.out.println(url);
                        WebClient client =  WebClient.create(url);
                        //int status = client.get().getStatus();
                        //System.out.println(status);
                    }
              
            }
            else{
                System.err.println("Sati query error! + " + Json.encodePrettily(ar.result()));
            }
        });

        query = new JsonObject()   
        .put("complete", true)
        .put("closed", new JsonObject().put("$exists",false));
        mongo.find("abmb_tracker", query, ar -> {
            if (ar.succeeded()) {
                //System.err.println(Json.encodePrettily(ar.result()));
                if (ar.result().size()>0)
                    for (JsonObject cases : ar.result()) {
                        String url = "http://127.0.0.1:8484/api/v1/case/feedback/" + cases.getString("caseid");
                        //System.out.println(url);
                        WebClient client =  WebClient.create(url);
                        // int status = client.get().getStatus();
                        // System.out.println(url + status);
                    }
              
            }
            else{
                System.err.println("Sati query error! + " + Json.encodePrettily(ar.result()));
            }
        });
         
    }
  

}
