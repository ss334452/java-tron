package org.tron.core.net.service.statistics;

import lombok.Getter;
import org.tron.protos.Protocol;

public class NodeStatistics {
  @Getter
  private Protocol.ReasonCode remoteDisconnectReason = null;
  @Getter
  private Protocol.ReasonCode localDisconnectReason = null;
  @Getter
  private int disconnectTimes = 0;
  private long lastDisconnectedTime = 0;
  private long firstDisconnectedTime = 0;
  private long start = System.currentTimeMillis();

  public Protocol.ReasonCode getDisconnectReason() {
    if (localDisconnectReason != null) {
      return localDisconnectReason;
    }
    if (remoteDisconnectReason != null) {
      return remoteDisconnectReason;
    }
    return Protocol.ReasonCode.UNKNOWN;
  }

  public void nodeDisconnectedRemote(Protocol.ReasonCode reason) {
    remoteDisconnectReason = reason;
    notifyDisconnect();
  }

  public void nodeDisconnectedLocal(Protocol.ReasonCode reason) {
    localDisconnectReason = reason;
    notifyDisconnect();
  }

  private void notifyDisconnect() {
    lastDisconnectedTime = System.currentTimeMillis();
    if (firstDisconnectedTime == 0) {
      firstDisconnectedTime = lastDisconnectedTime;
    }
    disconnectTimes++;
  }

  @Override
  public String toString() {
    return new StringBuilder()
            .append("time:").append(System.currentTimeMillis() - start)
            .append(", disconnectTimes:").append(disconnectTimes)
            .append(", localReason:").append(localDisconnectReason)
            .append(", remoteReason:").append(remoteDisconnectReason).toString();
  }

//  public class SimpleStatter {
//
//    private long sum;
//    @Getter
//    private long count;
//    @Getter
//    private long last;
//    @Getter
//    private long min;
//    @Getter
//    private long max;
//
//    public void add(long value) {
//      last = value;
//      sum += value;
//      min = min == 0 ? value : Math.min(min, value);
//      max = Math.max(max, value);
//      count++;
//    }
//
//    public long getAvg() {
//      return count == 0 ? 0 : sum / count;
//    }
//  }

}
