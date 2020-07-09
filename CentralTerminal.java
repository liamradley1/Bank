import java.sql.*;
import java.util.InputMismatchException;
import java.util.Scanner;

class CentralTerminal {
    public static String[] login(Scanner scanner, Connection connection){
        String[] details = new String[2];
        try{    
            boolean valid = false;
            String sortCode= null;
            String accountNumber = null;
            while(!valid){
                System.out.println("Please enter your sort code. Do not include any dashes...");
                sortCode = scanner.nextLine();
                Statement statement = connection.createStatement();
                ResultSet bankSet = statement.executeQuery("SELECT sort_code from banks");
                while(bankSet.next()){
                    String checker = bankSet.getString("sort_code");
                    if(sortCode.equals(checker)){
                        valid = true;
                        System.out.println("Sort code accepted!");
                    }
                }
                if(!valid){
                    System.out.println("This sort code could not be found. Press enter to try again...");
                    scanner.nextLine();
                }
            }
            valid = false;
            while(!valid){
                System.out.println("Please enter your account number...");
                accountNumber = scanner.nextLine();
                String sql = "SELECT account_number from accounts where sort_code = ?";
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, sortCode);
                ResultSet accountSet = preparedStatement.executeQuery();
                while(accountSet.next()){
                    String checker = accountSet.getString("account_number");
                    if(accountNumber.equals(checker)){
                        valid = true;
                        System.out.println("Account number accepted!");
                    }
                    if(!valid){
                        System.out.println("This account number could not be found. Press enter to try again...");
                        scanner.nextLine();
                    }
                }
            }
            
            valid = false; 
            while(!valid){
                System.out.println("Please enter your pin...");
                String pin = scanner.nextLine();
                String sql = "SELECT pin from accounts where sort_code = ? AND account_number = ?";
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1,sortCode);
                preparedStatement.setString(2,accountNumber);
                ResultSet accountSet = preparedStatement.executeQuery();
                while(accountSet.next()){
                    String checker = accountSet.getString("pin");
                    if(pin.equals(checker)){
                        valid = true;
                        System.out.println("Pin accepted!");
                    }
                    if(!valid){
                    System.out.println("Invalid pin. Press enter to try again...");
                    scanner.nextLine();
                    }
                }
            }
            details[0] = accountNumber;
            details[1] = sortCode;
        }
        catch(SQLException e) {
            System.out.println("Connection error.");
            e.printStackTrace();
        }
        return details;
    }

    public static void loadMenu(Scanner scanner, Connection connection, String accountNumber, String sortCode){
        int choice = 0;
        try{
            do {
                System.out.println("Please type the number corresponding to what you would like to do...");
                System.out.println("1. Make a deposit.    2. Make a withdrawal.");
                System.out.println("3. Check you balance. 4. Make a transfer.");
                System.out.println("5. Log out.");
                choice = scanner.nextInt();

                if (choice <1 || choice > 5) {
                    System.out.println("Invalid choice. Please choose an option between 1-5.");
                    scanner.nextLine();
                    break;
                } 
            } while (choice < 1 || choice > 5);

            switch (choice) {

                case 1:
                deposit(scanner, connection, accountNumber, sortCode);
                break;

                case 2:
                withdrawal(scanner, connection, accountNumber, sortCode);
                break; 

                case 3: 
                checkBalance(connection, accountNumber, sortCode);
                break; 

                case 4:
                transfer(scanner, connection, accountNumber, sortCode);
                break;

                case 5:
                System.out.println("Thank you, logging out!");
                scanner.nextLine();
                break;
            }

            if (choice !=5) {
                loadMenu(scanner, connection, accountNumber, sortCode);
            }
        }
        catch (InputMismatchException e) {
            System.out.println("Invalid choice. Please choose an option between 1-5.");
            choice = 1;
            scanner.nextLine();
            loadMenu(scanner, connection, accountNumber, sortCode);
        }
    }

    public static void deposit(Scanner scanner, Connection connection, String accountNumber, String sortCode){
        try{

            String sql = "SELECT currency from accounts WHERE account_number = ? AND sort_code = ?;";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, accountNumber);
            preparedStatement.setString(2, sortCode);
            ResultSet currencySet = preparedStatement.executeQuery();
            String currency = null;
            while(currencySet.next()){
                currency = currencySet.getString("currency");
            }

            boolean valid = false;
            do {
                System.out.println("Please enter the amount you would like to deposit...");
                System.out.printf("%s", currency);
                double amount = scanner.nextDouble();
                if(amount < 0.01){
                    System.out.println("Please enter a valid amount.");
                    scanner.nextLine();
                }
                else{

                    String sql1 = "UPDATE accounts SET balance = ? WHERE account_number = ? AND sort_code = ?;";
                    String sql2 = "SELECT balance, holder_uid  from accounts WHERE account_number = ? AND sort_code = ?;";
                    String sql3 = "INSERT INTO transactions(transaction_uid, account_number, sort_code, time_stamp, amount, memo) VALUES (uuid_generate_v4(), ?, ?, ?, ?, ?);";

                    PreparedStatement preparedStatement1 = connection.prepareStatement(sql1);
                    PreparedStatement preparedStatement2 = connection.prepareStatement(sql2);
                    
                    preparedStatement1.setString(2, accountNumber);
                    preparedStatement1.setString(3, sortCode);
                    
                    preparedStatement2.setString(1, accountNumber);
                    preparedStatement2.setString(2, sortCode);

                    PreparedStatement preparedStatement3 = connection.prepareStatement(sql3);
                    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                    preparedStatement3.setString(1, accountNumber);
                    preparedStatement3.setString(2,sortCode);
                    preparedStatement3.setTimestamp(3, timestamp);
                    preparedStatement3.setDouble(4, amount);
                    preparedStatement3.setString(5, "Deposit via ATM");
                    preparedStatement3.execute();

                    ResultSet amountSet = preparedStatement2.executeQuery();
                    while (amountSet.next()){
                        double currentBalance = amountSet.getDouble("balance"); 
                        amount += currentBalance;
                    }
                    preparedStatement1.setDouble(1, amount);
                    preparedStatement1.execute();

                    valid = true;
                    scanner.nextLine();
                }
            } while (valid == false);
        }
        catch(InputMismatchException e){
            System.out.println("Invalid character provided.");
            scanner.nextLine();
            deposit(scanner, connection, accountNumber, sortCode);
        }
        catch (SQLException e) {
            System.out.println("Connection error.");
            e.printStackTrace();
        }
    }

    public static void withdrawal(Scanner scanner, Connection connection, String accountNumber, String sortCode){
        try{

            String sql = "SELECT currency from accounts WHERE account_number = ? AND sort_code = ?;";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, accountNumber);
            preparedStatement.setString(2, sortCode);
            ResultSet currencySet = preparedStatement.executeQuery();
            String currency = null;
            while(currencySet.next()){
                currency = currencySet.getString("currency");
            }

            boolean valid = false;
            do {
                System.out.println("Please enter the amount you would like to withdraw...");
                System.out.printf("%s", currency);
                double amount = scanner.nextDouble();
                if(amount < 0.01){
                    System.out.println("Invalid input.");
                    scanner.nextLine();
                }
                else{
                    amount = - amount;
                    String sql1 = "UPDATE accounts SET balance = ? WHERE account_number = ? AND sort_code = ?";
                    String sql2 = "SELECT balance from accounts WHERE account_number = ? AND sort_code = ?";
                    String sql3 = "INSERT INTO transactions(transaction_uid, account_number, sort_code, time_stamp, amount, memo) VALUES (uuid_generate_v4(), ?, ?, ?, ?, ?);";
                    PreparedStatement preparedStatement1 = connection.prepareStatement(sql1);
                    PreparedStatement preparedStatement2 = connection.prepareStatement(sql2);
                    preparedStatement1.setString(2, accountNumber);
                    preparedStatement2.setString(1, accountNumber);
                    preparedStatement1.setString(3, sortCode);
                    preparedStatement2.setString(2, sortCode);

                    PreparedStatement preparedStatement3 = connection.prepareStatement(sql3);
                    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                    preparedStatement3.setString(1, accountNumber);
                    preparedStatement3.setString(2,sortCode);
                    preparedStatement3.setTimestamp(3, timestamp);
                    preparedStatement3.setDouble(4, amount);
                    preparedStatement3.setString(5, "Withdrawal via ATM");
                    preparedStatement3.execute();

                    ResultSet amountSet = preparedStatement2.executeQuery();
                    while (amountSet.next()){
                        double currentBalance = amountSet.getDouble("balance"); 
                        currentBalance += amount;
                        amount = currentBalance;
                    }
                    preparedStatement1.setDouble(1, amount);
                    preparedStatement1.execute();

                    valid = true;
                    scanner.nextLine();
                }
            } while (!valid);
        }
        catch(InputMismatchException e){
            System.out.println("Invalid character provided.");
            scanner.nextLine();
            withdrawal(scanner, connection, accountNumber, sortCode);
        }
        catch (SQLException e) {
            System.out.println("Connection error.");
            e.printStackTrace();
        }
    }

    public static void checkBalance(Connection connection, String accountNumber, String sortCode){
        try{
            String sql = "SELECT currency from accounts WHERE account_number = ? AND sort_code = ?;";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, accountNumber);
            preparedStatement.setString(2, sortCode);
            ResultSet currencySet = preparedStatement.executeQuery();
            String currency = null;
            while(currencySet.next()){
                currency = currencySet.getString("currency");
            }

            String sql2 = "SELECT balance FROM accounts where account_number = ? AND sort_code = ?";
            PreparedStatement preparedStatement2 = connection.prepareStatement(sql2);
            preparedStatement2.setString(1, accountNumber);
            preparedStatement2.setString(2, sortCode);
            ResultSet balanceSet = preparedStatement2.executeQuery();
            String balanceString = null;
            while(balanceSet.next()){
                balanceString = balanceSet.getString("balance");
            }
            System.out.printf("Your balance is: %s %s %n", currency, balanceString);
        }
        catch (SQLException e){
            System.out.println("Connection error.");
            e.printStackTrace();
        }
    }

    public static void transferPayment(Scanner scanner, Connection connection, String accountNumber, String sortCode, String currency, String accountNumberTo, String sortCodeTo){
    try{
        boolean valid = false;
        double amount = 0;
        do {
            System.out.println("Please enter the amount of money you wish to transfer...");
            System.out.printf("%s", currency);
            amount = scanner.nextDouble();
            if(amount < 0.01){
                System.out.println("Invalid amount. Press enter to try again...");
                scanner.nextLine();
            }
            else{
                valid = true;
            }
        } while (!valid);
        scanner.nextLine();
        System.out.println("Please add a memo for this transaction");
        String memo = scanner.nextLine();
        String sql1 = "UPDATE TABLE accounts SET balance = ? WHERE account_number = ? AND sort_code = ?;";
        String sql2 = "SELECT balance FROM accounts WHERE account_number = ? AND sort_code = ?;";
        String sql3 = "INSERT INTO transactions(transaction_uid, account_number, sort_code, transfer_to_sort_code, transfer_to_account_number, time_stamp, amount, memo) VALUES (uuid_generate_v4(), ?, ?, ?, ?, ?, ?, ?);";
                    
        PreparedStatement preparedStatement1 = connection.prepareStatement(sql1);
        PreparedStatement preparedStatement2 = connection.prepareStatement(sql2);
        PreparedStatement preparedStatement3 = connection.prepareStatement(sql3);
        preparedStatement2.setString(1, accountNumber);
        preparedStatement2.setString(2, sortCode);
        ResultSet currentBalanceSet = preparedStatement2.executeQuery();
        double currentBalance = 0;
        while(currentBalanceSet.next()){
            currentBalance = currentBalanceSet.getDouble("balance");
        }
        currentBalance -= amount;
        preparedStatement1.setDouble(1, currentBalance);
        preparedStatement1.setString(2, accountNumber);
        preparedStatement1.setString(3, sortCode);

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        preparedStatement3.setString(1, accountNumber);
        preparedStatement3.setString(2, sortCode);
        preparedStatement3.setString(3, accountNumberTo);
        preparedStatement3.setString(4, sortCodeTo);
        preparedStatement3.setTimestamp(5, timestamp);
        preparedStatement3.setDouble(6, -amount);
        preparedStatement3.setString(7, memo);
        preparedStatement3.execute();

        preparedStatement2.setString(1, accountNumberTo);
        preparedStatement2.setString(2, sortCodeTo);
        currentBalanceSet = preparedStatement2.executeQuery();

        while(currentBalanceSet.next()){
            currentBalance = currentBalanceSet.getDouble("balance");
        }
        currentBalance += amount;
        preparedStatement1.setDouble(1, currentBalance);
        preparedStatement1.setString(2, accountNumberTo);
        preparedStatement1.setString(3, sortCodeTo); 

        preparedStatement3.setString(1, accountNumberTo);
        preparedStatement3.setString(2, sortCodeTo);
        preparedStatement3.setString(3, accountNumber);
        preparedStatement3.setString(4, sortCode);
        preparedStatement3.setTimestamp(5, timestamp);
        preparedStatement3.setDouble(6, amount);
        preparedStatement3.setString(7, memo);
        preparedStatement3.execute();  
        System.out.println("Transfer complete!"); 
    }
    catch (SQLException e){
        System.out.println("Connection error");
        e.printStackTrace();
    }
    catch (InputMismatchException e){
        System.out.println("Invalid amount. Press enter to try again.");
        scanner.nextLine();
        transferPayment(scanner, connection, accountNumber, sortCode, currency, accountNumberTo, sortCodeTo);
    }
    }

    public static void transfer(Scanner scanner, Connection connection, String accountNumber, String sortCode){
        boolean valid = false;
        String sortCodeTo = "";
        String accountNumberTo = "";
        try{
            scanner.nextLine();
            String sql4 = "SELECT currency from accounts WHERE account_number = ? AND sort_code = ?;";
            PreparedStatement preparedStatement4 = connection.prepareStatement(sql4);
            preparedStatement4.setString(1, accountNumber);
            preparedStatement4.setString(2, sortCode);
            ResultSet currencySet = preparedStatement4.executeQuery();
            String currency = null;
            while(currencySet.next()){
                currency = currencySet.getString("currency");
            }

            do {
                System.out.println("Please enter the sort code of the account you intend to transfer money to...");
                sortCodeTo = scanner.nextLine();
                String sql = "SELECT sort_code From banks;";
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                ResultSet numberSet = preparedStatement.executeQuery();
                while(numberSet.next()){
                    String checker = numberSet.getString("sort_code");
                    if(sortCodeTo.equals(checker)){
                        valid = true;
                    }
                }
                if(!valid){
                    System.out.println("This sort code could not be found. Press enter to try again...");
                    scanner.nextLine();
                }
            } while (!valid);
            valid = false;
            do{
                System.out.println("Please enter the account number of the account you intend to transfer money to...");
                accountNumberTo = scanner.nextLine();
                String sql = "SELECT account_number from accounts where sort_code = ?";
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, sortCodeTo);
                ResultSet accountSet = preparedStatement.executeQuery();
                while(accountSet.next()){
                    String checker = accountSet.getString("account_number");
                    if(accountNumberTo.equals(checker)){
                        valid = true;
                        System.out.println("Account number accepted!");
                    }
                }
                if(!valid){
                    System.out.println("This account number could not be found. Press enter to try again...");
                    scanner.nextLine();
                }
            } while (!valid);
            
            transferPayment(scanner, connection, accountNumber, sortCode, currency, accountNumberTo, sortCodeTo);
        }   
        catch (SQLException e) {
            System.out.println("Connection error.");
            e.printStackTrace();
        }
        catch (InputMismatchException e){
            System.out.println("Invalid input. Press enter to try again.");
            scanner.nextLine();

        }
    }

    public static void main(String[] args){
        try { 
            Class.forName("org.postgresql.Driver");
            System.out.println("Connected to bank server!");
            Scanner scanner = new Scanner(System.in);
            while (true){
                Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/bank", "postgres", "Liam");
                String[] details = login(scanner, connection);
                loadMenu(scanner, connection, details[0], details[1]);
            }
        }
        catch (SQLException e) {
            System.out.println("Connection failure.");
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            System.out.println("JDBC Driver not found.");
            e.printStackTrace();
        }
    }
} 