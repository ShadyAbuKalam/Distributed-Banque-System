import database.DatabaseInterface;
import database.exceptions.DuplicateAccountException;
import database.exceptions.NotEnoughBalanceException;
import database.exceptions.NotFoundAccountException;

import java.sql.SQLException;

public class main {
    public static void main(String[] args) throws SQLException, DuplicateAccountException, NotFoundAccountException, NotEnoughBalanceException {
        DatabaseInterface db = new DatabaseInterface();
       db.clearTables();
        System.out.println("Inserting new users");
       db.insertAccount("shady94","123456");
       db.insertAccount("shady95","123456");
       
       db.deposit("shady94",5000);
       db.withdraw("shady94",1000);
       db.transferTo("shady94","shady95",3000);
    }
}