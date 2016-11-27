package database;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Created by Shady Atef on 11/26/16.
 * Copyrights Shadyoatef@gmail.com
 */
public class DatabaseInterface {
    
    //todo : Join configuration from gradle & here into unified access
    private static final String URL = "jdbc:mysql://localhost/distributed_banque";
    private static final String USER_NAME = "root";
    private static final String PASSWORD = "123";
    private Connection getConnection()
    {
        try {
            return DriverManager.getConnection(URL,USER_NAME,PASSWORD);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    public boolean authenticate(String user_name,String password){
        
        throw new NotImplementedException();
    }
    public int getBalance(String user_name){
        throw new NotImplementedException();
    }
    
    public boolean withdraw(String user_name,int amount){
        throw new NotImplementedException();
    }
    
    
    public boolean deposit(String user_name,int amount){
        throw new NotImplementedException();
    }
    
    public boolean transferTo(String send_user,String target_user,int amount){
        throw new NotImplementedException();
    }
    
    public boolean transferTo(String internal_user
            ,String external_bank,String external_user,int amount){
        throw new NotImplementedException();
    }
    
}
