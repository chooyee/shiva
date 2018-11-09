package io.vertx.shiva.signavio;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

import java.util.List;
import java.util.ArrayList;
import java.util.Base64;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.BufferedWriter;
import java.util.Date;
import java.text.SimpleDateFormat;
// import io.vertx.util.Runner;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class InitCase extends Case
{
    public InitCase(MongoClient mongo)
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
        final String id = jsonObj.getString("id");
        //final String newCaseName = jsonObj.getString("newCaseName");
        String idFrontFileID = "";
        String idBackFileID = "";
        String idCusFileID = "";
        JsonObject fileJson;
        String caseCreator = jsonObj.getString("email");
        JsonArray custList = getCustomerDetails(jsonObj);
        JsonObject product = getProductDetails(jsonObj);
        if (jsonObj.containsKey("efaces"));
        {
            String idfront = "";
            String idback = "";
            JsonArray eface = jsonObj.getJsonArray("efaces");
            
            for (int i = 0 ; i < eface.size(); i++) {
                JsonObject obj = eface.getJsonObject(i);
               
                idfront = obj.getString("idfront");
                idback = obj.getString("idback");
            }
            byte[] imageByte = Base64.getDecoder().decode(idfront);
            fileJson = new JsonObject(WebClientHelper.uploadToSignavio(imageByte,"id_front.png", token));
            idFrontFileID = fileJson.getString("id");

            imageByte = Base64.getDecoder().decode(idback);
            fileJson = new JsonObject(WebClientHelper.uploadToSignavio(imageByte,"id_back.png", token));
            idBackFileID = fileJson.getString("id");

            
            String filePath = genXls(jsonObj.getJsonObject("application").getString("uuid"), custList, product);
          
            try{
                byte[] bFile = Files.readAllBytes(Paths.get(filePath));
                fileJson = new JsonObject(WebClientHelper.uploadToSignavio(bFile,"cus details.xlsx", token));
                idCusFileID = fileJson.getString("id");
            }
            catch(IOException io)
            {
                JsonObject log = new JsonObject().put("Type", Base.LogTypeHelper.Info.logType());
                log.put("origin", Base.getCurrentMethodName());
                log.put("file path", filePath);
                log.put("Error", io.getMessage());
                new LogHelper(mongo).log(log);
            }
           
        }
        caseWhisky(getAMLCode(jsonObj), idFrontFileID, idBackFileID, idCusFileID, caseCreator, custList, ar->{
            if (ar.succeeded())
            {
               
                aHandler.handle(Future.succeededFuture(WebClientHelper.postJson(new SignavioApiPathHelper().getCases(), token, ar.result()))); 
            }
            else{
                aHandler.handle(Future.failedFuture(ar.cause()));
            }
        });
        // caseWhisky(getAMLCode(jsonObj), idFrontFileID, idBackFileID, idCusFileID, ar->{
        //     if (ar.succeeded())
        //     {
        //         Whisky whisky = ar.result();
        //         System.err.println(Json.encodePrettily(whisky));
        //         aHandler.handle(Future.succeededFuture(WebClientHelper.postJson(new SignavioApiPathHelper().getCases(), token, whisky))); 
        //     }
        //     else{
        //         aHandler.handle(Future.failedFuture(ar.cause()));
        //     }
        // });      
        
       
    }//end InitCase
    private List<JsonObject> getAMLCode(JsonObject jsonObj)
    {
        List<JsonObject> aml = new ArrayList<JsonObject>();
        jsonObj.getJsonArray("eddCases").forEach(c->{
            JsonObject eddCase = (JsonObject)c;
            eddCase.getJsonObject("eddResults").getJsonObject("result").getJsonArray("details").forEach(e->{                
                aml.add((JsonObject)e);
            });
        });
        return aml;
    }
    private JsonArray getCustomerDetails(JsonObject jsonObj)
    {
        JsonArray custList = new JsonArray();
        
        if (jsonObj.containsKey("application"));
        {
            JsonObject application = jsonObj.getJsonObject("application");
            if (application.containsKey("eform"))
            {
                application.getJsonArray("eform").forEach(e->{
                    JsonObject cust = new JsonObject();
                    JsonObject eform = (JsonObject)e;
                    if (eform.containsKey("idNo"))
                    {
                        cust.put("idNo", eform.getString("idNo"));
                    }
                    if (eform.containsKey("idTypeCode"))
                    {
                        cust.put("idTypeCode", eform.getString("idTypeCode"));
                    }
                    if (eform.containsKey("contactDetails"))
                    {
                        cust.put("contactDetails", eform.getJsonObject("contactDetails"));
                    }
                    if (eform.containsKey("identityInfo"))
                    {
                        cust.put("identityInfo", eform.getJsonObject("identityInfo"));
                    }
                    if (eform.containsKey("occupationIncome"))
                    {
                        cust.put("occupationIncome", eform.getJsonObject("occupationIncome"));
                    }
                    custList.add(cust);
                });
            }
            // if (application.containsKey("facilityCodes"))
            // {
            //     cust.put("facilityCodes", application.getJsonArray("application"));
            // }
           
            return custList;
        }

    }

    private JsonObject getProductDetails(JsonObject jsonObj)
    {
        JsonObject product = new JsonObject();
        
        if (jsonObj.containsKey("product"));
        {
            product.put("code", jsonObj.getJsonObject("product").getString("code"));
            product.put("name", jsonObj.getJsonObject("product").getString("name"));
            product.put("segmentCode", jsonObj.getJsonObject("product").getString("segmentCode"));
            
            JsonArray facilities = new JsonArray();
            jsonObj.getJsonObject("product").getJsonArray("facilities").forEach(e->{
                JsonObject facility = (JsonObject)e;
                facilities.add(new JsonObject().put("name", facility.getString("name")));

            });
            product.put("facilities", facilities);
            // if (application.containsKey("facilityCodes"))
            // {
            //     cust.put("facilityCodes", application.getJsonArray("application"));
            // }
           
            return product;
        }

    }

    private String genXls(String uuid, JsonArray custList, JsonObject product)
    {
        String XLS_FILE = "c:/temp/"+uuid+".xlsx";
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Sheet 1");

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 14);
        
        CellStyle headerCellStyle = workbook.createCellStyle();
        headerCellStyle.setFont(headerFont);

        for(int i =0; i<custList.size();i++)
        {
            JsonObject cust = custList.getJsonObject(i);
            JsonObject identity = cust.getJsonObject("identityInfo");
            int rowNum = 0;
            Row row = sheet.createRow(rowNum++);
            Cell cell = row.createCell(0);
            cell.setCellValue((String) "Identity");
            cell.setCellStyle(headerCellStyle);
    
            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "ID No");
            cell = row.createCell(1);
            cell.setCellValue((String) cust.getString("idNo") );

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "ID Type Code");
            cell = row.createCell(1);
            cell.setCellValue((String) cust.getString("idTypeCode") );

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "City (Permanent)");
            cell = row.createCell(1);
            cell.setCellValue((String) identity.getString("city") );

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Post Code (Permanent)");
            cell = row.createCell(1);
            cell.setCellValue((String) identity.getString("postCode") );

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "State (Permanent)");
            cell = row.createCell(1);
            cell.setCellValue((String) identity.getString("stateCode") );

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Country (Permanent)");
            cell = row.createCell(1);
            cell.setCellValue((String) identity.getString("country") );

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "D.O.B.");
            cell = row.createCell(1);
            cell.setCellValue((String) identity.getString("dob") );

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Full Name");
            cell = row.createCell(1);
            cell.setCellValue((String) identity.getString("fullName") );

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Gender");
            cell = row.createCell(1);
            cell.setCellValue((String) identity.getString("genderCode") );

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Issuing Country");
            cell = row.createCell(1);
            cell.setCellValue((String) identity.getString("issuingCountry") );

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Nationality");
            cell = row.createCell(1);
            cell.setCellValue((String) identity.getString("nationality") );
          

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Race");
            cell = row.createCell(1);
            cell.setCellValue((String) identity.getString("raceCode") );
           

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Religion");
            cell = row.createCell(1);
            cell.setCellValue((String) identity.getString("religionCode") );
           

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue(" ");
            cell.setCellStyle(headerCellStyle);


            JsonObject contact = cust.getJsonObject("contactDetails");

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Contact Details");
            cell.setCellStyle(headerCellStyle);

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Correspondence Address Same With MYKAD");
            cell = row.createCell(1);
            if (contact.getBoolean("contactAddressSameWithMYKAD"))
            {
                cell.setCellValue("Same as permanent address");
            }
            else{

                /**
                 * TODO: Show customer address
                 */
                cell.setCellValue(contact.getBoolean("contactAddressSameWithMYKAD"));
            }
            

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "City (Correspondence)");
            cell = row.createCell(1);
            cell.setCellValue((String) contact.getString("city") );

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Post Code (Correspondence)");
            cell = row.createCell(1);
            cell.setCellValue((String) contact.getString("postCode") );

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "State (Correspondence)");
            cell = row.createCell(1);
            cell.setCellValue((String) contact.getString("stateCode") );

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Country (Correspondence)");
            cell = row.createCell(1);
            cell.setCellValue((String) contact.getString("country") );

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Email");
            cell = row.createCell(1);
            cell.setCellValue((String) contact.getString("email") );

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Mobile No");
            cell = row.createCell(1);
            cell.setCellValue((String) contact.getString("mobileNo") );
          
            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue(" ");
            cell.setCellStyle(headerCellStyle);

            JsonObject occupation = cust.getJsonObject("occupationIncome");
            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Occupation");
            cell.setCellStyle(headerCellStyle);

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "BNM Counter Party Code");
            cell = row.createCell(1);
            cell.setCellValue((String) occupation.getString("bnmCounterPartyCode") );

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Employment Type Code");
            cell = row.createCell(1);
            cell.setCellValue((String) occupation.getString("employmentTypeCode") );

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Gross Annual Income");
            cell = row.createCell(1);
            cell.setCellValue(occupation.getInteger("grossAnnualIncome"));

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Industry Category");
            cell = row.createCell(1);
            cell.setCellValue((String) occupation.getString("industryCategory") );

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Marital Status Code");
            cell = row.createCell(1);
            cell.setCellValue((String) occupation.getString("maritalStatusCode") );
         
            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Occupation AML Code");
            cell = row.createCell(1);
            cell.setCellValue((String) occupation.getString("occupationAMLCode") );

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Occupation CCRIS Code");
            cell = row.createCell(1);
            cell.setCellValue((String) occupation.getString("occupationCCRISCode") );

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Occupation Category Code");
            cell = row.createCell(1);
            cell.setCellValue((String) occupation.getString("occupationCategoryCode") );
          
            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Source Of Fund");
            cell = row.createCell(1);
            cell.setCellValue((String) occupation.getString("sourceOfFundCode") );

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Source Of Wealth");
            cell = row.createCell(1);
            cell.setCellValue((String) occupation.getString("sourceOfWealthCode") );
          
            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue(" ");
            cell.setCellStyle(headerCellStyle);

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Product");
            cell.setCellStyle(headerCellStyle);

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Product Code");
            cell = row.createCell(1);
            cell.setCellValue((String) product.getString("code") );

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Product Name");
            cell = row.createCell(1);
            cell.setCellValue((String) product.getString("name") );

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Prodcut Segment Code");
            cell = row.createCell(1);
            cell.setCellValue((String) product.getString("segmentCode") );

            int j = 1;

            for (int k = 0; k < product.getJsonArray("facilities").size(); k++)
            {
                int fnum = k + 1;
                JsonObject facility = product.getJsonArray("facilities").getJsonObject(k);
                row = sheet.createRow(rowNum++);
                cell = row.createCell(0);
                cell.setCellValue((String) "Facility " + fnum);
                cell = row.createCell(1);
                cell.setCellValue((String) facility.getString("name"));
            }
            for (int c = 0; c < 3; c++) {
                sheet.autoSizeColumn(c);
              }
        }
       
        try {
            FileOutputStream outputStream = new FileOutputStream(XLS_FILE);
            workbook.write(outputStream);
            workbook.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return XLS_FILE;
    }

    private String genCSV(String uuid, JsonArray custList, JsonObject product)
    {
        String CSV_FILE = "c:/temp/"+uuid+".csv";
        
        try (
            BufferedWriter writer = Files.newBufferedWriter(Paths.get(CSV_FILE));

            CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.EXCEL.withDelimiter(','));
        ) 
        {
            custList.forEach(c->{
                try{
                    JsonObject cus = (JsonObject)c;
                    JsonObject identity = cus.getJsonObject("identityInfo");
                    csvPrinter.println();
                    csvPrinter.printRecord("Identity");
                    csvPrinter.printRecord("ID", cus.getString("idNo"));
                    csvPrinter.printRecord("ID Type Code", cus.getString("idTypeCode"));
                    csvPrinter.printRecord("City", identity.getString("city"));
                    csvPrinter.printRecord("Country", identity.getString("country"));
                    csvPrinter.printRecord("D.O.B.", identity.getString("dob"));
                    csvPrinter.printRecord("Full Name", identity.getString("fullName"));
                    csvPrinter.printRecord("Gender", identity.getString("genderCode"));
                    csvPrinter.printRecord("Issuing Country", identity.getString("issuingCountry"));
                    csvPrinter.printRecord("Nationality", identity.getString("nationality"));
                    csvPrinter.printRecord("Post Code", identity.getString("postCode"));
                    csvPrinter.printRecord("Race Code", identity.getString("raceCode"));
                    csvPrinter.printRecord("Religion Code", identity.getString("religionCode"));
                    csvPrinter.printRecord("State Code", identity.getString("stateCode"));
        
                    JsonObject occupation = cus.getJsonObject("occupationIncome");
                    csvPrinter.println();
                    csvPrinter.printRecord("Occupation");
                    csvPrinter.printRecord("BNM Counter Party Code", occupation.getString("bnmCounterPartyCode"));
                    csvPrinter.printRecord("Employment Type Code", occupation.getString("employmentTypeCode"));
                    csvPrinter.printRecord("Gross Annual Income", occupation.getInteger("grossAnnualIncome"));
                    csvPrinter.printRecord("Industry Category", occupation.getString("industryCategory"));
                    csvPrinter.printRecord("Marital Status Code", occupation.getString("maritalStatusCode"));
                    csvPrinter.printRecord("Occupation AML Code", occupation.getString("occupationAMLCode"));
                    csvPrinter.printRecord("Occupation CCRIS Code", occupation.getString("occupationCCRISCode"));
                    csvPrinter.printRecord("Occupation Category COde", occupation.getString("occupationCategoryCode"));
                    csvPrinter.printRecord("Source Of Fund", occupation.getString("sourceOfFundCode"));
                    csvPrinter.printRecord("Source Of Wealth", occupation.getString("sourceOfWealthCode"));
        
                    JsonObject contact = cus.getJsonObject("contactDetails");
                    csvPrinter.println();
                    csvPrinter.printRecord("Contact Details");
                    csvPrinter.printRecord("City", contact.getString("city"));
                    csvPrinter.printRecord("MyKad Contact Address", contact.getBoolean("contactAddressSameWithMYKAD"));
                    csvPrinter.printRecord("Country", contact.getString("country"));
                    csvPrinter.printRecord("Email", contact.getString("email"));
                    csvPrinter.printRecord("Mobile No", contact.getString("mobileNo"));
                    csvPrinter.printRecord("Post Code", contact.getString("postCode"));
                    csvPrinter.printRecord("State Code", contact.getString("stateCode"));
                    csvPrinter.println();

                    csvPrinter.println();
                    csvPrinter.printRecord("Product");
                    csvPrinter.printRecord("Product Code", product.getString("code"));
                    csvPrinter.printRecord("Product Name", product.getString("name"));
                    csvPrinter.printRecord("Prodcut Segment Code", product.getString("segmentCode"));
                  
                    product.getJsonArray("facilities").forEach(e->{
                        JsonObject facility = (JsonObject)e;
                        try{
                            csvPrinter.printRecord("Facility ", facility.getString("name"));
                        }
                        catch(Exception ex)
                        {
                            
                        }
                       
                    });
                    csvPrinter.println();
                    csvPrinter.println();
                   
                }
                catch(IOException io)
                {
                    JsonObject log = new JsonObject().put("Type", Base.LogTypeHelper.Info.logType());
                    log.put("origin", Base.getCurrentMethodName());
                    log.put("Cust List", custList);
                    log.put("Error", io.getMessage());
                    new LogHelper(mongo).log(log);
                }
            });
            csvPrinter.flush(); 
            return CSV_FILE;      
        }
        catch(IOException ex)
        {
            System.err.println(ex.getMessage());
            JsonObject log = new JsonObject().put("Type", Base.LogTypeHelper.Info.logType());
            log.put("origin", Base.getCurrentMethodName());
            log.put("Cust List", custList);
            log.put("Error", ex.getMessage());
            new LogHelper(mongo).log(log);
            return "";
        }
   
     
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

    public void initWfTracker(String resultStr, String userid, String branchCode, Handler<AsyncResult<String>> aHandler) 
    {
        System.err.println(userid);

        JsonObject caseObj = new JsonObject(resultStr);
        
        mongo.findOne(CollectionHelper.USER_BRANCH.collection(), new JsonObject().put("email", userid), null, ar -> {
            if (ar.succeeded()) {
                System.err.println(ar.result());
                JsonObject document = new JsonObject()
                .put("caseid", caseObj.getString("id"))
                .put("email", userid)
                .put("branch", branchCode)
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

    // private void caseWhisky(List<JsonObject> amlObjects, String idfront, String idback, String customerDetails, Handler<AsyncResult<Whisky>> aHandler)
    // {
    //     mongo.findOne(CollectionHelper.WF_TRIGGER.collection(), new JsonObject().put("name", "newage").put("version", "1.0"), null, ar -> {
    //         if (ar.succeeded()) {
    //             Gson gson = new GsonBuilder().create();
    //             Whisky whisky = gson.fromJson(Json.encode(ar.result()), Whisky.class);
    //             List<Field> fields = whisky.triggerInstance.data.formInstance.value.getFields();
    //             List<Field> jfk = new ArrayList<Field>();
    //             for (int i = 0; i < fields.size(); i++) {
    //                 boolean add = true;
    //                 Field f = fields.get(i);
    //                 // System.out.println(gson.toJson(f));
    //                 // System.out.println(f.getName().equals("Name"));
    //                 // System.out.println(f.getName());
    //                 for(int j=0;j<amlObjects.size();j++){
    //                     System.err.println(f.getName().toLowerCase() + ":" + amlObjects.get(j).getString("eddCode").toLowerCase());
    //                     if (f.getName().toLowerCase().equals(amlObjects.get(j).getString("eddCode").toLowerCase()))
    //                     {
    //                         //f.setValue("true");
    //                         System.err.println("in");
    //                         //fields.remove(f);
    //                         add = false;
                           
    //                     }

    //                 } 
                   
    //                 if (f.getName().toLowerCase().equals("id front"))
    //                 {
    //                     f.setValue(idfront);
                      
    //                 }
    //                 else if (f.getName().toLowerCase().equals("id back"))
    //                 {
    //                     f.setValue(idfront);
                       
    //                 }
    //                 else if (f.getName().toLowerCase().equals("customer details"))
    //                 {
    //                     f.setValue(idfront);
                        
    //                 }
    //                 fields.set(i, f);
    //                 if (add)
    //                     jfk.add(f);
    //             }//end for
    //             whisky.triggerInstance.data.formInstance.value.setFields(jfk);
    //             aHandler.handle(Future.succeededFuture(whisky));
    //         }
    //         else{
    //             aHandler.handle(Future.failedFuture(ar.cause()));
    //         }
            
    //     });
       
    // }
    private void caseWhisky(List<JsonObject> amlObjects, String idfront, String idback, String customerDetails, String caseCreator, JsonArray custDetails, Handler<AsyncResult<JsonObject>> aHandler)
    {
        mongo.findOne(CollectionHelper.WF_TRIGGER.collection(), new JsonObject().put("name", "newage").put("version", "1.0"), null, ar -> {
            if (ar.succeeded()) {
                JsonObject t = new JsonObject().put("triggerInstance", ar.result().getJsonObject("triggerInstance"));

                JsonArray fields = t.getJsonObject("triggerInstance").getJsonObject("data").getJsonObject("formInstance").getJsonObject("value").getJsonArray("fields");
                fields.forEach(f->{
                    JsonObject field = (JsonObject) f;
                    // for(int j=0;j<amlObjects.size();j++){
                        
                    //     if (field.getString("name").toLowerCase().equals(amlObjects.get(j).getString("eddCode").toLowerCase()))
                    //     {
                    //         field.put("value", true);
                        
                    //     }

                    // } 
                    for(int j=0;j<amlObjects.size();j++){
                        
                        if (field.getString("name").toLowerCase().equals("edd code"))
                        {
                            field.put("value", amlObjects.get(j).getString("eddCode"));
                        
                        }

                    } 
                    if (field.getString("name").toLowerCase().equals("id front"))
                    {
                        field.put("value", idfront);
                    }
                    else if (field.getString("name").toLowerCase().equals("id back"))
                    {
                        field.put("value", idback);
                    }
                    else if (field.getString("name").toLowerCase().equals("customer details"))
                    {
                        field.put("value", customerDetails);
                    }
                    else if (field.getString("name").toLowerCase().equals("case creator"))
                    {
                        field.put("value", caseCreator);
                    }
                    else if (field.getString("name").toLowerCase().equals("customer name"))
                    {
                        custDetails.forEach(c->{
                          
                            JsonObject cus = (JsonObject)c;
                            JsonObject identity = cus.getJsonObject("identityInfo");
                            field.put("value",  identity.getString("fullName"));
                          
                        });
                       
                    }
                    else if (field.getString("name").toLowerCase().equals("nric"))
                    {
                        custDetails.forEach(c->{
                            JsonObject cus = (JsonObject)c;
                            field.put("value", cus.getString("idNo"));
                        });
                       
                       
                    }
                });
                //System.err.println(t);
                aHandler.handle(Future.succeededFuture(t));
            }
            else{
                aHandler.handle(Future.failedFuture(ar.cause()));
            }
            
        });
       
    }
    // private static Whisky caseWhisky(String newCaseName, String idfront)
    // {
    //     try(Reader reader = new InputStreamReader(io.vertx.shiva.MainVerticle.class.getClassLoader().getResourceAsStream("test.json"), "UTF-8")){
    //         Gson gson = new GsonBuilder().create();
    //         Whisky whisky = gson.fromJson(reader, Whisky.class);
    //         List<Field> fields = whisky.triggerInstance.data.formInstance.value.getFields();
    //         for (int i = 0; i < fields.size(); i++) {
    //             Field f = fields.get(i);
    //             // System.out.println(gson.toJson(f));
    //             // System.out.println(f.getName().equals("Name"));
    //             // System.out.println(f.getName());
    //             if (f.getName().equals("name"))
    //             {
    //                 f.setValue(newCaseName);
    //                 fields.set(i, f);
    //             }
    //             if (f.getName().equals("idfront"))
    //             {
    //                 f.setValue(idfront);
    //                 fields.set(i, f);
    //             }
    //         }//end for
    //         whisky.triggerInstance.data.formInstance.value.setFields(fields);
    //         return whisky;
        
    //     }//end try
    //     catch (IOException e) {
    //         return null;
    //     }//end catch
    // }
}