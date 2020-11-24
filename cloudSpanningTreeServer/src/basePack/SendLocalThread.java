package basePack;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class SendLocalThread extends Thread {
    private final int _row, _column;
    private final ConcurrentHashMap<Integer, AtomicReference<LinkStatus>> _links;
    private final AtomicBoolean _isPartOfSpanningTree;
    private final ConsoleLogger _logger;

    public SendLocalThread(int row, int column, ConcurrentHashMap<Integer, AtomicReference<LinkStatus>> links, AtomicBoolean isPartOfSpanningTree, ConsoleLogger logger) {
        _row = row;
        _column = column;
        _links = links;
        _isPartOfSpanningTree = isPartOfSpanningTree;
        _logger = logger;
    }

    public void run(){
        Random rand = new Random();
        DatagramSocket socket = null;
        try {
            synchronized (_isPartOfSpanningTree) {
                _isPartOfSpanningTree.wait();
            }
            _logger.log("Connected to spanning tree");
            InetAddress myIp = InetAddress.getByName(ConfigVars.nodeIp);
            int myPort = ConfigVars.nodeSendPortTemplate + _row * 10 + _column;
            InetAddress groupIp = InetAddress.getByName(ConfigVars.groupIp);
            int groupPort = ConfigVars.groupPortTemplate + _row * 10 + _column;
            socket = new DatagramSocket(myPort, myIp);
            boolean allLinksKnown;

            do {
                allLinksKnown = true;
                List<Integer> neighbours = Collections.list(_links.keys());
                for (Integer neighbour : neighbours) {
                    allLinksKnown &= _links.get(neighbour).get() != LinkStatus.UNKNOWN;
                    InetAddress ipToSend = InetAddress.getByName(ConfigVars.nodeIp);
                    int portToSend = ConfigVars.nodeListenPortTemplate + neighbour;
                    if (_links.get(neighbour).get() == LinkStatus.ESTABLISHED) {
                        byte[] pongBuff = "pong".getBytes();
                        DatagramPacket pongPacket = new DatagramPacket(pongBuff, pongBuff.length, ipToSend, portToSend);
                        socket.send(pongPacket);

                    } else {
                        byte[] doNotPingBuff = "doNotPing".getBytes();
                        DatagramPacket doNotPingPacket = new DatagramPacket(doNotPingBuff, doNotPingBuff.length, ipToSend, portToSend);
                        socket.send(doNotPingPacket);
                    }
                }
                byte[] pingBuff = "ping".getBytes();
                DatagramPacket pingPacket = new DatagramPacket(pingBuff, pingBuff.length, groupIp, groupPort);
                socket.send(pingPacket);
                LogRemainingLinks(neighbours);
                Main.randomSleep(rand);
            } while (!allLinksKnown);
        } catch (Exception e){
            _logger.logStackTrace(e);
        }finally {
            if (socket != null)
                socket.close();
        }

        _logger.log("Exit SendLocalThread");
    }

    private void LogRemainingLinks(List<Integer> neighbours){
        StringBuilder left = new StringBuilder();
        for(Integer neighbour:neighbours){
            if(_links.get(neighbour).get() == LinkStatus.UNKNOWN)
                left.append(neighbour).append(" ");
        }
        _logger.log("Remaining links to set: " + left);
    }
}
