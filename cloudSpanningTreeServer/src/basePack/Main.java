package basePack;

import java.util.Random;

public class Main {
    public static void main(String[] args) {
        int row = 0;
        int column = 0;
        boolean validInputs = true;

        if(args.length == 2) {
            try {
                row = Integer.parseInt(args[0]);
                column = Integer.parseInt(args[1]);
            } catch (Exception ignored) {
                validInputs = false;
            }

            if (row < ConfigVars.minRow || row > ConfigVars.maxRow
                    || column < ConfigVars.minColumn || column > ConfigVars.maxColumn)
                validInputs = false;
        }
        else {
            validInputs = false;
        }

        if(!validInputs){
            String helpMsg = "Invalid parameters!\nprogram row column (1 based index), where row and column bust be integers in range [1,4]\nExample: program 1 2\n";
            System.out.println(helpMsg);
            return;
        }

        ConsoleLogger logger = new ConsoleLogger(row, column);
        CSTServer server = new CSTServer(row, column, logger);

        try {
            server.start();
        } catch (Exception e) {
            logger.logStackTrace(e);
        }
    }

    public static void randomSleep(Random rand){
        try {
            Thread.sleep(rand.nextInt(1000));
        } catch (InterruptedException ignored) {
        }
    }
}
