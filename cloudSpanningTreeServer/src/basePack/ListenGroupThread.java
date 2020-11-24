package basePack;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ListenGroupThread extends Thread {
    private final int _row, _column;
    private final ConcurrentHashMap<Integer, AtomicReference<LinkStatus>> _links;
    private final AtomicBoolean _isPartOfSpanningTree;
    private final ConsoleLogger _logger;

    public ListenGroupThread(int row, int column, ConcurrentHashMap<Integer, AtomicReference<LinkStatus>> links, AtomicBoolean isPartOfSpanningTree, ConsoleLogger logger) {
        _row = row;
        _column = column;
        _links = links;
        _isPartOfSpanningTree = isPartOfSpanningTree;
        _logger = logger;
    }

    public void run() {
        Random rand = new Random();
        MulticastSocket socket = null;
        try {
            InetAddress group = InetAddress.getByName(ConfigVars.groupIp);
            int port = ConfigVars.groupPortTemplate + _row * 10 + _column;
            socket = new MulticastSocket(port);
            socket.joinGroup(group);
            byte[] buff = new byte[256];
            DatagramPacket packet = new DatagramPacket(buff, buff.length);

            while(!_isPartOfSpanningTree.get()) {
                socket.setSoTimeout(1005);
                try {
                    socket.receive(packet);
                    String msg = new String(packet.getData(), packet.getOffset(), packet.getLength());
                    if(msg.equals("ping") && _isPartOfSpanningTree.compareAndSet(false, true)) {
                        _links.get(_row * 10 + _column).set(LinkStatus.ESTABLISHED);
                        _logger.log("Received ping from " + (_row * 10 + _column));
                        Main.randomSleep(rand);
                        synchronized (_isPartOfSpanningTree) {
                            _isPartOfSpanningTree.notifyAll();
                        }
                    }
                } catch (SocketTimeoutException ignored) {
                }
            }
        } catch (Exception e) {
            _logger.logStackTrace(e);
        } finally {
            if(socket != null)
                socket.close();
        }
        _logger.log("Exit ListenGroupThread " + (_row * 10 + _column));
    }
}
