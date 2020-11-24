package basePack;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class CSTServer {
    private final int _row, _column;
    private final ConcurrentHashMap<Integer, AtomicReference<LinkStatus>> _links;
    private final AtomicBoolean _isPartOfSpanningTree;
    private final ConsoleLogger _logger;

    CSTServer(int row, int column, ConsoleLogger logger) {
        _row = row;
        _column = column;
        _links = new ConcurrentHashMap<>();
        _isPartOfSpanningTree = new AtomicBoolean(false);
        _logger = logger;
    }

    public void start() throws Exception {
        _logger.log("Server started");

        Thread[] listenGroupThreads = new Thread[4];
        Thread listenLocalThread, sendLocalThread;
        listenLocalThread = new ListenLocalThread(_row, _column, _links, _isPartOfSpanningTree, _logger);

        if(_row > ConfigVars.minRow) {
            _links.put((_row - 1) * 10 + (_column), new AtomicReference<>(LinkStatus.UNKNOWN));
            listenGroupThreads[0] = new ListenGroupThread(_row - 1, _column, _links, _isPartOfSpanningTree, _logger);
        }
        if(_column > ConfigVars.minColumn) {
            _links.put((_row) * 10 + (_column - 1), new AtomicReference<>(LinkStatus.UNKNOWN));
            listenGroupThreads[1] = new ListenGroupThread(_row, _column - 1, _links, _isPartOfSpanningTree, _logger);
        }
        if(_row < ConfigVars.maxRow) {
            _links.put((_row + 1) * 10 + (_column), new AtomicReference<>(LinkStatus.UNKNOWN));
            listenGroupThreads[2] = new ListenGroupThread(_row + 1, _column, _links, _isPartOfSpanningTree, _logger);
        }
        if(_column < ConfigVars.maxColumn) {
            _links.put((_row) * 10 + (_column + 1), new AtomicReference<>(LinkStatus.UNKNOWN));
            listenGroupThreads[3] = new ListenGroupThread(_row , _column + 1, _links, _isPartOfSpanningTree, _logger);
        }

        listenLocalThread.start();
        for(int i=0; i<4; i++)
            if(listenGroupThreads[i] != null)
                listenGroupThreads[i].start();

        sendLocalThread = new SendLocalThread(_row, _column, _links, _isPartOfSpanningTree, _logger);
        sendLocalThread.start();

        sendLocalThread.join();
        listenLocalThread.join();
        for(int i=0; i<4; i++)
            if(listenGroupThreads[i] != null)
                listenGroupThreads[i].join();
        _logger.log("Server closed");
    }
}

enum LinkStatus {UNKNOWN, REFUSED, ESTABLISHED}