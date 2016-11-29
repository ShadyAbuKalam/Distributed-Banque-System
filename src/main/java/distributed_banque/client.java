package distributed_banque;

import java.io.Console;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Scanner;
public class client {

	public static void main(String[] args) {
		try {  
			//.create distributed_banque.client socket and connect to server
			System.out.println("please enter the IP Address and Port number");
			
			Scanner sc = new Scanner(System.in);
			String tempIPandport = sc.nextLine();
			String[] Ipandport=tempIPandport.split(":");
			String ip = Ipandport[0];
			int port = Integer.valueOf(Ipandport[1]);
			Socket cl =new Socket(ip,port) ;
		
		   //.create comm streams
			DataInputStream dis = new DataInputStream(cl.getInputStream());
			DataOutputStream dos = new DataOutputStream(cl.getOutputStream());
		
		   //3.perform I/O with server
			dos.writeUTF("CLIENT-CONN");
        
	        while (true) {
	            //receive msg from server
	            String servercommand =  dis.readUTF();
	            System.out.println(servercommand);
	            
	            if(servercommand.equalsIgnoreCase("disconnect"))
	                break;
	            else if(servercommand.equals("More than 3 wrong entries") )
	            	break;
	            else if(servercommand.contains("Please")) {
	            	String userInput;
	            	if(servercommand.contains("enter password")) {
	            		Console console = System.console();
	            		if(console != null) {
	            			char[] pass = console.readPassword(); 
	                		userInput = new String(pass);
	            		}
	            		else {
	            			userInput = sc.nextLine();
	            		}
	            	} else {
	            		userInput = sc.nextLine();
	            	}
	                dos.writeUTF(userInput);
	            }
	        }
	        //4.terminate connection with server
	        cl.close();
	        dis.close();
	        dos.close();
		} catch (Exception e) {
			System.out.println("Something went wrong");
		}
	}
}
