package distributed_banque;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;


public class BankServer {

    public static void main(String[] args) {
        try {
        	
            ServerSocket server = new ServerSocket(Config.getPort());

            System.out.println("Server is Ready");
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
