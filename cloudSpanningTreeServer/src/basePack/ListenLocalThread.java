package basePack;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ListenLocalThread extends Thread {
    private final int _row, _column;
    private final ConcurrentHashMap<Integer, AtomicReference<LinkStatus>> _links;
    private final AtomicBoolean _isPartOfSpanningTree;
    private final ConsoleLogger _logger;

    public ListenLocalThread(int row, int column, ConcurrentHashMap<Integer, AtomicReference<LinkStatus>> links, AtomicBoolean isPartOfSpanningTree, ConsoleLogger logger) {
        _row = row;
        _column = column;
        _links = links;
        _isPartOfSpanningTree = isPartOfSpanningTree;
        _logger = logger;
    }

    public void run() {
        DatagramSocket socket = null;
        try{
            InetAddress myIp = InetAddress.getByName(ConfigVars.nodeIp);
            int port = ConfigVars.nodeListenPortTemplate + _row * 10 + _column;
            socket = new DatagramSocket(port, myIp);
            byte[] buff = new byte[256];
            DatagramPacket packet = new DatagramPacket(buff, buff.length);
            _logger.log("Waiting for message on ip="+myIp+", port="+port);
            boolean receivedStopServer;
            do {
                socket.receive(packet);
                 receivedStopServer = handleRequest(socket, packet);
            } while (!receivedStopServer);
        }catch (Exception e){
            _logger.logStackTrace(e);
        }finally {
            if(socket != null)
                socket.close();
        }
    }

    private boolean handleRequest(DatagramSocket socket, DatagramPacket packet) throws IOException {
        String msg = new String(packet.getData(), packet.getOffset(), packet.getLength());
        int nodeRowCol = packet.getPort() % 100;
        switch (msg) {
            case "pong":
                if (_isPartOfSpanningTree.get()) {
                    if(_links.get(nodeRowCol).get() == LinkStatus.UNKNOWN)
                        _logger.logReceived(packet.getAddress().toString(), "" + packet.getPort(), msg);
                    _links.get(nodeRowCol).compareAndSet(LinkStatus.UNKNOWN, LinkStatus.ESTABLISHED);
                }
                break;
            case "doNotPing":
                if (_isPartOfSpanningTree.get()) {
                    if(_links.get(nodeRowCol).get() == LinkStatus.UNKNOWN)
                        _logger.logReceived(packet.getAddress().toString(), "" + packet.getPort(), msg);
                    _links.get(nodeRowCol).compareAndSet(LinkStatus.UNKNOWN, LinkStatus.REFUSED);
                }
                break;
            case "startBuildingSpanningTree":
                if (!_isPartOfSpanningTree.get())
                    synchronized (_isPartOfSpanningTree) {
                        _isPartOfSpanningTree.notify();
                    }
                _logger.logReceived(packet.getAddress().toString(), "" + packet.getPort(), msg);
                break;
            case "getLinks":
                boolean allLinksKnown = true;
                List<Integer> neighbours = Collections.list(_links.keys());
                StringBuilder links = new StringBuilder();
                for (Integer neighbour : neighbours) {
                    if (_links.get(neighbour).get() == LinkStatus.UNKNOWN)
                        allLinksKnown = false;
                    else if (_links.get(neighbour).get() == LinkStatus.ESTABLISHED)
                        links.append(neighbour);
                }
                byte[] buff;
                if (allLinksKnown)
                    buff = links.toString().getBytes();
                else
                    buff = "working".getBytes();
                DatagramPacket responsePacket = new DatagramPacket(buff, buff.length, packet.getAddress(), packet.getPort());
                socket.send(responsePacket);
                _logger.logReceived(packet.getAddress().toString(), "" + packet.getPort(), msg);
                break;
        }
        return msg.equals("stopServer");
    }
}
