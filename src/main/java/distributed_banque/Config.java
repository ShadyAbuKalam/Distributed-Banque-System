package distributed_banque;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.FileReader;

/**
 * Created by Shady Atef on 11/28/16.
 * Copyrights Shadyoatef@gmail.com
 */
public class Config {
    private static final String filename = "config.json";
    private static String bankName;
    private static String authToken;
    private static String database;
    private static String databaseUser;
    private static String databasePassword;
    private static int port;
    
    static {
        JsonParser parser = new JsonParser();
        try {
            JsonReader reader = new JsonReader(new FileReader(filename));
            JsonObject jsonObject = parser.parse(reader).getAsJsonObject();
            bankName = jsonObject.get("bankName").getAsString();
            authToken = jsonObject.get("authToken").getAsString();
            database = jsonObject.get("database").getAsString();
            databaseUser = jsonObject.get("databaseUser").getAsString();
            databasePassword = jsonObject.get("databasePassword").getAsString();
            port = jsonObject.get("port").getAsInt();
            
        } catch (Exception e) {
            System.out.println("We are doomed, can't read config from json ! run from your TA as fast as possible");
            e.printStackTrace();
            
        }
        
    }
    
    public static String getAuthToken() {
        return authToken;
    }
    
    public static int getPort() {
        return port;
    }
    
    public static String getBankName() {
        return bankName;
        
    }
    
    public static String getDatabaseName() {
        return database;
        
    }
    
    public static String getDatabaseUser() {
        return databaseUser;
        
    }
    
    public static String getDatabasePassword() {
        return databasePassword;
    }
    
    
}
