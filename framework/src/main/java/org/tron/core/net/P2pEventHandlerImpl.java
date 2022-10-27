package org.tron.core.net;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.tron.common.prometheus.MetricKeys;
import org.tron.common.prometheus.Metrics;
import org.tron.consensus.pbft.message.PbftMessage;
import org.tron.core.exception.P2pException;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.PbftMessageFactory;
import org.tron.core.net.message.TronMessage;
import org.tron.core.net.message.TronMessageFactory;
import org.tron.core.net.message.handshake.HelloMessage;
import org.tron.core.net.messagehandler.BlockMsgHandler;
import org.tron.core.net.messagehandler.ChainInventoryMsgHandler;
import org.tron.core.net.messagehandler.FetchInvDataMsgHandler;
import org.tron.core.net.messagehandler.InventoryMsgHandler;
import org.tron.core.net.messagehandler.PbftDataSyncHandler;
import org.tron.core.net.messagehandler.PbftMsgHandler;
import org.tron.core.net.messagehandler.SyncBlockChainMsgHandler;
import org.tron.core.net.messagehandler.TransactionsMsgHandler;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.service.handshake.HandshakeService;
import org.tron.core.net.service.keepalive.KeepAliveService;
import org.tron.p2p.P2pEventHandler;
import org.tron.p2p.connection.Channel;
import org.tron.protos.Protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j(topic = "net")
@Component
public class P2pEventHandlerImpl extends P2pEventHandler {

  private static final String TAG = "~";
  private static final int DURATION_STEP = 50;

  private static List<PeerConnection> activePeers = Collections
          .synchronizedList(new ArrayList<>());

  @Autowired
  private ApplicationContext ctx;

  @Autowired
  private SyncBlockChainMsgHandler syncBlockChainMsgHandler;

  @Autowired
  private ChainInventoryMsgHandler chainInventoryMsgHandler;

  @Autowired
  private InventoryMsgHandler inventoryMsgHandler;

  @Autowired
  private FetchInvDataMsgHandler fetchInvDataMsgHandler;

  @Autowired
  private BlockMsgHandler blockMsgHandler;

  @Autowired
  private TransactionsMsgHandler transactionsMsgHandler;

  @Autowired
  private PbftDataSyncHandler pbftDataSyncHandler;

  @Autowired
  private HandshakeService handshakeService;

  @Autowired
  private PbftMsgHandler pbftMsgHandler;

  @Autowired
  private KeepAliveService keepAliveService;

  public P2pEventHandlerImpl() {
    Set<Byte> set = new HashSet<>();
    for (byte i = 0; i< 127; i++) {
      set.add(i);
    }
    messageTypes = set;
  }

  @Override
  public synchronized void onConnect(Channel c) {
    PeerConnection peerConnection = getPeerConnection(c);
    if (peerConnection == null) {
      peerConnection = ctx.getBean(PeerConnection.class);
      peerConnection.setChannel(c);
      activePeers.add(peerConnection);
      handshakeService.startHandshake(peerConnection);
    }
  }



  @Override
  public synchronized void onDisconnect(Channel c) {
    PeerConnection peerConnection = getPeerConnection(c);
    if (peerConnection != null) {
      activePeers.remove(peerConnection);
      peerConnection.onDisconnect();
    }
  }

  @Override
  public void onMessage(Channel c, byte[] data) {
    PeerConnection peerConnection = getPeerConnection(c);
    if (peerConnection == null) {
      logger.warn("Receive msg from unknown peer {}", c.getInetSocketAddress());
      return;
    }

    if (MessageTypes.PBFT_MSG.asByte() == data[0]) {
      PbftMessage message = null;
      try {
        message = (PbftMessage)PbftMessageFactory.create(data);
        pbftMsgHandler.processMessage(peerConnection, message);
      }catch (Exception e) {
        logger.warn("PBFT Message from {} process failed, {}",
                peerConnection.getInetAddress(), message, e);
        peerConnection.disconnect(Protocol.ReasonCode.BAD_PROTOCOL);
      }
      return;
    }

    processMessage(peerConnection, data);
  }

