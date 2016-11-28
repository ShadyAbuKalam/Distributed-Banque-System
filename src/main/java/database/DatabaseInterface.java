package database;

import com.google.common.hash.Hashing;
import database.exceptions.DuplicateAccountException;
import database.exceptions.NotEnoughBalanceException;
import database.exceptions.NotFoundAccountException;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.nio.charset.StandardCharsets;
import java.sql.*;

/**
 * Created by Shady Atef on 11/26/16.
 * Copyrights Shadyoatef@gmail.com
 */
public class DatabaseInterface implements AutoCloseable {
    //todo : Join configuration from gradle & here into unified access
    private static final String URL = "jdbc:mysql://localhost/distributed_banque";
    private static final String USER_NAME = "root";
    private static final String PASSWORD = "123";
    private Connection connection;
    
    @Override
    public void close() throws Exception {
        if (connection != null)
            connection.close();
    }
    
    private Connection getConnection() {
        try {
            if (connection == null) {
                connection = DriverManager.getConnection(URL, USER_NAME, PASSWORD);
                connection.setAutoCommit(false);
            }
            return connection;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public boolean authenticate(String user_name, String password) throws NotFoundAccountException {
        
        try (PreparedStatement stmt = getConnection().prepareStatement("SELECT  password FROM Accounts WHERE user_name = ?")) {
            String hashPassword = Hashing.sha256().hashString(password, StandardCharsets.UTF_8).toString();
            
            stmt.setString(1, user_name);
            
            ResultSet result = stmt.executeQuery();
            if (!result.first())
                throw new NotFoundAccountException();
            String databasePassword = result.getString("password");
            
            result.close();
            
            return databasePassword.equals(hashPassword);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean insertAccount(String user_name
            , String password) throws DuplicateAccountException {
        int resultCount;
        try (PreparedStatement stmt = getConnection().prepareStatement("INSERT INTO Accounts(user_name,password) VALUES (?,?)")) {
            String hashPassword = Hashing.sha256().hashString(password, StandardCharsets.UTF_8).toString();
            
            stmt.setString(1, user_name);
            stmt.setString(2, hashPassword);
            resultCount = stmt.executeUpdate();
            getConnection().commit();
            
            return resultCount == 1;
        } catch (SQLException e) {
            try {
                getConnection().rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            
            throw new DuplicateAccountException();
        }
        
    }
    
    public int getBalance(String user_name) throws NotFoundAccountException {
        PreparedStatement stmt = null;
        try {
            
            stmt = getConnection().prepareStatement("SELECT  balance FROM Accounts WHERE user_name = ?", ResultSet.TYPE_SCROLL_SENSITIVE);
            stmt.setString(1, user_name);
            
            ResultSet result = stmt.executeQuery();
            if (!result.first())
                throw new NotFoundAccountException();
            
            int balance = result.getInt("balance");
            
            result.close();
            
            return balance;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }
    
    public boolean withdraw(String user_name, int amount) throws NotFoundAccountException, NotEnoughBalanceException {
        try (PreparedStatement stmt = getConnection().prepareStatement("UPDATE Accounts SET balance = ? WHERE user_name = ?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
             PreparedStatement stmt2 = getConnection().prepareStatement("INSERT INTO Transactions (user_name,amount,type) VALUES (?,?,?)")
        ) {
            
            
            int balance = this.getBalance(user_name);
            if (balance < amount)
                throw new NotEnoughBalanceException();
            balance = balance - amount;
            stmt.setInt(1, balance);
            stmt.setString(2, user_name);
            stmt.executeUpdate();
            
            stmt2.setString(1, user_name);
            stmt2.setInt(2, amount);
            stmt2.setInt(3, TransactionTypes.Withdraw.ordinal());
            stmt2.executeUpdate();
            
            getConnection().commit();
            
            
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                
                getConnection().rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            
            return false;
        }
        
    }
    
    public boolean deposit(String user_name, int amount) throws NotFoundAccountException {
        try (
                PreparedStatement stmt = getConnection().prepareStatement("UPDATE Accounts SET balance = ? WHERE user_name = ?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
                PreparedStatement stmt2 = getConnection().prepareStatement("INSERT INTO Transactions (user_name,amount,type) VALUES (?,?,?)")
        
        ) {
            
            int balance = this.getBalance(user_name);
            balance = balance + amount;
            stmt.setInt(1, balance);
            stmt.setString(2, user_name);
            stmt.executeUpdate();
            
            stmt2.setString(1, user_name);
            stmt2.setInt(2, amount);
            stmt2.setInt(3, TransactionTypes.Deposit.ordinal());
            stmt2.executeUpdate();
            
            getConnection().commit();
            
            
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                
                getConnection().rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            
            return false;
        }
    }
    
    public boolean transferTo(String send_user, String target_user, int amount) throws NotFoundAccountException, NotEnoughBalanceException {
        try (
                PreparedStatement stmt = getConnection().prepareStatement("SELECT * FROM Accounts WHERE user_name IN (?,?)", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
                PreparedStatement transactionStmt = getConnection().prepareStatement("INSERT INTO Transactions (user_name,amount,type,timestamp) VALUES (?,?,?,?)");
                PreparedStatement InternalTransactionStmt = getConnection().prepareStatement("INSERT INTO InternalTransactions (send_user,timestamp,target_user) VALUES (?,?,?)");
        
        ) {
            
            stmt.setString(1, send_user);
            stmt.setString(2, target_user);
            ResultSet resultset = stmt.executeQuery();
            
            if (!resultset.first())
                throw new NotFoundAccountException();
            if (resultset.getInt("balance") < amount)
                throw new NotEnoughBalanceException();
            int balance = resultset.getInt("balance");
            resultset.updateInt("balance", resultset.getInt("balance") - amount);
            resultset.updateRow();
            
            if (!resultset.next()) {
                throw new NotFoundAccountException();
            }
            resultset.updateInt("balance", resultset.getInt("balance") + amount);
            resultset.updateRow();
            
            //todo Insert Transaction History
            
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            transactionStmt.setString(1, send_user);
            transactionStmt.setInt(2, amount);
            transactionStmt.setInt(3, TransactionTypes.InternalTransaction.ordinal());
            transactionStmt.setTimestamp(4, timestamp);
            transactionStmt.execute();
            
            InternalTransactionStmt.setString(1, send_user);
            InternalTransactionStmt.setTimestamp(2, timestamp);
            InternalTransactionStmt.setString(3, target_user);
            InternalTransactionStmt.execute();
            getConnection().commit();
            
            
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                
                getConnection().rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            
            return false;
        } catch (Exception e1) {
            try {
                getConnection().rollback();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            throw e1;
        }
    }
    
    public boolean transferTo(String internal_user
            , String external_bank, String external_user, int amount) {
        throw new NotImplementedException();
    }
    
    public boolean transferFrom(String internal_user
            , String external_bank, String external_user, int amount) {
        throw new NotImplementedException();
    }
    
    public void clearTables() throws SQLException {
        Statement stmt = getConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
        stmt.execute("SET FOREIGN_KEY_CHECKS=0;");
        ResultSet x = stmt.executeQuery("SELECT Concat('TRUNCATE TABLE ',table_schema,'.',TABLE_NAME, ';') FROM INFORMATION_SCHEMA.TABLES WHERE  table_schema IN ('distributed_banque')");
        while (x.next()) {
            Statement stmt2 = getConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            stmt2.execute(x.getString(1));
            stmt2.close();
        }
        ;
        stmt.execute("SET FOREIGN_KEY_CHECKS=1;");
        
    }
    
    enum TransactionTypes {
        Withdraw, Deposit, InternalTransaction, IncomingExternalTransaction, OutcomingExternalTransaction
    }
    
}