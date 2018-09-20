package io.vertx.shiva.signavio;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.mongo.MongoClient;

import java.util.List;
import java.util.ArrayList;

public class FinalCase extends InitCase{

    public FinalCase(MongoClient mongo)
    {
        super(mongo);
    }

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

    public void finale(String caseId, Handler<AsyncResult<JsonObject>> finalHandler)
    {
        JsonObject finObj = new JsonObject();
        //Parent Case
        getCase(caseId, caseHandler ->{

            if (caseHandler.succeeded()){
                
                JsonObject caseObj = caseHandler.result();
                finObj.put("id", caseId);
                finObj.put("name", caseObj.getString("name"));
                finObj.put("createTime", caseObj.getJsonObject("createTime").getString("$date"));
                finObj.put("closeTime", caseObj.getJsonObject("closeTime").getString("$date"));
                finObj.put("closed", caseObj.getBoolean("closed"));
               
                PrepareFinalCase(caseObj, finalCase->{
                    if (finalCase.succeeded())
                    {
                        finObj.put("participants", finalCase.result());
                        if (caseObj.containsKey("controlTasks"))
                        {
                            JsonArray subTasks = new JsonArray();
                            List<Future> futures = new ArrayList<>();
                            caseObj.getJsonArray("controlTasks").forEach(c ->{
                                Future uFuture = Future.future();
                                futures.add(uFuture);
                                JsonObject task = (JsonObject)c;
                                String calledCaseId = new JsonObject(task.getValue("calledCaseId").toString()).getString("$oid");
                                finale(calledCaseId, fh->{
                                    if (fh.succeeded()){
                                        subTasks.add(fh.result());
                                        uFuture.complete();
                                    }
                                    else{
                                        uFuture.complete();
                                    }
                                });
                            });

                            CompositeFuture.all(futures)
                            .setHandler(ar -> {
                                if (ar.succeeded()) {
                                    //complete.complete();
                                    //System.err.println(newEvents);
                                    finObj.put("subTasks", subTasks);
                                    finalHandler.handle(Future.succeededFuture(finObj));
                                    
                                } else {
                                    //complete.fail(ar.cause());
                                    finalHandler.handle(Future.failedFuture(ar.cause())); 
                                }
                            });

                        }
                        else{
                            finalHandler.handle(Future.succeededFuture(finObj));
                        }
                    }
                   
                });
            }
            else{
                finalHandler.handle(Future.failedFuture(caseHandler.cause())); 
            }
        });

    }

    public void PrepareFinalCase(JsonObject caseObj, Handler<AsyncResult<JsonArray>> finalHandler){
        JsonArray newEvents =  new JsonArray();
        List<Future> futures = new ArrayList<>();

        caseObj.getJsonArray("participants").forEach(p ->{
            //System.err.println(Json.encodePrettily(p));
            JsonObject participant = (JsonObject)p;
            participant.getJsonArray("events").forEach(e->{                
                JsonObject event = (JsonObject)e;
                //System.err.println(Json.encodePrettily(event));
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
                        uFuture.fail("Failed to find user:" +userId);
                    }
                });
                
            });
        });

        CompositeFuture.all(futures)
        .setHandler(ar -> {
            if (ar.succeeded()) {
                //complete.complete();
                //System.err.println(newEvents);
                finalHandler.handle(Future.succeededFuture(newEvents));
                
            } else {
                //complete.fail(ar.cause());
                finalHandler.handle(Future.failedFuture(ar.cause())); 
            }
        });
               
        
    }

   
}