package distributed_banque;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by Shady Atef on 11/28/16.
 * Copyrights Shadyoatef@gmail.com
 */
public class Config {
    private static  String filename = "config.json";
    private static String bankName;
    private static String authToken;
    private static String database;
    private static String databaseUser;
    private static String databasePassword;
    private static int port;
    
    static {
        //If file doesn't exist get it from home directory
        // This is a hack to run two or more servers on Vagrant MultiMachine system
        if(Files.notExists(Paths.get(filename))){
            filename = System.getProperty("user.home")+"/"+filename;
        }
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
            System.out.println("Config  file is not found or mal-formatted, please check your config.json file. ");
            System.exit(-1);
            
            
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
