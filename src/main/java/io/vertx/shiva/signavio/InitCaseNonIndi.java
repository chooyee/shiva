package io.vertx.shiva.signavio;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import java.util.Base64;


public class InitCaseNonIndi extends Case
{
    public InitCaseNonIndi(MongoClient mongo)
    {
        super(mongo);
    }

    /**
     * Inititalize new workflow
     * @param jsonObj
     * @param token
     * @param aHandler
     */
    public void init(JsonObject jsonObj, String token, Handler<AsyncResult<String>> aHandler) 
    {
        JsonArray attachmentIdJArray = new JsonArray();
        JsonArray qnaJArray = new JsonArray();
        //final String id = jsonObj.getString("id");
        final String refNo = jsonObj.getString("refNo");
        final String compRegNo = jsonObj.getString("companyRegNo");
        final String compName = jsonObj.getString("companyName");
        final String creatorLanName = jsonObj.getString("creatorLanName");
        final String creatorEmail = jsonObj.getString("creatorEmail");
        final String creatorLanId = jsonObj.getString("creatorLanId");
        final String requestForApprovalName = jsonObj.getString("requestForApprovalName");
        final String requestForApprovalEmail = jsonObj.containsKey("requestForApprovalEmail")?jsonObj.getString("requestForApprovalEmail"):"";
        final String requestForApprovalLanId = jsonObj.getString("requestForApprovalLanId");
        final String concurrenceLanName = jsonObj.getString("concurrenceLanName");
        final String concurrenceEmail = jsonObj.getString("concurrenceEmail");
        final String approverLanName = jsonObj.getString("approverLanName");
        final String approverEmail = jsonObj.getString("approverEmail");
        
       
        if (jsonObj.containsKey("attachment"))
        {
            jsonObj.getJsonArray("attachment").forEach(a->{
                byte[] imageByte;
                JsonObject fileJson;
                JsonObject attach = (JsonObject) a;
                String fileName = attach.getString("fileName");
                String fileContent = attach.getString("fileContent");
    
                if (fileContent.length()>0)
                {
                    imageByte = Base64.getDecoder().decode(fileContent);
                    fileJson = new JsonObject(WebClientHelper.uploadToSignavio(imageByte,fileName, token));
                    String attachmentId = fileJson.getString("id");
                    attachmentIdJArray.add(attachmentId);
                }
            });
        }

        if (jsonObj.containsKey("eddQuestions"))
        {
            jsonObj.getJsonArray("eddQuestions").forEach(f->{
                JsonObject qna = (JsonObject) f;
                qna.put("type", "qna");
                qnaJArray.add(qna.toString()) ;
            });
        }
        
     
        jsonForSignavio(
        "NONINDI_AML",
        refNo,
        compRegNo,
        compName,
        creatorLanName,
        creatorEmail,
        creatorLanId,
        requestForApprovalName,
        requestForApprovalEmail,
        requestForApprovalLanId,
        concurrenceLanName,
        concurrenceEmail,
        approverLanName,
        approverEmail,
        attachmentIdJArray,
        qnaJArray,
        ar->{
            if (ar.succeeded())
            {
               
                aHandler.handle(Future.succeededFuture(WebClientHelper.postJson(new SignavioApiPathHelper().getCases(), token, ar.result()))); 
            }
            else{
                aHandler.handle(Future.failedFuture(ar.cause()));
            }
        });
      
       
    }//end InitCase
   

  
    private void jsonForSignavio(
    String eddCode, 
    String refNo,
    String compRegNo,
    String compName,
    String creatorLanName,
    String creatorEmail,
    String creatorLanId,
    String requestForApprovalName,
    String requestForApprovalEmail,
    String requestForApprovalLanId,
    String concurrenceLanName,
    String concurrenceEmail,
    String approverLanName,
    String approverEmail,
    JsonArray attachmentIdJArray,
    JsonArray qnaJArray,
    Handler<AsyncResult<JsonObject>> aHandler)
    {
      
        mongo.findOne(CollectionHelper.WF_TRIGGER.collection(), new JsonObject().put("name", eddCode).put("version", "1.0"), null, ar -> {
            if (ar.succeeded()) {
                JsonObject t = new JsonObject().put("triggerInstance", ar.result().getJsonObject("triggerInstance"));

                JsonArray fields = t.getJsonObject("triggerInstance").getJsonObject("data").getJsonObject("formInstance").getJsonObject("value").getJsonArray("fields");
                fields.forEach(f->{
                    JsonObject field = (JsonObject) f;
                  
                    if (field.getString("name").toLowerCase().equals("reference number"))
                    {
                        field.put("value", refNo);
                    }
                    else if (field.getString("name").toLowerCase().equals("company registration number"))
                    {
                        field.put("value", compRegNo);
                    }
                    else if (field.getString("name").toLowerCase().equals("company name"))
                    {
                        field.put("value", compName);
                    }
                    else if (field.getString("name").toLowerCase().equals("creator lan name"))
                    {
                        field.put("value", creatorLanName);
                    }
                    else if (field.getString("name").toLowerCase().equals("creator email"))
                    {
                        field.put("value", creatorEmail);
                    }
                    else if (field.getString("name").toLowerCase().equals("creator lan id"))
                    {
                        field.put("value", creatorLanId);
                    }
                    else if (field.getString("name").toLowerCase().equals("request for approval name"))
                    {
                        field.put("value", requestForApprovalName);
                    }
                    else if (field.getString("name").toLowerCase().equals("request for approval email"))
                    {
                        field.put("value", requestForApprovalEmail);
                    }
                    else if (field.getString("name").toLowerCase().equals("request for approval lan id"))
                    {
                        field.put("value", requestForApprovalLanId);
                    }
                    else if (field.getString("name").toLowerCase().equals("concurrence lan name"))
                    {
                        field.put("value", concurrenceLanName);
                    }
                    else if (field.getString("name").toLowerCase().equals("concurrence email"))
                    {
                        field.put("value", concurrenceEmail);
                    }
                    else if (field.getString("name").toLowerCase().equals("approver lan name"))
                    {
                        field.put("value", approverLanName);
                    }
                    else if (field.getString("name").toLowerCase().equals("approver email"))
                    {
                        field.put("value", approverEmail);
                    }
                    else if (field.getString("name").toLowerCase().equals("attachment"))
                    {
                        field.put("value", attachmentIdJArray);
                    }
                    else if (field.getString("name").toLowerCase().equals("questions and answers"))
                    {
                        field.put("value", qnaJArray);
                    }
                });
                System.err.println(t);
                aHandler.handle(Future.succeededFuture(t));
            }
            else{
                aHandler.handle(Future.failedFuture(ar.cause()));
            }
            
        });
       
    }
  
}