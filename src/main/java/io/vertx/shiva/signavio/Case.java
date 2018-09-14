package io.vertx.shiva.signavio;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.mongo.MongoClient;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;

public class Case 
{
    /**
     * Inititalize new workflow
     * @param jsonObj
     * @param token
     * @param aHandler
     */
    public static void getCase(MongoClient mongo, String caseID,Handler<AsyncResult<JsonObject>> aHandler) 
    {
        mongo.findOne("cases", new JsonObject().put("_id", new JsonObject().put("$oid",caseID)), null, ar -> {
            if (ar.succeeded()) {
                aHandler.handle(Future.succeededFuture(ar.result())); 
            }
            else{
                aHandler.handle(Future.failedFuture(Json.encodePrettily(ar.result()))); 
            }
          });
        
    }//end getCase

    public static void setTaskAssignee(MongoClient mongo,String caseId, Handler<AsyncResult<JsonObject>> aHandler) 
    {   
        getUnAssignedTask(mongo, caseId, tar->{
            List<JsonObject> unassignedTask = tar.result();
           
            if (unassignedTask.size()>0){
                for (JsonObject task : unassignedTask) {

                    String roleVariableId = task.getString("roleVariableId");
                    String workflowId = new JsonObject(task.getValue("workflowId").toString()).getString("$oid");
                    String taskId = new JsonObject(task.getValue("_id").toString()).getString("$oid");
                   
                    /**
                     * Get task role name and creator branch to derrive Group Name
                     */
                    getUserInfo(mongo, workflowId, roleVariableId, rar->{
                        String roleName = rar.result().getString("roleName");
                        String groupName = rar.result().getString("branch") + "_" + roleName;
                        String token = rar.result().getString("token");
                        //System.err.println(groupName);
                        UserHelper.getGroupInfo(mongo, groupName.toUpperCase(), gar->{
                            if (gar.result() != null)
                            {
                                JsonObject group =  gar.result();
                                String groupID = new JsonObject(group.getValue("_id").toString()).getString("$oid");
                                String apiUrl = new SignavioApiPathHelper().getTasks() + "/" + taskId;
                                /**
                                 * Start Update to Signavio
                                 */
                                JsonObject result = new JsonObject(WebClientHelper.putJson(apiUrl, token, new JsonObject().put("assigneeGroupId", groupID)));
                                /**
                                 * End update To Signavio
                                 */

                                //Log to DB for success case
                                mongo.insert("abmb_set_assignee_success", result, insertar -> {
                                  
                                });
                            }
                            else
                            {
                                //to handle Group Name not found! Log to DB
                                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+00:00");                          
                                String isoDate = df.format(new Date());
                                //System.err.println(isoDate);
                                JsonObject document = new JsonObject()
                                .put("caseId", new JsonObject(task.getValue("caseId").toString()).getString("$oid"))
                                .put("taskId",taskId)
                                .put("groupName",groupName)
                                .put("date", new JsonObject().put("$date", isoDate));
                                mongo.insert("abmb_set_assignee_failed", document, insertar -> {
                                    // if (insertar.succeeded()) {
                                    //     aHandler.handle(Future.succeededFuture(insertar.result())); 
                                    // } else {
                                    //     aHandler.handle(Future.failedFuture(insertar.result())); 
                                    // }
                                });
                            }
                        });
                    });
                }
                aHandler.handle(Future.succeededFuture(new JsonObject().put("task", unassignedTask.size())));
            }
            else{
                aHandler.handle(Future.succeededFuture(new JsonObject().put("task", 0)));
            }
        });
        
    }//end InitCase
 
    public static void getUserInfo(MongoClient mongo, String workflowID, String roleID, Handler<AsyncResult<JsonObject>> aHandler)
    {
        getWorkflow(mongo, workflowID, ar->{
            if (ar.succeeded())
            {
                String roleName = "";
                JsonObject workflow = ar.result();
                JsonArray variables = workflow.getJsonArray("variables");
                for (int i =0; i < variables.size();i++) {
                    JsonObject var = variables.getJsonObject(i);
                    if (var.getString("id").equals(roleID))
                    {
                        roleName = var.getString("name");
                    }
                }
                final String fRoleName = roleName;
                UserHelper.getUserInfoByObjID(mongo, workflow.getString("creatorId"), userAr->{
                    JsonObject result = new JsonObject()
                    .put("roleName", fRoleName)
                    .put("branch", userAr.result().getString("branch"))
                    .put("token", userAr.result().getString("token"));
                    aHandler.handle(Future.succeededFuture(result));
                });
                
            }
            else{aHandler.handle(Future.failedFuture(Json.encodePrettily(ar.result())));}
        });

    }

    public static void getWorkflow(MongoClient mongo, String workflowID, Handler<AsyncResult<JsonObject>> aHandler) 
    {
        JsonObject query = new JsonObject().put("_id", new JsonObject().put("$oid",workflowID));
        mongo.findOne("workflows", query, null, ar -> {
            if (ar.succeeded()) {
                aHandler.handle(Future.succeededFuture(ar.result())); 
            }
            else{
                aHandler.handle(Future.failedFuture(Json.encodePrettily(ar.result()))); 
            }
          });
        
    }//end getCase

    public static void getUnAssignedTask(MongoClient mongo,String caseId, Handler<AsyncResult<List<JsonObject>>> aHandler) 
    {
        getCase(mongo, caseId, car->{
            if (car.succeeded()) {
                if (car.result().containsKey("closed"))
                {
                    updateTracker(mongo, caseId, res->{});
                    aHandler.handle(Future.succeededFuture(new ArrayList<JsonObject>())); 
                }
                else{
                    JsonObject query = new JsonObject()
                    .put("caseId", new JsonObject().put("$oid", caseId))
                    .put("assigneeGroupId", new JsonObject().put("$exists",false))
                    .put("assigneeId", new JsonObject().put("$exists",false))
                    .put("completed", new JsonObject().put("$exists",false));
                    mongo.find("tasks", query, ar -> {
                        if (ar.succeeded()) {
                            aHandler.handle(Future.succeededFuture(ar.result())); 
                        }
                        else{
                            aHandler.handle(Future.failedFuture(Json.encodePrettily(ar.result()))); 
                        }
                      });
                }
            }
            else{
                aHandler.handle(Future.failedFuture(Json.encodePrettily(car.result()))); 
            }
        });
       
        
    }//end getUnAssignedTask

    private static void updateTracker(MongoClient mongo, String caseId, Handler<AsyncResult<String>> aHandler ){
        JsonObject query = new JsonObject()
        .put("caseid", caseId);
      
        JsonObject update = new JsonObject().put("$set", new JsonObject()
        .put("complete", true));

        mongo.updateCollection("abmb_tracker", query, update, res -> {
            //System.err.println(Json.encodePrettily(res.result()));
            aHandler.handle(Future.succeededFuture("Update sucessfull!")); 
        });
    }
}