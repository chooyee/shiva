package io.vertx.shiva.signavio;


import io.vertx.ext.mongo.MongoClient;


public class Base
{
    protected MongoClient mongo;

    public MongoClient getMongo() {
        return mongo;
    }

    public void setMongo(MongoClient mongo) {
        this.mongo = mongo;
    }

    public Base(MongoClient mongo)
    {
        this.mongo = mongo;
    }

    public enum CollectionHelper {
        CASES("cases"),
        TASKS("tasks"),
        USERS("users"),
        GROUPS("groups"),
        WORKFLOWS("WORKFLOWS"),
        TRACKER("abmb_tracker"),
        USER_BRANCH("abmb_user_branch"),
        INIT_TRACK("abmb_init_log"),
        ASSIGN_FAILED("abmb_set_assignee_failed"),
        ASSIGN_SUCCESS("abmb_set_assignee_success"),
        LOG("abmb_log");
       
    
        private String collection;
    
        CollectionHelper(String collection) {
            this.collection = collection;
        }
    
        public String collection() {
            return collection;
        }
    }
    
}