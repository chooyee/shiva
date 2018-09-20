package io.vertx.shiva;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;

public class MainVerticle extends AbstractVerticle {

  
  // Convenience method so you can run it in your IDE
//   public static void main(String[] args) {
//     Runner.runExample(MainVerticle.class);
//   }

  /**
   * This method is called when the verticle is deployed. It creates a HTTP server and registers a simple request
   * handler.
   * <p/>
   * Notice the `listen` method. It passes a lambda checking the port binding result. When the HTTP server has been
   * bound on the port, it call the `complete` method to inform that the starting has completed. Else it reports the
   * error.
   *
   * @param fut the future
   */
    @Override
    public void start(Future<Void> fut) {

        System.out.println("Main verticle has started, let's deploy some others...");
        
        CompositeFuture.all(deployHelper(ShivaVerticle.class.getName()),
            deployHelper(SatiVerticle.class.getName())).setHandler(result -> { 
                if(result.succeeded()){
                    fut.complete();
                } else {
                    fut.fail(result.cause());
                }
        });

    }

 
    @Override
    public void stop() {
    }

    private Future<Void> deployHelper(String name){
        final Future<Void> future = Future.future();
        vertx.deployVerticle(name,new DeploymentOptions().setConfig(config()), res -> {
            if(res.failed()){
                System.err.println("Failed to deploy verticle " + name);
                future.fail(res.cause());
            } else {
                System.out.println("Deployed verticle " + name);
                future.complete();
            }
        });

        return future;
    }
}
