import database.DatabaseInterface;
import database.exceptions.DuplicateAccountException;
import database.exceptions.NotFoundAccountException;

public class main {
    public static void main(String[] args) {
        DatabaseInterface db = new DatabaseInterface();
        System.out.println("Inserting new user");
        try {
            if (db.insertAccount("shady94", "12345")) {
                System.out.println("Inserted new user successfully");
                try {
                    if (db.authenticate("shady94", "12345")) {
                        System.out.println("Authenticated new user successfully");
                        
                    }
                } catch (NotFoundAccountException e) {
                    System.out.println("Account Not Found");
    
                }
            } else
                System.out.println("Failed to insert new user successfully");
        } catch (DuplicateAccountException e) {
            System.out.println((char)27 + "[31mDuplicate Entry" + (char)27 + "[0m");
        }
        
        
    }
}