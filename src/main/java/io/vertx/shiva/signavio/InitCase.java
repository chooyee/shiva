package io.vertx.shiva.signavio;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.CompositeFuture;
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

public class InitCase extends Case {
    public InitCase(MongoClient mongo) {
        super(mongo);
    }

    public void InitWorkflow(JsonObject jsonObj, String token, Handler<AsyncResult<JsonObject>> aHandler) {
        JsonArray eddObjs = new JsonArray();
        List<JsonObject> amlObjs = getAMLCode(jsonObj);
        JsonObject initWf = init(jsonObj, token);
        List<Future> futures = new ArrayList<>();
        JsonArray qna = new JsonArray();
        for (int k = 0; k < amlObjs.size(); k++) {

            Future<JsonObject> insertFuture = Future.future();
            futures.add(insertFuture);
            String eddCode = amlObjs.get(k).getString("eddCode");
            JsonArray questions = amlObjs.get(k).getJsonArray("questions");
            prepareWorkflowJson2(eddCode, initWf.getString("idFrontFileID"), initWf.getString("idBackFileID"),
                    initWf.getString("idCusFileID"), initWf.getString("caseCreator"), initWf.getString("branchCode"),
                    initWf.getJsonArray("custList"), initWf.getJsonArray("attachmentIdJArray"), token, ar -> {
                        if (ar.succeeded()) {
                            String postResult = WebClientHelper.postJson(new SignavioApiPathHelper().getCases(), token,
                                    ar.result());
                            String caseId = new JsonObject(postResult).getString("id");
                            JsonObject edd = new JsonObject();
                            edd.put("eddCode", eddCode);
                            edd.put("caseId", caseId);
                            edd.put("checker", initWf.getString("checker"));
                            edd.put("maker", initWf.getString("maker"));
                            edd.put("note", initWf.getString("note"));
                            edd.put("qna", filterQnA(initWf.getJsonArray("qna"), questions));
                            eddObjs.add(edd);
                            initWfTracker(caseId, jsonObj, wfHandler -> {
                            });
                            insertFuture.complete(edd);
                        }
                    });
            // prepareWorkflowJson(eddCode, initWf.getString("idFrontFileID"),
            // initWf.getString("idBackFileID"), initWf.getString("idCusFileID"),
            // initWf.getString("caseCreator"), initWf.getString("branchCode"),
            // initWf.getJsonArray("custList"), ar->{
            // if (ar.succeeded())
            // {
            // String postResult = WebClientHelper.postJson(new
            // SignavioApiPathHelper().getCases(), token, ar.result());
            // String caseId = new JsonObject(postResult).getString("id");
            // JsonObject edd = new JsonObject();
            // edd.put("eddCode", eddCode);
            // edd.put("caseId", caseId);
            // edd.put("qna", initWf.getJsonArray("qna"));
            // eddObjs.add(edd);
            // initWfTracker(caseId, jsonObj, wfHandler->{});
            // insertFuture.complete(edd);
            // }
            // });
        }

        CompositeFuture.all(futures).setHandler(ah -> {
            System.err.println(ah.result());
            String email = jsonObj.getString("email");
            String branchCode = jsonObj.getString("branchCode");
            String customerId = jsonObj.getJsonObject("application").getJsonArray("eform").getJsonObject(0)
                    .getString("idNo");
            // System.err.println(customerId);

            JsonObject document = new JsonObject().put("customerId", customerId).put("email", email)
                    .put("branch", branchCode).put("type", "indi").put("edd", eddObjs).put("complete", false);

            mongo.insert(CollectionHelper.INDI_TRACKER.collection(), document, insertar -> {
                if (insertar.succeeded()) {
                    JsonObject result = new JsonObject();
                    result.put("id", insertar.result());
                    aHandler.handle(Future.succeededFuture(result));
                } else {
                    aHandler.handle(Future.failedFuture(document.encodePrettily()));
                }
            });
        });

    }

    public JsonArray filterQnA(JsonArray QnAs, JsonArray questions) {
        JsonArray filter = new JsonArray();
        for (int i = 0; i < questions.size(); i++) {
            JsonObject question = questions.getJsonObject(i);
            for (int k = 0; k < QnAs.size(); k++) {
                JsonObject qna = QnAs.getJsonObject(k);
                if (qna.getString("questionId").toLowerCase() == question.getString("id").toLowerCase()) {
                    filter.add(qna);
                }
            }
        }
        return filter;
    }

