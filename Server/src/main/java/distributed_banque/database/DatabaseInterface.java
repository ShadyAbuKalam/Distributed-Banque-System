package distributed_banque.database;

import com.google.common.hash.Hashing;
import distributed_banque.Config;
import distributed_banque.database.exceptions.BankNotRegisteredException;
import distributed_banque.database.exceptions.DuplicateAccountException;
import distributed_banque.database.exceptions.NotEnoughBalanceException;
import distributed_banque.database.exceptions.NotFoundAccountException;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;

/**
 * Created by Shady Atef on 11/26/16.
 * Copyrights Shadyoatef@gmail.com
 */
public class DatabaseInterface implements AutoCloseable {
    //todo : Join configuration from gradle & here into unified access
    private static final String URL = "jdbc:mysql://localhost/"+ Config.getDatabaseName();
    private static final String USER_NAME = Config.getDatabaseUser();
    private static final String PASSWORD = Config.getDatabasePassword();
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
        try (PreparedStatement stmt = getConnection().prepareStatement("SELECT  balance FROM Accounts WHERE user_name = ?", ResultSet.TYPE_FORWARD_ONLY);
        ) {
            
            stmt.setString(1, user_name);
            
            ResultSet result = stmt.executeQuery();
            if (!result.first())
                throw new NotFoundAccountException();
            
            int balance = result.getInt("balance");
            
            result.close();
            getConnection().commit();
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
    
    public boolean transferToExternalBank(String send_user, String target_user, int amount) throws NotFoundAccountException, NotEnoughBalanceException {
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
    
    public boolean transferToExternalBank(String internal_user
            , String external_bank, String external_user, int amount) throws NotEnoughBalanceException, NotFoundAccountException {
        try (
                PreparedStatement stmt = getConnection().prepareStatement("SELECT * FROM Accounts WHERE user_name = ?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
                PreparedStatement transactionStmt = getConnection().prepareStatement("INSERT INTO Transactions (user_name,amount,type,timestamp) VALUES (?,?,?,?)");
                PreparedStatement InternalTransactionStmt = getConnection().prepareStatement("INSERT INTO ExternalTransactions (internal_user,timestamp,external_user,external_bank) VALUES (?,?,?,?)");
    
        ) {
        
            stmt.setString(1, internal_user);
            ResultSet resultset = stmt.executeQuery();
            //Decrement the balance of the internal user
            if (!resultset.first())
                throw new NotFoundAccountException();
            if (resultset.getInt("balance") < amount)
                throw new NotEnoughBalanceException();
            int balance = resultset.getInt("balance");
            resultset.updateInt("balance", resultset.getInt("balance") - amount);
            resultset.updateRow();
        
        
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            transactionStmt.setString(1, internal_user);
            transactionStmt.setInt(2, amount);
            transactionStmt.setInt(3, TransactionTypes.OutcomingExternalTransaction.ordinal());
            transactionStmt.setTimestamp(4, timestamp);
            transactionStmt.execute();
        
            InternalTransactionStmt.setString(1, internal_user);
            InternalTransactionStmt.setTimestamp(2, timestamp);
            InternalTransactionStmt.setString(3, external_user);
            InternalTransactionStmt.setString(4, external_bank);
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
        }
            
        }
    
    public boolean transferFrom(String internal_user
            , String external_bank, String external_user, int amount) throws NotFoundAccountException {
        //todo : implement the record-keeping of transferring from external bank
    
        try (
                PreparedStatement stmt = getConnection().prepareStatement("SELECT * FROM Accounts WHERE user_name = ?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
                PreparedStatement transactionStmt = getConnection().prepareStatement("INSERT INTO Transactions (user_name,amount,type,timestamp) VALUES (?,?,?,?)");
                PreparedStatement ExternalTransactionStmt = getConnection().prepareStatement("INSERT INTO ExternalTransactions (internal_user,timestamp,external_user,external_bank) VALUES (?,?,?,?)");
    
        ) {
        
            stmt.setString(1, internal_user);
            ResultSet resultset = stmt.executeQuery();
            //Decrement the balance of the internal user
            if (!resultset.first())
                throw new NotFoundAccountException();;
            int balance = resultset.getInt("balance");
            resultset.updateInt("balance", resultset.getInt("balance") + amount);
            resultset.updateRow();
        
        
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            transactionStmt.setString(1, internal_user);
            transactionStmt.setInt(2, amount);
            transactionStmt.setInt(3, TransactionTypes.IncomingExternalTransaction.ordinal());
            transactionStmt.setTimestamp(4, timestamp);
            transactionStmt.execute();
        
            ExternalTransactionStmt.setString(1, internal_user);
            ExternalTransactionStmt.setTimestamp(2, timestamp);
            ExternalTransactionStmt.setString(3, external_user);
            ExternalTransactionStmt.setString(4, external_bank);
            ExternalTransactionStmt.execute();
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
    
    public String getBankAuthToken(String bankname) throws BankNotRegisteredException {
        try (PreparedStatement stmt = getConnection().prepareStatement("SELECT  * FROM Banks WHERE name = ?");
        ) {
        
            stmt.setString(1, bankname);
        
            ResultSet result = stmt.executeQuery();
            if(!result.first())
                throw new BankNotRegisteredException();
            return result.getString("auth_token");
        
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        
    }
    
    public boolean doAccountExists(String user_name) {
        try (PreparedStatement stmt = getConnection().prepareStatement("SELECT  * FROM Accounts WHERE user_name = ?");
        ) {
            
            stmt.setString(1, user_name);
            
            ResultSet result = stmt.executeQuery();
            boolean exists = result.first();
            result.close();
            
            return exists;
            
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public ArrayList<String> getTransactions(String username) throws NotFoundAccountException {
        if(!this.doAccountExists(username))
        {throw  new NotFoundAccountException();}
        ArrayList<String> transactionsHistory = new ArrayList<>();
        try (
                PreparedStatement withdrawalDepositStmt = getConnection().prepareStatement("SELECT * FROM Transactions WHERE user_name = ? AND type IN (?,?)");
                PreparedStatement internalTransferStmt = getConnection().prepareStatement("SELECT a.timestamp,a.amount, b.send_user ,b.target_user FROM Transactions as a,InternalTransactions as b WHERE a.user_name = b.send_user AND a.timestamp = b. timestamp AND (b.send_user = ? OR b.target_user = ?) ");
                PreparedStatement externalTransferStmt = getConnection().prepareStatement("SELECT a.timestamp,a.amount,a.type, b.internal_user ,b.external_user,b.external_bank FROM Transactions as a,ExternalTransactions as b WHERE a.user_name = b.internal_user AND a.timestamp = b. timestamp AND b.internal_user= ?  ")
        
        ) {
            withdrawalDepositStmt.setString(1,username);
            withdrawalDepositStmt.setInt(2,TransactionTypes.Withdraw.ordinal());
            withdrawalDepositStmt.setInt(3,TransactionTypes.Deposit.ordinal());
            ResultSet result = withdrawalDepositStmt.executeQuery();
            while (result.next()){
                String history = "%s %s %s" ;
                if(result.getInt("type")==TransactionTypes.Deposit.ordinal())
                {
                  history =  String.format(history, "Deposit",result.getInt("amount"),result.getTimestamp("timestamp"));
                }
                else history = String.format(history, "Withdraw",result.getInt("amount"),result.getTimestamp("timestamp"));
                
                transactionsHistory.add(history);
            
            }
            result.close();
    
            internalTransferStmt.setString(1,username);
            internalTransferStmt.setString(2,username);
             result =   internalTransferStmt.executeQuery();
            while (result.next()){
                String history = "Internal transfer from  %s to %s with amount of %s at %s" ;
    
                history = String.format(history, result.getString("send_user"),result.getString("target_user"),result.getInt("amount"),result.getTimestamp("timestamp"));
    
                transactionsHistory.add(history);
            }
            result.close();
            
            externalTransferStmt.setString(1,username);
             result =   externalTransferStmt.executeQuery();
            while (result.next()){
                String history = "External transfer from  %s to %s with amount of %s at %s" ;
    
                if(result.getInt("type") == TransactionTypes.IncomingExternalTransaction.ordinal())
                {
                    history = String.format(history,
                            result.getString("external_bank")+" - "+result.getString("external_user"),
                            result.getString("internal_user"),
                            result.getInt("amount"),
                            result.getTimestamp("timestamp"));
    
                }
                else{
                    history = String.format(history,
                            result.getString("internal_user"),
                            result.getString("external_bank")+" - "+result.getString("external_user"),
                            result.getInt("amount"),
                            result.getTimestamp("timestamp"));
    
                }
    
                transactionsHistory.add(history);
            }
            result.close();
            // Mysql select starts a transaction & due to its isolation level REPEATABLE-READ, A commit is required to read other updates written by other threads
            getConnection().commit();
            
        } catch (SQLException e) {
            
            e.printStackTrace();
        }
        return transactionsHistory;
    }
    
    enum TransactionTypes {
        Withdraw, Deposit, InternalTransaction, IncomingExternalTransaction, OutcomingExternalTransaction
    }
    
}