  private void processMessage(PeerConnection peer, byte[] data) {
    long startTime = System.currentTimeMillis();
    TronMessage msg = null;
    try {
      msg = TronMessageFactory.create(data);
      logger.info("Receive message from  peer: {}, {}",
                peer.getInetAddress(), msg);
      switch (msg.getType()) {
        case P2P_PING:
        case P2P_PONG:
          keepAliveService.processMessage(peer, msg);
          break;
        case P2P_HELLO:
          handshakeService.processHelloMessage(peer, (HelloMessage)msg);
          break;
        case P2P_DISCONNECT:
          peer.getChannel().close();
          break;
        case SYNC_BLOCK_CHAIN:
          syncBlockChainMsgHandler.processMessage(peer, msg);
          break;
        case BLOCK_CHAIN_INVENTORY:
          chainInventoryMsgHandler.processMessage(peer, msg);
          break;
        case INVENTORY:
          inventoryMsgHandler.processMessage(peer, msg);
          break;
        case FETCH_INV_DATA:
          fetchInvDataMsgHandler.processMessage(peer, msg);
          break;
        case BLOCK:
          blockMsgHandler.processMessage(peer, msg);
          break;
        case TRXS:
          transactionsMsgHandler.processMessage(peer, msg);
          break;
        case PBFT_COMMIT_MSG:
          pbftDataSyncHandler.processMessage(peer, msg);
          break;
        default:
          throw new P2pException(P2pException.TypeEnum.NO_SUCH_MESSAGE, msg.getType().toString());
      }
    } catch (Exception e) {
      processException(peer, msg, e);
    } finally {
      long costs = System.currentTimeMillis() - startTime;
      if (costs > 50) {
        logger.info("Message processing costs {} ms, peer: {}, type: {}, time tag: {}",
                costs, peer.getInetAddress(), msg.getType(), getTimeTag(costs));
        Metrics.histogramObserve(MetricKeys.Histogram.MESSAGE_PROCESS_LATENCY,
                costs / Metrics.MILLISECONDS_PER_SECOND, msg.getType().name());
      }
    }
  }

  private void processException(PeerConnection peer, TronMessage msg, Exception ex) {
    Protocol.ReasonCode code;

    if (ex instanceof P2pException) {
      P2pException.TypeEnum type = ((P2pException) ex).getType();
      switch (type) {
        case BAD_TRX:
          code = Protocol.ReasonCode.BAD_TX;
          break;
        case BAD_BLOCK:
          code = Protocol.ReasonCode.BAD_BLOCK;
          break;
        case NO_SUCH_MESSAGE:
        case MESSAGE_WITH_WRONG_LENGTH:
        case BAD_MESSAGE:
          code = Protocol.ReasonCode.BAD_PROTOCOL;
          break;
        case SYNC_FAILED:
          code = Protocol.ReasonCode.SYNC_FAIL;
          break;
        case UNLINK_BLOCK:
          code = Protocol.ReasonCode.UNLINKABLE;
          break;
        case DB_ITEM_NOT_FOUND:
          code = Protocol.ReasonCode.FETCH_FAIL;
          break;
        default:
          code = Protocol.ReasonCode.UNKNOWN;
          break;
      }
      logger.warn("Message from {} process failed, {} \n type: {}, detail: {}",
              peer.getInetAddress(), msg, type, ex.getMessage());
    } else {
      code = Protocol.ReasonCode.UNKNOWN;
      logger.warn("Message from {} process failed, {}",
              peer.getInetAddress(), msg, ex);
    }

    peer.disconnect(code);
  }

  private String getTimeTag(long duration) {
    StringBuilder tag = new StringBuilder(TAG);
    long tagCount = duration / DURATION_STEP;
    for (; tagCount > 0; tagCount--) {
      tag.append(TAG);
    }
    return tag.toString();
  }

  private PeerConnection getPeerConnection(Channel channel) {
    for(PeerConnection peer: new ArrayList<>(activePeers)) {
      if (peer.getChannel().equals(channel)) {
        return peer;
      }
    }
    return null;
  }

  public static List<PeerConnection> getPeers() {
    List<PeerConnection> peers = Lists.newArrayList();
    for (PeerConnection peer : new ArrayList<>(activePeers)) {
      if (!peer.isDisconnect()) {
        peers.add(peer);
      }
    }
    return peers;
  }

  public synchronized static void sortPeers(){
    activePeers.sort(Comparator.comparingDouble(c -> c.getChannel().getLatency()));
  }
}
