package distributed_banque;

import com.google.common.base.Joiner;
import distributed_banque.database.DatabaseInterface;
import distributed_banque.database.exceptions.BankNotRegisteredException;
import distributed_banque.database.exceptions.NotEnoughBalanceException;
import distributed_banque.database.exceptions.NotFoundAccountException;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class ClientHandler extends Thread {
    private final static String CLIENT_CONN = "CLIENT-CONN";
    private final static String BANK_CONN = "BANK-CONN";
    private final static String DONE = "Done";
    private final static String NOT_FOUND_ACCOUNT = "Not Found Account";
    private static final String ENTER_TRANSACTION = "Enter Transaction";
    private static final String REFUSED_CONNECTION = "Refused Connection";
    private final DataOutputStream dos;
    private final DataInputStream dis;
    
    Socket client;
    int bytesRead;
    DatabaseInterface db = new DatabaseInterface();
    private String username;
    
    public ClientHandler(Socket c) throws IOException { //constructor
        this.client = c;
        //3.create communication channel
        this.dis = new DataInputStream(client.getInputStream());
        this.dos = new DataOutputStream(client.getOutputStream());
    }
    
    
    @Override
    public void run() {
        
        try {
            
            
            String conn_type = dis.readUTF();
            while (true) {
                if (conn_type.equals(CLIENT_CONN)) {
                    if (!handleLogin()) break;
                    
                    String[] options;
                    String option;
                    do {
                        dos.writeUTF("Please select an option "
                                + "\n 1-check balance "
                                + "\n 2-View history "
                                + "\n 3-Deposit : amount "
                                + "\n 4-Withdraw : amount "
                                + "\n 5-Internal Transfer : other user : amount"
                                + "\n 6-External Transfer : bankIP : port : other bank : other user : amount "
                                + "\n 7-quit ");
                        option = dis.readUTF();
                        options = option.split(":");
                        
                        if (options[0].equals("1")) {
                            handleCheckBalance();
                        } else if (options[0].equals("2")) {
                            handleViewHistory();
                        } else if (options[0].equals("3")) {
                            handleDeposit(options);
                            
                        } else if (options[0].equals("4")) {
                            handleWithdrawal(options);
                        } else if (options[0].equals("5")) {
                            handleInternalTransfer(options);
                        } else if (options[0].equals("6")) {
                            handleClientDoExternalTransfer(options);
    
                        }
                    }
                    while (!options[0].equals("7"));
                    dos.writeUTF("disconnect");
                    break;
                } else if (conn_type.equals(BANK_CONN)) {
                    //check authentication
                    String authtoken = dis.readUTF();
                    
                    if (!(authtoken.equals(Config.getAuthToken()))) {
                        dos.writeUTF(REFUSED_CONNECTION);
                        
                    } else {
                        dos.writeUTF(ENTER_TRANSACTION);
                        String[] input = dis.readUTF().split(":");
                        String otherbank = input[0];
                        String otheruser = input[1];
                        String internaluser = input[2];
                        int amount = Integer.parseInt(input[3]);
                        if (db.transferFrom(internaluser, otherbank, otheruser, amount)) {
                            dos.writeUTF(DONE);
                        } else {
                            dos.writeUTF(NOT_FOUND_ACCOUNT);
                        }
                    }
                    break;
                }
                
            }
            
            //5.terminate connection with distributed_banque.client
            client.close();
            dis.close();
            dos.close();
            
        } catch (Exception e) {
            System.out.println("Something went wrong");
            e.printStackTrace();
        }
        
    }
    
    private void handleClientDoExternalTransfer(String[] options) throws IOException {
        try {
            
            String bankIP = options[1];
            String port = options[2];
            String otherbank = options[3];
            String otheruser = options[4];
            String amount = options[5];
            if (db.getBalance(username) >= Integer.valueOf(amount)) {
                Socket p2p = new Socket(bankIP, Integer.valueOf(port));
                DataInputStream p2pdis = new DataInputStream(p2p.getInputStream());
                DataOutputStream p2pdos = new DataOutputStream(p2p.getOutputStream());
                
                //Declare it's Bank-to-Bank communication
                p2pdos.writeUTF(BANK_CONN);
                //Send authentication token
                String authentication = db.getBankAuthToken(otherbank);
                p2pdos.writeUTF(authentication);
                while (true) {
                    //receive msg from server
                    String peerresponse = p2pdis.readUTF();
                    
                    
                    if (peerresponse.equalsIgnoreCase(ENTER_TRANSACTION)) {
                        
                        p2pdos.writeUTF(Config.getBankName() + ":" + username + ":" + otheruser + ":" + amount);
                    } else if (peerresponse.equalsIgnoreCase(REFUSED_CONNECTION)) {
                        dos.writeUTF("Process denied");
                        break;
                    } else if (peerresponse.equalsIgnoreCase(DONE)) {
                        db.transferToExternalBank(username, otherbank, otheruser, Integer.valueOf(amount));
                    dos.writeUTF(String.format("Transferred successfully %s to %s - %s", amount,otherbank,otheruser));
                        break;
                    } else if (peerresponse.equalsIgnoreCase(NOT_FOUND_ACCOUNT)) {
                        dos.writeUTF("Target account not found");
                        break;
                    }
                }
                
                
                p2p.close();
                p2pdis.close();
                p2pdos.close();
                
            }
        } catch (NotFoundAccountException e) {
            
            dos.writeUTF("This is account is not found");
        } catch (NotEnoughBalanceException e) {
            dos.writeUTF("This  account doesn't have  enough balance");
            
        } catch (BankNotRegisteredException e) {
            dos.writeUTF("The target bank is not known");
        }
        catch (UnknownHostException e){
            dos.writeUTF("Can't communicate with the target bank");
    
        }
    }
    
    private void handleViewHistory() throws IOException {
        try {
            ArrayList<String> transactions = db.getTransactions(username);
            if(transactions.size() == 0)
            {dos.writeUTF("No transactions yet on this account");}
            else
            dos.writeUTF(Joiner.on("\n").join(transactions));
        } catch (NotFoundAccountException e) {
            dos.writeUTF("Account Not Found");
        }
    }
    private void handleExternalTransfer(){
        
    }
    private void handleInternalTransfer(String[] options) throws IOException {
        try {
            db.transferToExternalBank(username, options[1], Integer.parseInt(options[2]));
            dos.writeUTF("Internal transfer of " + options[2] + " LE to " + options[1]);
        } catch (NotFoundAccountException e) {
            dos.writeUTF("This is account is not found");
        } catch (NotEnoughBalanceException e) {
            dos.writeUTF("This  account doesn't have  enough balance");
            
        } catch (NumberFormatException | IndexOutOfBoundsException e)
        
        {
            dos.writeUTF("Wrong request format");
            
        }
    }
    
    private void handleWithdrawal(String[] options) throws IOException {
        try {
            db.withdraw(username, Integer.valueOf(options[1]));
            dos.writeUTF("Balance =" + db.getBalance(username));
        } catch (NotFoundAccountException e) {
            dos.writeUTF("This is account is not found");
        } catch (NotEnoughBalanceException e) {
            dos.writeUTF("This  account doesn't have  enough balance");
            
        } catch (NumberFormatException | IndexOutOfBoundsException e)
        
        {
            dos.writeUTF("Wrong request format");
            
        }
    }
    
    private void handleDeposit(String[] option) throws IOException {
        try {
            db.deposit(username, Integer.valueOf(option[1]));
            
            dos.writeUTF("Balance =" + db.getBalance(username));
        } catch (NotFoundAccountException e) {
            dos.writeUTF("This is account is not found");
        } catch (NumberFormatException | IndexOutOfBoundsException e)
        
        {
            dos.writeUTF("Wrong request format");
            
        }
        
    }
    
    private boolean handleLogin() throws IOException, NotFoundAccountException {
        //4.perform I/O
        //a.asks for Username.
        int count = 0;
        dos.writeUTF("Please enter username");
        username = dis.readUTF();
        while (!db.doAccountExists(username) && (count < 2)) {
            dos.writeUTF("Username not valid. Please re-enter username");
            username = dis.readUTF();
            count++;
        }
        if (count >= 2) {
            dos.writeUTF("More than 3 wrong entries");
            return false;
        }
        //a.asks for Password.
        dos.writeUTF("Please enter password");
        String password = dis.readUTF();
        count = 0;
        while (!db.authenticate(username, password) && (count < 2)) {
            dos.writeUTF("Wrong password.Please re-enter your password");
            password = dis.readUTF();
            count++;
        }
        if (count >= 2) {
            dos.writeUTF("More than 3 wrong entries");
            return false;
        }
        
        dos.writeUTF("Welcome, " + username);
        return true;
    }
    
    private void handleCheckBalance() throws IOException {
        try {
            dos.writeUTF("Balance =" + db.getBalance(username));
        } catch (NotFoundAccountException e) {
            dos.writeUTF("This is account is not found");
        }
    }
}


