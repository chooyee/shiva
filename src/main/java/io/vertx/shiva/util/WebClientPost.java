package io.vertx.shiva.util;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;

import io.vertx.ext.web.client.WebClient;

public class WebClientPost extends AbstractVerticle
{
    public void postJson(String host, String requestUri, int port, String authToken, Object pojo, Handler<AsyncResult<String>> aHandler)
    {
        System.out.println(Json.encodePrettily(pojo));
        WebClient client = WebClient.create(vertx);
        client       
        .post(port, host, requestUri)
        .putHeader("Authorization", authToken)
        .putHeader("Content-Type", "application/json")
        .sendJson(pojo, ar -> {
        if (ar.succeeded()) {
            aHandler.handle(Future.succeededFuture(ar.result().bodyAsString())); 
        }
        else
        {
            aHandler.handle(Future.failedFuture("Error: An unexpected error had occur!" + Json.encodePrettily(pojo))); 
        }
        });//end client
    }
}