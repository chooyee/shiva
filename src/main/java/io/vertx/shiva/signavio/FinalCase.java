package io.vertx.shiva.signavio;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.handler.AuthHandler;

import java.util.List;
import java.util.ArrayList;

public class FinalCase extends InitCase{

    public FinalCase(MongoClient mongo)
    {
        super(mongo);
    }

    /**
     * Test function
     * @param mongo
     * @param caseId
     */
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

    /**
     * Package case to json object to pass to DBOS
     * @param caseId
     * @param finalHandler
     */
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
                        // JsonArray participants = finalCase.result();
                        // int lastEventInt = participants.size()-1;
                        // JsonArray variables = participants.getJsonObject(lastEventInt).getJsonArray("variables");
                        // variables.
                        finObj.put("status", finalCase.result().getString("status"));
                        finObj.put("participants", finalCase.result().getJsonArray("events"));

                        /**
                         * Check for sub process, if yes perform recursive call
                         */
                        if (caseObj.containsKey("controlTasks"))
                        {
                            JsonArray subTasks = new JsonArray();
                            List<Future> futures = new ArrayList<>();
                            caseObj.getJsonArray("controlTasks").forEach(c ->{
                                Future uFuture = Future.future();
                                futures.add(uFuture);
                                JsonObject task = (JsonObject)c;
                                String calledCaseId = new JsonObject(task.getValue("calledCaseId").toString()).getString("$oid");
                                /**
                                 * Recursive call
                                 */
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

                            /**
                             * Wait for all async task complete
                             */
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

    /**
     * Put email address to action
     * @param caseObj
     * @param finalHandler
     */
    public void PrepareFinalCase(JsonObject caseObj, Handler<AsyncResult<JsonObject>> finalHandler){
        JsonArray newEvents =  new JsonArray();
        List<Future> futures = new ArrayList<>();

        caseObj.getJsonArray("participants").forEach(p ->{
            JsonObject participant = (JsonObject)p;
            participant.getJsonArray("events").forEach(e->{  
                JsonObject event = (JsonObject)e;
                String userId = new JsonObject(event.getValue("userId").toString()).getString("$oid");
                Future uFuture = Future.future();
                futures.add(uFuture);

                //Get User Info
                UserHelper.getUserInfoByObjID(mongo, userId, ar -> {
                //mongo.findOne("users", new JsonObject().put("_id", new JsonObject().put("$oid", userId)), null, ar -> {
                    if (ar.succeeded()) {               
                        event.put("email", ar.result().getString("emailAddressLower"));
                        
                        /**
                         * Get forms variables from Tasks
                         */
                        if (event.containsKey("taskId"))
                        {
                            getTask(event, taskHandler->{
                                if (taskHandler.succeeded())
                                {
                                    newEvents.add(taskHandler.result());
                                    uFuture.complete();
                                }
                                else{
                                    uFuture.fail(taskHandler.cause());
                                }
                            });
                         
                        }
                        else{
                            newEvents.add(event);
                            uFuture.complete();
                        }
                      
                    }
                    else{
                        uFuture.fail(ar.cause());
                    }
                });

                
            });
        });

        CompositeFuture.all(futures)
        .setHandler(ar -> {
            if (ar.succeeded()) {
                //get Last Participant's variables
                int lastEventInt = newEvents.size()-1;
                JsonObject lastEvent = newEvents.getJsonObject(lastEventInt);
                String lastStatus = "";
                //System.err.println(Json.encodePrettily(lastEvent) );
                if (lastEvent.containsKey("variables"))
                {
                    JsonArray newVariables = addValueName(lastEvent);
                   
                    //=================================================================================
                    //Get last approval status
                    //=================================================================================
                    JsonObject lastVariable = newVariables.getJsonObject(newVariables.size()-1);
                    if (lastVariable.containsKey("value_name"))
                        lastStatus = lastVariable.getString("value_name");
                    //=================================================================================
                    //End Get last approval status
                    //=================================================================================

                    lastEvent.put("variables", newVariables);
                    newEvents.remove(lastEventInt);
                    newEvents.add(lastEvent);
                }
                
                JsonObject result = new JsonObject()
                .put("status", lastStatus)
                .put("events", newEvents);
                finalHandler.handle(Future.succeededFuture(result));
                
            } else {
                //complete.fail(ar.cause());
                finalHandler.handle(Future.failedFuture(ar.cause())); 
            }
        });
               
        
    }

   private void getTask(JsonObject event, Handler<AsyncResult<JsonObject>> taskHandler)
   {
    String taskId = new JsonObject(event.getValue("taskId").toString()).getString("$oid");
                        
    JsonObject query = new JsonObject()
    .put("_id", new JsonObject().put("$oid", taskId))
    .put("form", new JsonObject().put("$exists",true));
    mongo.findOne("tasks", query, null, tar -> {
        if (tar.succeeded()) {
            if (tar.result() != null)
            {
                event.put("variables", tar.result().getJsonObject("form").getJsonArray("fields")); 
            }
            taskHandler.handle(Future.succeededFuture(event));
        }
        else{
            taskHandler.handle(Future.failedFuture(tar.cause()));
        }
    });
   }

   private JsonArray addValueName(JsonObject lastEvent)
   {
    JsonArray newVariables = new JsonArray();
    lastEvent.getJsonArray("variables").forEach(v->{
        JsonObject variable = (JsonObject)v;
        if (variable.getJsonObject("type").getString("name").equals("choice"))
        {
            String variableValue = variable.getString("value");
            variable.getJsonObject("type").getJsonArray("options").forEach(opt->{
                JsonObject option = (JsonObject)opt;
                if (option.getString("id").equals(variableValue))
                {
                    variable.put("value_name", option.getString("name"));
                }
            });
        }
        newVariables.add(variable);
    });

    return newVariables;

   }
}