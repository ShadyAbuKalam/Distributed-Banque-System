package distributed_banque;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;


public class BankServer {

    public static void main(String[] args) {
        try {
            //0. Set Fakes users
//            DatabaseInterface db = new DatabaseInterface();
//            db.clearTables();
//            db.insertAccount("shady94","123");
//            db.insertAccount("yasmine","123");
//            db.close();
            //1.Create Server Socket
        	
            ServerSocket server = new ServerSocket(Config.getPort());

            System.out.print("Server is Ready");
            while (true) {
                //2.accept connection
                Socket c = server.accept();
                System.out.println("Client Arrived");
                ClientHandler ch = new ClientHandler(c);
                ch.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Something Went Wrong");
        }
    }
}