    /**
     * Inititalize new workflow
     * 
     * @param jsonObj
     * @param token
     * @param aHandler
     */
    public JsonObject init(JsonObject jsonObj, String token) {
        final String id = jsonObj.getString("id");
        final String branchCode = jsonObj.getString("branchCode");
        JsonArray attachmentIdJArray = new JsonArray();
        // final String newCaseName = jsonObj.getString("newCaseName");
        String idFrontFileID = "";
        String idBackFileID = "";
        String idCusFileID = "";
        JsonObject fileJson;
        String caseCreator = jsonObj.getString("email");
        JsonArray custList = getCustomerDetails(jsonObj);
        JsonObject product = getProductDetails(jsonObj);
        JsonArray qna = getQNA(jsonObj);
        String maker = "";
        String checker = "";
        String note = "";
        if (jsonObj.containsKey("eddCases")) {

            JsonArray eddCases = jsonObj.getJsonArray("eddCases");
            for (int k = 0; k < eddCases.size(); k++) {
                JsonObject eddCase = eddCases.getJsonObject(k);
                maker = eddCase.getString("createdBy");
                checker = eddCase.getString("lastModifiedBy");
                note = eddCase.getString("caseStatusReason");
                int j = 1;
                JsonArray supportingDocs = eddCase.getJsonArray("attachment");

                for (int i = 0; i < supportingDocs.size(); i++) {
                    j = i + 1;
                    byte[] imageByte;
                    JsonObject supportingDoc = supportingDocs.getJsonObject(i);
                    String fileName = "supporting_doc_" + j + ".png";
                    String fileContent = supportingDoc.getString("content");

                    if (fileContent.length() > 0) {
                        imageByte = Base64.getDecoder().decode(fileContent);
                        JsonObject fileJsonAtt = new JsonObject(
                                WebClientHelper.uploadToSignavio(imageByte, fileName, token));
                        String attachmentId = fileJsonAtt.getString("id");
                        attachmentIdJArray.add(attachmentId);
                    }
                }

            }
        }

        if (jsonObj.containsKey("efaces")) {
            String idfront = "";
            String idback = "";
            byte[] imageByte;
            JsonArray eface = jsonObj.getJsonArray("efaces");

            for (int i = 0; i < eface.size(); i++) {
                JsonObject obj = eface.getJsonObject(i);

                if (obj.containsKey("idfront"))
                    idfront = obj.getString("idfront");
                if (obj.containsKey("idback"))
                    idback = obj.getString("idback");
            }

            if (idfront.length() > 0) {
                imageByte = Base64.getDecoder().decode(idfront);
                fileJson = new JsonObject(WebClientHelper.uploadToSignavio(imageByte, "id_front.png", token));
                idFrontFileID = fileJson.getString("id");
            }
            if (idback.length() > 0) {
                imageByte = Base64.getDecoder().decode(idback);
                fileJson = new JsonObject(WebClientHelper.uploadToSignavio(imageByte, "id_back.png", token));
                idBackFileID = fileJson.getString("id");
            }

            String filePath = genXls(jsonObj.getJsonObject("application").getString("uuid"), custList, product, qna);

            try {
                byte[] bFile = Files.readAllBytes(Paths.get(filePath));
                fileJson = new JsonObject(WebClientHelper.uploadToSignavio(bFile, "cus details.xlsx", token));
                idCusFileID = fileJson.getString("id");
            } catch (IOException io) {
                JsonObject log = new JsonObject().put("Type", Base.LogTypeHelper.Info.logType());
                log.put("origin", Base.getCurrentMethodName());
                log.put("file path", filePath);
                log.put("Error", io.getMessage());
                new LogHelper(mongo).log(log);
            }

        }

        JsonObject result = new JsonObject();
        result.put("idFrontFileID", idFrontFileID);
        result.put("idBackFileID", idBackFileID);
        result.put("idCusFileID", idCusFileID);
        result.put("caseCreator", caseCreator);
        result.put("branchCode", branchCode);
        result.put("custList", custList);
        result.put("qna", qna);
        result.put("checker", checker);
        result.put("maker", maker);
        result.put("note", note);
        result.put("attachmentIdJArray", attachmentIdJArray);
        return result;
        // prepareWorkflowJson(getAMLCode(jsonObj), idFrontFileID, idBackFileID,
        // idCusFileID, caseCreator,branchCode, custList, ar->{
        // if (ar.succeeded())
        // {
        // aHandler.handle(Future.succeededFuture(WebClientHelper.postJson(new
        // SignavioApiPathHelper().getCases(), token, ar.result())));
        // }
        // else{
        // aHandler.handle(Future.failedFuture(ar.cause()));
        // }
        // });

    }// end InitCase

