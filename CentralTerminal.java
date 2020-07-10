import java.sql.*;
import java.util.InputMismatchException;
import java.util.Scanner;

class CentralTerminal {

    private static String getCurrency(Connection connection, String accountNumber, String sortCode){
        String currency = "";
        try{
            String sql = "SELECT currency from accounts WHERE account_number = ? AND sort_code = ?;";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, accountNumber);
            preparedStatement.setString(2, sortCode);
            ResultSet currencySet = preparedStatement.executeQuery();
            while(currencySet.next()){
                currency = currencySet.getString("currency");
            }
        }
        catch(SQLException e){
            System.out.println("SQL Error. See stack trace for more information");
            e.printStackTrace();
        }
        return currency;
    }

    private static boolean checkSortCode(String sortCode, Connection connection){
        boolean valid = false;
        try{
        Statement statement = connection.createStatement();
        ResultSet bankSet = statement.executeQuery("SELECT sort_code from banks");
        while(bankSet.next()){
            String checker = bankSet.getString("sort_code");
            if(sortCode.equals(checker)){
                valid = true;
                }
            } 
        }
        catch (SQLException e){
            System.out.println("SQL error. See stack trace for more details.");
            e.printStackTrace();
        }
        return valid;
    }

    private static boolean checkAccount(String sortCode, String accountNumber, Connection connection){
        boolean valid = false;
        try{  
            String sql = "SELECT account_number from accounts where sort_code = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, sortCode);
            ResultSet accountSet = preparedStatement.executeQuery();
            while(accountSet.next()){
                String checker = accountSet.getString("account_number");
                if(accountNumber.equals(checker)){
                    valid = true;
                }
            }
        }
        catch (SQLException e){
            System.out.println("SQL error. See stack trace for more information.");
            e.printStackTrace();
        }
        return valid;
    }
    
    private static boolean checkPin(String pin, Connection connection, String accountNumber, String sortCode){
        boolean valid = false;
        try{
            String sql = "SELECT pin from accounts where sort_code = ? AND account_number = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1,sortCode);
            preparedStatement.setString(2,accountNumber);
            ResultSet accountSet = preparedStatement.executeQuery();
            while(accountSet.next()){
                String checker = accountSet.getString("pin");
                if(pin.equals(checker)){
                    valid = true;
                }
            }
        }
        catch(SQLException e){
            System.out.println("SQL error. See stack trace for more information.");
            e.printStackTrace();
        }
        return valid;
    }

    private static String[] login(Scanner scanner, Connection connection){
        String[] details = new String[2]; 
        boolean valid = false;
        String sortCode= null;
        String accountNumber = null;
        do{
            System.out.println("Please enter your sort code. Do not include any dashes...");
            sortCode = scanner.nextLine();
            valid = checkSortCode(sortCode, connection);
            if(valid){
                System.out.println("Sort code accepted!");
            }
            else{
                System.out.println("This sort code could not be found. Press enter to try again...");
                scanner.nextLine();
            }
        } while (!valid);
        valid = false;
        do {
            System.out.println("Please enter your account number...");
            accountNumber = scanner.nextLine();
            valid = checkAccount(sortCode, accountNumber, connection);
            if(valid){        
                System.out.println("Account number accepted!");
                }
            else{
                System.out.println("This account number could not be found. Press enter to try again...");
                scanner.nextLine();
            }
        } while (!valid);
        valid = false; 
        do {
            System.out.println("Please enter your pin...");
            String pin = scanner.nextLine();
            valid = checkPin(pin, connection, accountNumber, sortCode);
            if(valid){
                System.out.println("Pin accepted!");
            }
            else{
                System.out.println("Invalid pin. Press enter to try again...");
                scanner.nextLine();
            }
        } while (!valid);
        details[0] = accountNumber;
        details[1] = sortCode;
        return details;
    }

    private static void loadMenu(Scanner scanner, Connection connection, String accountNumber, String sortCode){
        int choice = 0;
        try{
            do {
                System.out.println("Please type the number corresponding to what you would like to do...");
                System.out.println("1. Make a deposit.           2. Make a withdrawal.");
                System.out.println("3. Check you balance.        4. Make a transfer.");
                System.out.println("5. Show transaction history. 6. Log out.");
                choice = scanner.nextInt();

                if (choice <1 || choice > 6) {
                    System.out.println("Invalid choice. Please choose an option between 1-6.");
                    scanner.nextLine();
                    break;
                } 
            } while (choice < 1 || choice > 6);

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
                scanner.nextLine();
                transfer(scanner, connection, accountNumber, sortCode);
                break;

                case 5:
                scanner.nextLine();
                viewTransactions(connection, accountNumber, sortCode);
                break;

                case 6:
                System.out.println("Thank you, logging out!");
                scanner.nextLine();
                break;
            }

            if (choice !=6) {
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

    private static void createTransferRecords(Connection connection, String accountNumber, String sortCode, String accountNumberTo, String sortCodeTo, String memo, double amount){
        try{
            String sql = "INSERT INTO transactions(transaction_uid, account_number, sort_code, transfer_to_sort_code, transfer_to_account_number, time_stamp, amount, memo) VALUES (uuid_generate_v4(), ?, ?, ?, ?, ?, ?, ?);";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());

            preparedStatement.setString(1, accountNumber);
            preparedStatement.setString(2, sortCode);
            preparedStatement.setString(3, accountNumberTo);
            preparedStatement.setString(4, sortCodeTo);
            preparedStatement.setTimestamp(5, timestamp);
            preparedStatement.setDouble(6, -amount);
            preparedStatement.setString(7, memo);
            preparedStatement.execute();

            preparedStatement.setString(1, accountNumberTo);
            preparedStatement.setString(2, sortCodeTo);
            preparedStatement.setString(3, accountNumber);
            preparedStatement.setString(4, sortCode);
            preparedStatement.setTimestamp(5, timestamp);
            preparedStatement.setDouble(6, amount);
            preparedStatement.setString(7, memo);
            preparedStatement.execute();

        }
        catch (SQLException e){
            System.out.println("SQL error. See stack trace for more information.");
            e.printStackTrace();
        }
    }

    private static void createOneWayRecord(String sortCode, String accountNumber, Connection connection, double amount){
        try{
            String memo = "";
            if(amount <0){
                memo = "Withdrawal via ATM.";
            }
            else{
                memo = "Deposit via ATM.";
            }
            String sql = "INSERT INTO transactions(transaction_uid, account_number, sort_code, time_stamp, amount, memo) VALUES (uuid_generate_v4(), ?, ?, ?, ?, ?);";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            preparedStatement.setString(1, accountNumber);
            preparedStatement.setString(2,sortCode);
            preparedStatement.setTimestamp(3, timestamp);
            preparedStatement.setDouble(4, amount);
            preparedStatement.setString(5, memo);
            preparedStatement.execute();
        }
        catch (SQLException e){
            System.out.println("SQL error. See stack trace for more information.");
            e.printStackTrace();
        }
    }

    private static double getBalance(Connection connection, String sortCode, String accountNumber){
        double balance = 0;
        try{
            String sql2 = "SELECT balance from accounts WHERE account_number = ? AND sort_code = ?;";
            PreparedStatement preparedStatement2 = connection.prepareStatement(sql2);
            preparedStatement2.setString(1, accountNumber);
            preparedStatement2.setString(2, sortCode);
            ResultSet amountSet = preparedStatement2.executeQuery();
            while(amountSet.next()){
                balance = amountSet.getDouble("balance");
            }
        }
        catch (SQLException e){
            System.out.println("SQL error. See stack trace for more information.");
            e.printStackTrace();
        }
        return balance;
    }

    private static void alterBalance(Connection connection, String sortCode, String accountNumber, double currentBalance){
        try{
            String sql = "UPDATE accounts SET balance = ? WHERE account_number = ? AND sort_code = ?;";
            PreparedStatement preparedStatement1 = connection.prepareStatement(sql);
            preparedStatement1.setDouble(1, currentBalance);
            preparedStatement1.setString(2, accountNumber);
            preparedStatement1.setString(3, sortCode);
            preparedStatement1.execute();
        }
        catch (SQLException e){
            System.out.println("SQL error. See stack trace for more information.");
            e.printStackTrace();
        }
    }  

    public static void deposit(Scanner scanner, Connection connection, String accountNumber, String sortCode){
        try{
            String currency = getCurrency(connection, accountNumber, sortCode);

            boolean valid = false;
            do {
                System.out.println("Please enter the amount you would like to deposit...");
                System.out.printf("%s ", currency);
                double amount = scanner.nextDouble();
                double currentBalance = 0;
                if(amount < 0.01){
                    System.out.println("Please enter a valid amount.");
                    scanner.nextLine();
                }
                else{
                        currentBalance = getBalance(connection, sortCode, accountNumber);
                        currentBalance += amount;
                    }
                    alterBalance(connection, sortCode, accountNumber, currentBalance);
                    createOneWayRecord(sortCode, accountNumber, connection, amount);
                    valid = true;
                    scanner.nextLine();
            } while (!valid);
        }
        catch(InputMismatchException e){
            System.out.println("Invalid character provided.");
            scanner.nextLine();
            deposit(scanner, connection, accountNumber, sortCode);
        }
    }

    public static void withdrawal(Scanner scanner, Connection connection, String accountNumber, String sortCode){
        try{
            String currency = getCurrency(connection, accountNumber, sortCode);
            boolean valid = false;
            do {
                System.out.println("Please enter the amount you would like to withdraw...");
                System.out.printf("%s ", currency);
                double amount = scanner.nextDouble();
                double currentBalance = 0;
                if(amount < 0.01){
                    System.out.println("Invalid input.");
                    scanner.nextLine();
                }
                else{
                    amount = - amount;
                    currentBalance = getBalance(connection, sortCode, accountNumber);
                    currentBalance += amount;
                }
                    alterBalance(connection, sortCode, accountNumber, currentBalance);
                    createOneWayRecord(sortCode, accountNumber, connection, amount);
                    valid = true;
                    scanner.nextLine();
            } while (!valid);
        }
        catch(InputMismatchException e){
            System.out.println("Invalid character provided.");
            scanner.nextLine();
            withdrawal(scanner, connection, accountNumber, sortCode);
        }
    }

    public static void checkBalance(Connection connection, String accountNumber, String sortCode){
        String currency = getCurrency(connection, accountNumber, sortCode);
        double balanceString = getBalance(connection, sortCode, accountNumber);
        System.out.printf("Your balance is: %s %s %n", currency, balanceString);
    }

    public static void transferPayment(Scanner scanner, Connection connection, String accountNumber, String sortCode, String currency, String accountNumberTo, String sortCodeTo){
    try{
        boolean valid = false;
        double amount = 0;
        do {
            System.out.println("Please enter the amount of money you wish to transfer...");
            System.out.printf("%s ", currency);
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
        
        double currentBalance = getBalance(connection, sortCode, accountNumber);
        currentBalance -= amount;
        alterBalance(connection, sortCode, accountNumber, currentBalance);

        currentBalance = getBalance(connection, sortCodeTo, accountNumberTo);
        currentBalance += amount;
        alterBalance(connection, sortCodeTo, accountNumberTo, currentBalance);

        createTransferRecords(connection, accountNumber, sortCode, accountNumberTo, sortCodeTo, memo, amount);

        System.out.println("Transfer complete!"); 
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
            
            String currency = getCurrency(connection, accountNumber, sortCode);
            do {
                System.out.println("Please enter the sort code of the account you intend to transfer money to...");
                sortCodeTo = scanner.nextLine();
                valid = checkSortCode(sortCodeTo, connection);
                if(!valid){
                    System.out.println("This sort code could not be found. Press enter to try again...");
                    scanner.nextLine();
                }
                else{
                    System.out.println("Sort code accepted!");
                }
            } while (!valid);
            valid = false;
            do{
                System.out.println("Please enter the account number of the account you intend to transfer money to...");
                accountNumberTo = scanner.nextLine();
                valid = checkAccount(sortCodeTo, accountNumberTo, connection);
                if(valid){    
                    System.out.println("Account number accepted!");
                }
                else{
                    System.out.println("This account number could not be found. Press enter to try again...");
                    scanner.nextLine();
                }
            } while (!valid);
            
            transferPayment(scanner, connection, accountNumber, sortCode, currency, accountNumberTo, sortCodeTo);
        }
        catch (InputMismatchException e){
            System.out.println("Invalid input. Press enter to try again.");
            scanner.nextLine();
        }
    }

    public static void viewTransactions(Connection connection, String accountNumber, String sortCode){
        String currency = getCurrency(connection, accountNumber, sortCode);
        try{
            String sql = "SELECT * FROM transactions WHERE account_number = ? AND sort_code = ?;";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, accountNumber);
            preparedStatement.setString(2, sortCode);
            ResultSet resultSet = preparedStatement.executeQuery();
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            int columnNumber = resultSetMetaData.getColumnCount();
            for (int i = 1; i < columnNumber + 1; ++i){
                System.out.print(resultSetMetaData.getColumnName(i) + " | ");
            }
            System.out.println();
            while(resultSet.next()){
                for (int i = 1; i<columnNumber + 1; ++i){
                    String columnValue = resultSet.getString(i);
                    if(columnValue == null){
                        columnValue = "    ";
                    }
                    if("amount".equals(resultSetMetaData.getColumnName(i))){
                        System.out.print(currency + " " + columnValue + " | ");
                    }
                    else{
                        System.out.print(columnValue + " | ");
                    }
                }
                System.out.println();
            }
        }
        catch(SQLException e){
            System.out.println("SQL Error. See stack trace for details.");
            e.printStackTrace();
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
            System.out.println("Connection error. See stack trace for more information.");
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            System.out.println("JDBC Driver not found.");
            e.printStackTrace();
        }
    }
} 