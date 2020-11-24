package basePack;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {
        int row = 0;
        int column = 0;
        boolean validInputs = true;

        if (args.length == 2) {
            try {
                row = Integer.parseInt(args[0]);
                column = Integer.parseInt(args[1]);
            } catch (Exception ignored) {
                validInputs = false;
            }

            if (row < ConfigVars.minRow || row > ConfigVars.maxRow
                    || column < ConfigVars.minColumn || column > ConfigVars.maxColumn)
                validInputs = false;
        } else {
            validInputs = false;
        }

        if (!validInputs) {
            String helpMsg = "Invalid parameters!\nprogram row column (1 based index), where row and column bust be integers in range [1,4]\nExample: program 1 2\n";
            logConsole(helpMsg);
            return;
        }

        logConsole("Started");

        DatagramSocket socket = null;
        try {
            InetAddress myIp = InetAddress.getByName(ConfigVars.nodeIp);
            int myPort = ConfigVars.nodePortTemplate + 1;
            socket = new DatagramSocket(myPort, myIp);
            socket.setSoTimeout(5 * 1000);

            startBuildSpanningTree(socket, row, column);
            ArrayList<Integer[]>[][] adjacencyMatrix = getAdjacencyMatrix(socket);
            drawConnections(adjacencyMatrix);
            stopServers(socket);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (socket != null)
                socket.close();
        }

        logConsole("Exit");
    }

    private static void startBuildSpanningTree(DatagramSocket socket, int row, int column) throws InterruptedException, IOException {
        InetAddress rootNodeIp = InetAddress.getByName(ConfigVars.nodeIp);
        int rootNodePort = ConfigVars.nodePortTemplate + row * 10 + column;
        byte[] buff = "startBuildingSpanningTree".getBytes();
        DatagramPacket packet = new DatagramPacket(buff, buff.length, rootNodeIp, rootNodePort);
        logConsole("Request to start building spanning tree from node " + row + column);
        for(int i=0;i<5;i++)
            socket.send(packet);
        Thread.sleep(500);
    }

    private static ArrayList<Integer[]>[][] getAdjacencyMatrix(DatagramSocket socket) throws IOException {
        boolean tryAgain;
        ArrayList<Integer[]>[][] adjacencyMatrix;
        do {
            logConsole("Trying to build adjacency matrix");
            tryAgain = false;
            adjacencyMatrix = new ArrayList[ConfigVars.maxRow + 1][ConfigVars.maxColumn + 1];
            for (int i = ConfigVars.minRow; i <= ConfigVars.maxRow && !tryAgain; i++)
                for (int j = ConfigVars.minColumn; j <= ConfigVars.maxColumn && !tryAgain; j++) {
                    if(adjacencyMatrix[i][j] != null)
                        continue;;

                    InetAddress nodeIp = InetAddress.getByName(ConfigVars.nodeIp);
                    int nodePort = ConfigVars.nodePortTemplate + i * 10 + j;
                    byte[] reqBuff = "getLinks".getBytes();
                    DatagramPacket requestPacket = new DatagramPacket(reqBuff, reqBuff.length, nodeIp, nodePort);
                    logConsole("Request adjacency list from node " + i + j);
                    socket.send(requestPacket);

                    byte[] respBuff = new byte[256];
                    DatagramPacket respPacket = new DatagramPacket(respBuff, respBuff.length);
                    try {
                        logConsole("Waiting to receive adjacency list from node " + i + j);
                        socket.receive(respPacket);
                        String msg = new String(respPacket.getData(), respPacket.getOffset(), respPacket.getLength());
                        if (msg.equals("working")) {
                            logConsole("Node " + i + j + " hasn't completed its adjacency list");
                            tryAgain = true;
                        } else {
                            adjacencyMatrix[i][j] = parseResponseList(msg);
                            StringBuilder logMsg = new StringBuilder("Adjacency list received from node " + i + j + ": ");
                            for (Integer[] nodeCoords : adjacencyMatrix[i][j])
                                logMsg.append(nodeCoords[0]).append(nodeCoords[1]).append(" ");
                            logConsole(logMsg.toString());
                        }

                    } catch (SocketTimeoutException ignored) {
                        logConsole("Timeout on receiving response from node " + i + j);
                        tryAgain = true;
                    }
                }
        } while (tryAgain);
        return adjacencyMatrix;
    }

    private static ArrayList<Integer[]> parseResponseList(String list) {
        ArrayList<Integer[]> response = new ArrayList<>();
        for (int i = 0; i < list.length() / 2; i++) {
            int row = Integer.parseInt(list.substring(i * 2, i * 2 + 1));
            int col = Integer.parseInt(list.substring(i * 2 + 1, i * 2 + 2));
            response.add(new Integer[]{row, col});
        }
        return response;
    }

    private static void drawConnections(ArrayList<Integer[]>[][] adjacencyMatrix) {
        System.out.print("\n\n");
        logConsole("Connections of server nodes\n");
        int spacing = 2;
        for (int i = ConfigVars.minRow; i <= ConfigVars.maxRow * spacing - spacing + 1; i++) {
            for (int j = ConfigVars.minColumn; j <= ConfigVars.maxColumn * spacing - spacing + 1; j++)
                if ((i - ConfigVars.minRow) % spacing == 0 && (j - ConfigVars.minColumn) % spacing == 0) // is node
                    System.out.print("X");
                else if ((i - ConfigVars.minRow) % spacing == 0
                        && areLinked(adjacencyMatrix,
                        (i - ConfigVars.minRow) / spacing + 1,
                        (j - ConfigVars.minColumn) / spacing + 1,
                        (i - ConfigVars.minRow) / spacing + 1,
                        (j - ConfigVars.minColumn) / spacing + 1 + 1)) // is on row of two connected nodes
                    System.out.print("--");
                else if ((j - ConfigVars.minColumn) % spacing == 0
                        && areLinked(adjacencyMatrix,
                        (i - ConfigVars.minRow) / spacing + 1,
                        (j - ConfigVars.minColumn) / spacing + 1,
                        (i - ConfigVars.minRow) / spacing + 1 + 1,
                        (j - ConfigVars.minColumn) / spacing + 1)) // is on column of two connected nodes
                    System.out.print("|");
                else if ((j - ConfigVars.minColumn) % spacing == 0) // is on column of two unconnected nodes
                    System.out.print(" ");
                else
                    System.out.print("  ");
            System.out.print("\n");
        }
        System.out.print("\n\n\n");
    }

    private static boolean areLinked(ArrayList<Integer[]>[][] adjacencyMatrix, int i1, int j1, int i2, int j2) {
        for (Integer[] nodeCoords1 : adjacencyMatrix[i1][j1])
            if (nodeCoords1[0] == i2 && nodeCoords1[1] == j2)
                for (Integer[] nodeCoords2 : adjacencyMatrix[i2][j2])
                    if (nodeCoords2[0] == i1 && nodeCoords2[1] == j1)
                        return true;
        return false;
    }

    private static void stopServers(DatagramSocket socket) throws IOException {
        for (int i = ConfigVars.minRow; i <= ConfigVars.maxRow; i++)
            for (int j = ConfigVars.minColumn; j <= ConfigVars.maxColumn; j++) {
                InetAddress nodeIp = InetAddress.getByName(ConfigVars.nodeIp);
                int nodePort = ConfigVars.nodePortTemplate + i * 10 + j;
                byte[] reqBuff = "stopServer".getBytes();
                DatagramPacket requestPacket = new DatagramPacket(reqBuff, reqBuff.length, nodeIp, nodePort);
                logConsole("Send stopServer to " + i + j);
                socket.send(requestPacket);
            }
    }

    private static void logConsole(String msg) {
        System.out.println("[Client] " + msg);
    }
}
