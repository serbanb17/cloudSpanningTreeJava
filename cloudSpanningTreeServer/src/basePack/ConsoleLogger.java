package basePack;

public class ConsoleLogger {
    private final String _msgPrefix;

    public ConsoleLogger(int row, int column){
        _msgPrefix = "[Server " + row + column + "]";
    }

    public void log(String msg){
        System.out.print(_msgPrefix + " " + msg + "\n");
    }

    public void logReceived(String ip, String port, String msg){
        log("Received " + msg + " from ip=" + ip + ", port=" + port);
    }

    public void logStackTrace(Exception e){
        StringBuilder stackTraceStr = new StringBuilder("\n\n" + _msgPrefix + " " + e.toString() + "\n");
        for(StackTraceElement elem : e.getStackTrace()){
            stackTraceStr.append(_msgPrefix).append(" ").append(elem.toString()).append("\n");
        }
        System.out.print(stackTraceStr + "\n\n");
    }
}
