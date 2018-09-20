package io.vertx.shiva.signavio;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.AsyncResult;

import io.vertx.core.Handler;

import io.vertx.rxjava.core.CompositeFuture;
import io.vertx.rxjava.core.Future;


import rx.Observable;
import rx.Subscription;

import io.vertx.ext.mongo.MongoClient;

import java.util.List;
import java.util.ArrayList;

public class rxTest {
    public static void compositeFutureTest(MongoClient mongo, String caseId){
        JsonArray newUsers =  new JsonArray();
        JsonArray users = new JsonArray()
        .add(new JsonObject().put("id","5b8529248edce408d81adc63"))
        .add(new JsonObject().put("id","5b852af48edce408d81adc88"))
        .add(new JsonObject().put("id","5b8faccb45816729ec5085f1"));
        
        List<Future> futures = new ArrayList<>();
        users.forEach(u->{
            JsonObject user = (JsonObject)u;
            Future uFuture = Future.future();
            futures.add(uFuture);
            mongo.findOne("users", new JsonObject().put("_id", new JsonObject().put("$oid",user.getString("id"))), null, ar -> {
                if (ar.succeeded()) {               
                    user.put("email", ar.result().getString("emailAddressLower"));
                    System.err.println(Json.encodePrettily(user)); 
                    newUsers.add(user);
                    uFuture.complete();
                }
                else{
                    uFuture.fail("Faile to find user:" + user.getString("id"));
                }
            });
        });

        CompositeFuture.all(futures)
        .setHandler(ar -> {
            if (ar.succeeded()) {
                //complete.complete();
                System.err.println(newUsers);
            } else {
                //complete.fail(ar.cause());
            }
        });
    }

     public static void updateDBOS(MongoClient mongo, String caseId){
        JsonArray newEvents =  new JsonArray();
        List<Future> futures = new ArrayList<>();
        
        Case.getCase(mongo, caseId, aHandler ->{
            if (aHandler.succeeded())
            {
                JsonObject caseObj = aHandler.result();
              
                caseObj.getJsonArray("participants").forEach(p ->{
                    //System.err.println(Json.encodePrettily(p));
                    JsonObject participant = (JsonObject)p;
                    participant.getJsonArray("events").forEach(e->{
                        
                        JsonObject event = (JsonObject)e;
                        System.err.println(Json.encodePrettily(event));
                        String userId = new JsonObject(event.getValue("userId").toString()).getString("$oid");
                        Future uFuture = Future.future();
                        futures.add(uFuture);
                        mongo.findOne("users", new JsonObject().put("_id", new JsonObject().put("$oid", userId)), null, ar -> {
                            if (ar.succeeded()) {               
                                event.put("email", ar.result().getString("emailAddressLower"));
                               
                                newEvents.add(event);
                                uFuture.complete();
                            }
                            else{
                                uFuture.fail("Faile to find user:" +userId);
                            }
                        });
                       
                    });
                });

                CompositeFuture.all(futures)
                .setHandler(ar -> {
                    if (ar.succeeded()) {
                        //complete.complete();
                        System.err.println(newEvents);
                       
                    } else {
                        //complete.fail(ar.cause());
                    }
                });
               
            }
            else
            {
               
            }
           
        });
        
    }
}