    private List<JsonObject> getAMLCode(JsonObject jsonObj) {
        List<JsonObject> aml = new ArrayList<JsonObject>();
        jsonObj.getJsonArray("eddCases").forEach(c -> {
            JsonObject eddCase = (JsonObject) c;
            eddCase.getJsonObject("eddResults").getJsonObject("result").getJsonArray("details").forEach(e -> {
                aml.add((JsonObject) e);
            });
        });
        return aml;
    }

    private JsonArray getCustomerDetails(JsonObject jsonObj) {
        JsonArray custList = new JsonArray();

        if (jsonObj.containsKey("application"))
            ;
        {
            JsonObject application = jsonObj.getJsonObject("application");
            if (application.containsKey("eform")) {
                application.getJsonArray("eform").forEach(e -> {
                    JsonObject cust = new JsonObject();
                    JsonObject eform = (JsonObject) e;
                    if (eform.containsKey("idNo")) {
                        cust.put("idNo", eform.getString("idNo"));
                    }
                    if (eform.containsKey("idTypeCode")) {
                        cust.put("idTypeCode", eform.getString("idTypeCode"));
                    }
                    if (eform.containsKey("contactDetails")) {
                        cust.put("contactDetails", eform.getJsonObject("contactDetails"));
                    }
                    if (eform.containsKey("identityInfo")) {
                        cust.put("identityInfo", eform.getJsonObject("identityInfo"));
                    }
                    if (eform.containsKey("occupationIncome")) {
                        cust.put("occupationIncome", eform.getJsonObject("occupationIncome"));
                    }
                    custList.add(cust);
                });
            }
            // if (application.containsKey("facilityCodes"))
            // {
            // cust.put("facilityCodes", application.getJsonArray("application"));
            // }

            return custList;
        }

    }

    private JsonObject getProductDetails(JsonObject jsonObj) {
        JsonObject product = new JsonObject();

        if (jsonObj.containsKey("product"))
            ;
        {
            product.put("code", jsonObj.getJsonObject("product").getString("code"));
            product.put("name", jsonObj.getJsonObject("product").getString("name"));
            product.put("segmentCode", jsonObj.getJsonObject("product").getString("segmentCode"));

            JsonArray facilities = new JsonArray();
            jsonObj.getJsonObject("product").getJsonArray("facilities").forEach(e -> {
                JsonObject facility = (JsonObject) e;
                facilities.add(new JsonObject().put("name", facility.getString("name")));

            });
            product.put("facilities", facilities);
            // if (application.containsKey("facilityCodes"))
            // {
            // cust.put("facilityCodes", application.getJsonArray("application"));
            // }

            return product;
        }

    }

    private JsonArray getQNA(JsonObject jsonObj) {
        JsonArray qna = new JsonArray();

        if (jsonObj.containsKey("eddCases"))
            ;
        {
            jsonObj.getJsonArray("eddCases").forEach(e -> {
                JsonObject eddCase = (JsonObject) e;
                if (eddCase.containsKey("eddAnswers")) {
                    eddCase.getJsonArray("eddAnswers").forEach(ea -> {
                        JsonObject eddAnswer = (JsonObject) ea;
                        eddAnswer.getJsonArray("answers").forEach(ans -> {
                            qna.add((JsonObject) ans);
                        });
                    });
                }
            });

        }
        return qna;

    }

    private String genXls(String uuid, JsonArray custList, JsonObject product, JsonArray qna) {
        String XLS_FILE = "c:/temp/" + uuid + ".xlsx";
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Sheet 1");

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 14);

        CellStyle headerCellStyle = workbook.createCellStyle();
        headerCellStyle.setFont(headerFont);

