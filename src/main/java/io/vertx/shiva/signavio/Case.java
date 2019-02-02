package io.vertx.shiva.signavio;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.mongo.MongoClient;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;

public class Case extends Base
{

    public Case(MongoClient mongo)
    {
        super(mongo);
    }

    /**
     * Inititalize new workflow
     * @param jsonObj
     * @param token
     * @param aHandler
     */
    public void getCase(String caseID,Handler<AsyncResult<JsonObject>> aHandler) 
    {
        mongo.findOne(CollectionHelper.CASES.collection(), new JsonObject().put("_id", new JsonObject().put("$oid",caseID)), null, ar -> {
            if (ar.succeeded()) {               
                aHandler.handle(Future.succeededFuture(ar.result())); 
            }
            else{
                aHandler.handle(Future.failedFuture("No case found with id : " + caseID)); 
            }
          });
        
    }//end getCase

    public void setTaskAssignee(String caseId, Handler<AsyncResult<JsonObject>> aHandler) 
    {   
        getUnAssignedTask(caseId, tar->{
            if (tar.succeeded()){
                List<JsonObject> unassignedTask = tar.result();
            
                if (unassignedTask.size()>0){
                    for (JsonObject task : unassignedTask) {

                        String roleVariableId = task.getString("roleVariableId");
                        String workflowId = new JsonObject(task.getValue("workflowId").toString()).getString("$oid");
                        String taskId = new JsonObject(task.getValue("_id").toString()).getString("$oid");
                    
                        /**
                         * Get task role name and creator branch to derrive Group Name
                         */
                        getAssigneeInfo(workflowId, caseId, roleVariableId, rar->{
                            String roleName = rar.result().getString("roleName");
                            String caseCreator = rar.result().getString("caseCreator");
                            String token = rar.result().getString("token");
                            String branchName =  rar.result().getString("branch");

                            if (roleName.toLowerCase().equals("case_creator"))
                            {
                              
                                UserHelper.getUserInfoByEmail(mongo, caseCreator, uar->{
                                    JsonObject user =  uar.result();
                                   
                                    String apiUrl = new SignavioApiPathHelper().getTasks() + "/" + taskId;
                                     /**
                                     * Start Update to Signavio
                                     */
                                    JsonObject result = new JsonObject(WebClientHelper.putJson(apiUrl, token, new JsonObject().put("assigneeId",  new JsonObject(user.getValue("_id").toString()).getString("$oid"))));
                                    /**
                                     * End update To Signavio
                                     */

                                    //Log to DB for success case
                                    mongo.insert(CollectionHelper.ASSIGN_SUCCESS.collection(), result, mgoHandler->{});
                                });
                               
                               

                            }
                            else{
                                String groupName = branchName + "_" + roleName;
                               
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
                                        mongo.insert(CollectionHelper.ASSIGN_SUCCESS.collection(), result, insertar -> {
                                        
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
                                        mongo.insert(CollectionHelper.ASSIGN_FAILED.collection(), document, insertar -> {
                                            // if (insertar.succeeded()) {
                                            //     aHandler.handle(Future.succeededFuture(insertar.result())); 
                                            // } else {
                                            //     aHandler.handle(Future.failedFuture(insertar.result())); 
                                            // }
                                        });
                                    }
                                });
                            }
                        });
                    }
                    aHandler.handle(Future.succeededFuture(new JsonObject().put("task", unassignedTask.size())));
                }
                else{
                    aHandler.handle(Future.succeededFuture(new JsonObject().put("task", 0)));
                }
            }
            else{
                aHandler.handle(Future.succeededFuture(new JsonObject().put("task", 0)));
            }
        });
        
    }//end InitCase
 
    public void getAssigneeInfo(String workflowID, String caseID, String roleID, Handler<AsyncResult<JsonObject>> aHandler)
    {
        //System.err.println(workflowID);
        getWorkflow(workflowID, ar->{
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

                
                // UserHelper.getUserInfoByObjID(mongo, workflow.getString("creatorId"), userAr->{
                //     JsonObject result = new JsonObject()
                //     .put("roleName", fRoleName)
                //     .put("branch", userAr.result().getString("branch"))
                //     .put("token", userAr.result().getString("token"));
                //     aHandler.handle(Future.succeededFuture(result));
                // });

                 /**
                 * Change to
                 * 1. get admin token
                 * 2. get branch name from branch code 
                 */
              
                UserHelper.getAdminToken(mongo, userAr->{
                    if (userAr.succeeded()){
                        final String token = userAr.result();
                       
                        //Find branch code via caseid in abmb_tracker
                        JsonObject query = new JsonObject().put("caseid", caseID );
                        mongo.findOne(CollectionHelper.TRACKER.collection(), query, null, tar -> {
                            if (tar.succeeded()) {
                                String branchCode = tar.result().getString("branch");
                                String caseCreator = tar.result().getString("email");
                                //Find branch name in abmb_branch
                                JsonObject bquery = new JsonObject().put("code", branchCode );
                                mongo.findOne(CollectionHelper.BRANCH.collection(), bquery, null, bar -> {
                                    if (bar.succeeded()) {
                                        final String branchName = bar.result().getString("name");
                                        JsonObject result = new JsonObject()
                                        .put("roleName", fRoleName)
                                        .put("branch", branchName)
                                        .put("token", token)
                                        .put("caseCreator", caseCreator);
                                        aHandler.handle(Future.succeededFuture(result));
                                    }
                                    else{
                                        aHandler.handle(Future.failedFuture(bar.cause()));
                                    }
                                    
                                });
                                
                            }
                            else{
                                aHandler.handle(Future.failedFuture(tar.cause()));
                            }
                            
                        });
                    }
                    else{
                        aHandler.handle(Future.failedFuture(userAr.cause()));
                    }
                });

            }
            else{aHandler.handle(Future.failedFuture(Json.encodePrettily(ar.result())));}
        });

    }

    public void getWorkflow(String workflowID, Handler<AsyncResult<JsonObject>> aHandler) 
    {
        JsonObject query = new JsonObject().put("_id", new JsonObject().put("$oid",workflowID));
        //System.err.println(query);
        mongo.findOne(CollectionHelper.WORKFLOWS.collection(), query, null, ar -> {
            if (ar.succeeded()) {
                //System.err.println(ar.result());
                aHandler.handle(Future.succeededFuture(ar.result())); 
            }
            else{
                aHandler.handle(Future.failedFuture(Json.encodePrettily(ar.result()))); 
            }
          });
        
    }//end getCase

    public void getUnAssignedTask(String caseId, Handler<AsyncResult<List<JsonObject>>> aHandler) 
    {
        getCase(caseId, car->{
            if (car.succeeded()) {
                if (car.result()!=null)
                    if (car.result().containsKey("closed"))
                    {
                        updateTracker(caseId, res->{});
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
                                aHandler.handle(Future.failedFuture("No task found with case id : " + caseId)); 
                            }
                        });
                    }
                else{
                    aHandler.handle(Future.failedFuture("No case found with case id : " + caseId)); 
                }
            }
            else{
                aHandler.handle(Future.failedFuture("No case found with case id : " + caseId)); 
            }
        });
       
        
    }//end getUnAssignedTask

    private void updateTracker(String caseId, Handler<AsyncResult<String>> aHandler ){
        JsonObject query = new JsonObject()
        .put("caseid", caseId);
      
        JsonObject update = new JsonObject().put("$set", new JsonObject()
        .put("complete", true));

        mongo.updateCollection(CollectionHelper.TRACKER.collection(), query, update, res -> {
            //System.err.println(Json.encodePrettily(res.result()));
            aHandler.handle(Future.succeededFuture("Update sucessfull!")); 
        });
    }

    public void auditLog(JsonObject incomingObject, Handler<AsyncResult<String>> aHandler)
    {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+00:00");                          
        String isoDate = df.format(new Date());
        incomingObject.put("date", new JsonObject().put("$date", isoDate));
        
        mongo.insert(CollectionHelper.INIT_TRACK.collection(), incomingObject, insertar -> {
            if (insertar.succeeded()) {
                aHandler.handle(Future.succeededFuture(insertar.result())); 
            } else {
                aHandler.handle(Future.failedFuture(insertar.cause())); 
            }
        });
    }

    // public void updateWfTracker(String resultStr, JsonObject jsonObj, Handler<AsyncResult<String>> aHandler)
    // {
    //     String email = jsonObj.getString("email");
    //     String branchCode = jsonObj.getString("branchCode");
    //     String userId = jsonObj.getJsonArray("eform").getJsonObject(0).getString("idNo");
    //     String caseId = new JsonObject(resultStr).getString("id");

    //     JsonObject filter = new JsonObject();
    //     filter.put("userId", userId);
    //     filter.put("branchCode", branchCode);
    //     filter.put("userId", userId);

    //     List<JsonObject> eddObjs = getEddObjects(jsonObj);

    //     Json
    // }

    // public void initWfTracker(String resultStr, JsonObject jsonObj, String userid, String branchCode, Handler<AsyncResult<String>> aHandler) 
    // {
    //     System.err.println(userid);
    //     List<JsonObject> eddObjs = getEddObjects(jsonObj);
    //     JsonObject caseObj = new JsonObject(resultStr);
        
    //     mongo.findOne(CollectionHelper.USER_BRANCH.collection(), new JsonObject().put("email", userid), null, ar -> {
    //         if (ar.succeeded()) {
    //             System.err.println(ar.result());
    //             JsonObject document = new JsonObject()
    //             .put("caseid", caseObj.getString("id"))
    //             .put("email", userid)
    //             .put("branch", branchCode)
    //             .put("type", "indi")
    //             .put("edd", eddObjs)
    //             .put("complete", false);
                
    //             mongo.insert(CollectionHelper.TRACKER.collection(), document, insertar -> {
    //                 if (ar.succeeded()) {
    //                     aHandler.handle(Future.succeededFuture(insertar.result())); 
    //                 } else {
    //                     aHandler.handle(Future.failedFuture(document.encodePrettily())); 
    //                 }
    //             });
    //             //MongoDBHelper.insert(mongo, "abmb_tracker", document, aHandler);
    //         }
            
    //       });
        
    // }//end InitCase

 
    public void initWfTracker(String resultStr, JsonObject jsonObj, Handler<AsyncResult<String>> aHandler) 
    {
        String email = jsonObj.getString("email");
        String branchCode = jsonObj.getString("branchCode");
        String userId = jsonObj.getJsonObject("application").getJsonArray("eform").getJsonObject(0).getString("idNo");
        String caseId = new JsonObject(resultStr).getString("id");

        System.err.println(userId);
     
        mongo.findOne(CollectionHelper.USER_BRANCH.collection(), new JsonObject().put("email", email), null, ar -> {
            if (ar.succeeded()) {
                System.err.println(ar.result());
                JsonObject document = new JsonObject()
                .put("caseid", caseId)
                .put("userId", userId)
                .put("email", email)
                .put("branch", branchCode)
                .put("type", "indi")
                .put("complete", false);
                
                mongo.insert(CollectionHelper.TRACKER.collection(), document, insertar -> {
                    if (ar.succeeded()) {
                        aHandler.handle(Future.succeededFuture(insertar.result())); 
                    } else {
                        aHandler.handle(Future.failedFuture(document.encodePrettily())); 
                    }
                });
            }
            
          });
        
    }//end initWfTracker

    /// <summary>
    /// Init Indi Wf Tracker
    /// </summary>
    /// <returns>Void </returns>
    public void initIndiWfMain(JsonObject jsonObj, Handler<AsyncResult<String>> aHandler) 
    {
        String email = jsonObj.getString("email");
        String branchCode = jsonObj.getString("branchCode");
        String userId = jsonObj.getJsonObject("application").getJsonArray("eform").getJsonObject(0).getString("idNo");
        System.err.println(userId);
        
        List<JsonObject> eddObjs = getEddObjects(jsonObj);

        JsonObject document = new JsonObject()
        .put("userId", userId)
        .put("email", email)
        .put("branch", branchCode)
        .put("type", "indi")
        .put("edd", eddObjs)
        .put("complete", false);
        
        mongo.insert(CollectionHelper.INDI_TRACKER.collection(), document, insertar -> {
            if (insertar.succeeded()) {
                
                System.err.println(insertar.result()); 
                aHandler.handle(Future.succeededFuture(insertar.result())); 
            } else {
                aHandler.handle(Future.failedFuture(document.encodePrettily())); 
            }
        });
        
    }//end initWfTracker

    private List<JsonObject> getEddObjects(JsonObject jsonObj)
    {
        List<JsonObject> aml = new ArrayList<JsonObject>();
        jsonObj.getJsonArray("eddCases").forEach(c->{
            JsonObject eddCase = (JsonObject)c;
            eddCase.getJsonObject("eddResults").getJsonObject("result").getJsonArray("details").forEach(e->{                
                 aml.add(((JsonObject)e).put("status", ""));
            });
        });
        return aml;
    }
    //caseRefId is id from Dbos Non-Indi
    public void initNonIndiWfTracker(String resultStr, String caseRefId, Handler<AsyncResult<String>> aHandler) 
    {
        System.err.println(caseRefId);

        JsonObject caseObj = new JsonObject(resultStr);
        
        mongo.findOne(CollectionHelper.USER_BRANCH.collection(), new JsonObject().put("refcaseid", caseRefId), null, ar -> {
            if (ar.succeeded()) {
                System.err.println(ar.result());
                JsonObject document = new JsonObject()
                .put("caseid", caseObj.getString("id"))
                .put("refcaseid", caseRefId)
                .put("type", "nonindi")
                .put("complete", false);
                
                mongo.insert(CollectionHelper.TRACKER.collection(), document, insertar -> {
                    if (ar.succeeded()) {
                        aHandler.handle(Future.succeededFuture(insertar.result())); 
                    } else {
                        aHandler.handle(Future.failedFuture(document.encodePrettily())); 
                    }
                });
                //MongoDBHelper.insert(mongo, "abmb_tracker", document, aHandler);
            }
            
          });
        
    }//end InitCase
}