        for (int i = 0; i < custList.size(); i++) {
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
            cell.setCellValue((String) cust.getString("idNo"));

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "ID Type Code");
            cell = row.createCell(1);
            cell.setCellValue((String) cust.getString("idTypeCode"));

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "City (Permanent)");
            cell = row.createCell(1);
            cell.setCellValue((String) identity.getString("city"));

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Post Code (Permanent)");
            cell = row.createCell(1);
            cell.setCellValue((String) identity.getString("postCode"));

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "State (Permanent)");
            cell = row.createCell(1);
            cell.setCellValue((String) identity.getString("stateCode"));

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Country (Permanent)");
            cell = row.createCell(1);
            cell.setCellValue((String) identity.getString("country"));

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "D.O.B.");
            cell = row.createCell(1);
            cell.setCellValue((String) identity.getString("dob"));

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Full Name");
            cell = row.createCell(1);
            cell.setCellValue((String) identity.getString("fullName"));

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Gender");
            cell = row.createCell(1);
            cell.setCellValue((String) identity.getString("genderCode"));

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Issuing Country");
            cell = row.createCell(1);
            cell.setCellValue((String) identity.getString("issuingCountry"));

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Nationality");
            cell = row.createCell(1);
            cell.setCellValue((String) identity.getString("nationality"));

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Race");
            cell = row.createCell(1);
            cell.setCellValue((String) identity.getString("raceCode"));

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Religion");
            cell = row.createCell(1);
            cell.setCellValue((String) identity.getString("religionCode"));

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
            if (contact.getBoolean("contactAddressSameWithMYKAD")) {
                cell.setCellValue("Same as permanent address");
            } else {

                /**
                 * TODO: Show customer address
                 */
                cell.setCellValue(contact.getBoolean("contactAddressSameWithMYKAD"));
            }

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "City (Correspondence)");
            cell = row.createCell(1);
            cell.setCellValue((String) contact.getString("city"));

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Post Code (Correspondence)");
            cell = row.createCell(1);
            cell.setCellValue((String) contact.getString("postCode"));

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "State (Correspondence)");
            cell = row.createCell(1);
            cell.setCellValue((String) contact.getString("stateCode"));

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Country (Correspondence)");
            cell = row.createCell(1);
            cell.setCellValue((String) contact.getString("country"));

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Email");
            cell = row.createCell(1);
            cell.setCellValue((String) contact.getString("email"));

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Mobile No");
            cell = row.createCell(1);
            cell.setCellValue((String) contact.getString("mobileNo"));

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
            cell.setCellValue((String) occupation.getString("bnmCounterPartyCode"));

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Employment Type Code");
            cell = row.createCell(1);
            cell.setCellValue((String) occupation.getString("employmentTypeCode"));

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Gross Annual Income");
            cell = row.createCell(1);
            cell.setCellValue(occupation.getInteger("grossAnnualIncome"));

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Industry Category");
            cell = row.createCell(1);
            cell.setCellValue((String) occupation.getString("industryCategory"));

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Marital Status Code");
            cell = row.createCell(1);
            cell.setCellValue((String) occupation.getString("maritalStatusCode"));

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Occupation AML Code");
            cell = row.createCell(1);
            cell.setCellValue((String) occupation.getString("occupationAMLCode"));

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Occupation CCRIS Code");
            cell = row.createCell(1);
            cell.setCellValue((String) occupation.getString("occupationCCRISCode"));

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Occupation Category Code");
            cell = row.createCell(1);
            cell.setCellValue((String) occupation.getString("occupationCategoryCode"));

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Source Of Fund");
            cell = row.createCell(1);
            cell.setCellValue((String) occupation.getString("sourceOfFundCode"));

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Source Of Wealth");
            cell = row.createCell(1);
            cell.setCellValue((String) occupation.getString("sourceOfWealthCode"));

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
            cell.setCellValue((String) product.getString("code"));

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Product Name");
            cell = row.createCell(1);
            cell.setCellValue((String) product.getString("name"));

            row = sheet.createRow(rowNum++);
            cell = row.createCell(0);
            cell.setCellValue((String) "Prodcut Segment Code");
            cell = row.createCell(1);
            cell.setCellValue((String) product.getString("segmentCode"));

            int j = 1;

            for (int k = 0; k < product.getJsonArray("facilities").size(); k++) {
                int fnum = k + 1;
                JsonObject facility = product.getJsonArray("facilities").getJsonObject(k);
                row = sheet.createRow(rowNum++);
                cell = row.createCell(0);
                cell.setCellValue((String) "Facility " + fnum);
                cell = row.createCell(1);
                cell.setCellValue((String) facility.getString("name"));
            }

            if (qna.size() > 0) {
                row = sheet.createRow(rowNum++);
                cell = row.createCell(0);
                cell.setCellValue(" ");
                cell.setCellStyle(headerCellStyle);

                row = sheet.createRow(rowNum++);
                cell = row.createCell(0);
                cell.setCellValue((String) "EDD Questions and Answers");
                cell.setCellStyle(headerCellStyle);
            }

            for (int l = 0; l < qna.size(); l++) {
                int qnum = l + 1;
                JsonObject qnaObj = qna.getJsonObject(l);
                row = sheet.createRow(rowNum++);
                cell = row.createCell(0);
                cell.setCellValue((String) "Question ID " + qnum);
                cell = row.createCell(1);
                cell.setCellValue((String) qnaObj.getString("questionId"));

                row = sheet.createRow(rowNum++);
                cell = row.createCell(0);
                cell.setCellValue((String) "Answer ");
                cell = row.createCell(1);
                Object aObj = qnaObj.getValue("value");
                if (aObj instanceof Boolean) {
                    Boolean bval = (Boolean) aObj;
                    cell.setCellValue((Boolean) bval);
                } else {
                    cell.setCellValue((String) aObj);
                }

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

    private String genCSV(String uuid, JsonArray custList, JsonObject product) {
        String CSV_FILE = "c:/temp/" + uuid + ".csv";

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(CSV_FILE));

                CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.EXCEL.withDelimiter(','));) {
            custList.forEach(c -> {
                try {
                    JsonObject cus = (JsonObject) c;
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

                    product.getJsonArray("facilities").forEach(e -> {
                        JsonObject facility = (JsonObject) e;
                        try {
                            csvPrinter.printRecord("Facility ", facility.getString("name"));
                        } catch (Exception ex) {

                        }

                    });
                    csvPrinter.println();
                    csvPrinter.println();

                } catch (IOException io) {
                    JsonObject log = new JsonObject().put("Type", Base.LogTypeHelper.Info.logType());
                    log.put("origin", Base.getCurrentMethodName());
                    log.put("Cust List", custList);
                    log.put("Error", io.getMessage());
                    new LogHelper(mongo).log(log);
                }
            });
            csvPrinter.flush();
            return CSV_FILE;
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
            JsonObject log = new JsonObject().put("Type", Base.LogTypeHelper.Info.logType());
            log.put("origin", Base.getCurrentMethodName());
            log.put("Cust List", custList);
            log.put("Error", ex.getMessage());
            new LogHelper(mongo).log(log);
            return "";
        }

    }

    // private void prepareWorkflowJson(String eddCode, String idfront, String
    // idback, String customerDetails, String caseCreator,String branchCode,
    // JsonArray custDetails, Handler<AsyncResult<JsonObject>> aHandler)
    // {
    // // String eddCode = "";
    // // for(int k=0;k<amlObjects.size();k++){

    // // eddCode = amlObjects.get(k).getString("eddCode");

    // mongo.findOne(CollectionHelper.WF_TRIGGER.collection(), new
    // JsonObject().put("name", eddCode).put("version", "1.0"), null, ar -> {
    // if (ar.succeeded()) {
    // JsonObject t = new JsonObject().put("triggerInstance",
    // ar.result().getJsonObject("triggerInstance"));

    // JsonArray fields =
    // t.getJsonObject("triggerInstance").getJsonObject("data").getJsonObject("formInstance").getJsonObject("value").getJsonArray("fields");
    // fields.forEach(f->{
    // JsonObject field = (JsonObject) f;

    // if (field.getString("name").toLowerCase().equals("edd code"))
    // {
    // field.put("value", eddCode);

    // }
    // if (field.getString("name").toLowerCase().equals("id front"))
    // {
    // if (idfront.length()>0)
    // field.put("value", idfront);
    // }
    // else if (field.getString("name").toLowerCase().equals("id back"))
    // {
    // if (idback.length()>0)
    // field.put("value", idback);
    // }
    // else if (field.getString("name").toLowerCase().equals("customer details"))
    // {
    // field.put("value", customerDetails);
    // }
    // else if (field.getString("name").toLowerCase().equals("case creator"))
    // {
    // field.put("value", caseCreator);
    // }
    // else if (field.getString("name").toLowerCase().equals("branch code"))
    // {
    // field.put("value", branchCode);
    // }
    // else if (field.getString("name").toLowerCase().equals("customer name"))
    // {
    // custDetails.forEach(c->{

    // JsonObject cus = (JsonObject)c;
    // JsonObject identity = cus.getJsonObject("identityInfo");
    // field.put("value", identity.getString("fullName"));

    // });

    // }
    // else if (field.getString("name").toLowerCase().equals("nric"))
    // {
    // custDetails.forEach(c->{
    // JsonObject cus = (JsonObject)c;
    // field.put("value", cus.getString("idNo"));
    // });

    // }
    // });
    // //System.err.println(t);
    // aHandler.handle(Future.succeededFuture(t));
    // }
    // else{
    // aHandler.handle(Future.failedFuture(ar.cause()));
    // }

    // });

    // }

    private void prepareWorkflowJson2(String eddCode, String idfront, String idback, String customerDetails,
            String caseCreator, String branchCode, JsonArray custDetails, JsonArray supportingDocs, String authToken,
            Handler<AsyncResult<JsonObject>> aHandler) {
        // String eddCode = "";
        // for(int k=0;k<amlObjects.size();k++){

        // eddCode = amlObjects.get(k).getString("eddCode");

        mongo.findOne(CollectionHelper.WF_TRIGGER.collection(),
                new JsonObject().put("name", eddCode).put("version", "1.0"), null, ar -> {
                    if (ar.succeeded()) {
                        String sourceWorkflowId = ar.result().getJsonObject("triggerInstance")
                                .getString("sourceWorkflowId");
                        // System.err.println(sourceWorkflowId);
                        // System.err.println(new
                        // SignavioApiPathHelper().getWorkflowStartInfo(sourceWorkflowId));
                        String jsonStr = WebClientHelper
                                .get(new SignavioApiPathHelper().getWorkflowStartInfo(sourceWorkflowId), authToken);
                        // System.err.println(jsonStr);
                        JsonObject startInfo = new JsonObject(jsonStr);
                        JsonArray fields = startInfo.getJsonObject("form").getJsonArray("fields");

                        fields.forEach(f -> {
                            JsonObject field = (JsonObject) f;

                            if (field.getString("name").toLowerCase().equals("edd code")) {
                                field.put("value", eddCode);

                            }
                            if (field.getString("name").toLowerCase().equals("id front")) {
                                if (idfront.length() > 0)
                                    field.put("value", idfront);
                            } else if (field.getString("name").toLowerCase().equals("id back")) {
                                if (idback.length() > 0)
                                    field.put("value", idback);
                            } else if (field.getString("name").toLowerCase().equals("customer details")) {
                                field.put("value", customerDetails);
                            } else if (field.getString("name").toLowerCase().equals("case creator")) {
                                field.put("value", caseCreator);
                            } else if (field.getString("name").toLowerCase().equals("branch code")) {
                                field.put("value", branchCode);
                            } else if (field.getString("name").toLowerCase().equals("customer name")) {
                                custDetails.forEach(c -> {

                                    JsonObject cus = (JsonObject) c;
                                    JsonObject identity = cus.getJsonObject("identityInfo");
                                    field.put("value", identity.getString("fullName"));

                                });

                            } else if (field.getString("name").toLowerCase().equals("nric")) {
                                custDetails.forEach(c -> {
                                    JsonObject cus = (JsonObject) c;
                                    field.put("value", cus.getString("idNo"));
                                });

                            } else if (field.getString("name").toLowerCase().equals("supporting documents")) {
                                field.put("value", supportingDocs);
                            }
                        });

                        JsonObject fieldsObject = new JsonObject().put("fields", fields);
                        JsonObject valueObject = new JsonObject().put("value", fieldsObject);
                        JsonObject formInstanceObject = new JsonObject().put("formInstance", valueObject);
                        JsonObject dataObject = new JsonObject().put("data", formInstanceObject);
                        JsonObject triggerInstanceObject = new JsonObject().put("triggerInstance", dataObject);
                        triggerInstanceObject.getJsonObject("triggerInstance").put("sourceWorkflowId",
                                sourceWorkflowId);
                        // System.err.println(triggerInstanceObject);
                        aHandler.handle(Future.succeededFuture(triggerInstanceObject));
                    } else {
                        aHandler.handle(Future.failedFuture(ar.cause()));
                    }

                });

    }
}