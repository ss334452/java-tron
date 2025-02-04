package org.tron.core.net;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.message.Message;
import org.tron.common.overlay.server.ChannelManager;
import org.tron.common.prometheus.MetricKeys;
import org.tron.common.prometheus.Metrics;
import org.tron.core.exception.P2pException;
import org.tron.core.exception.P2pException.TypeEnum;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.net.message.TransactionMessage;
import org.tron.core.net.message.TronMessage;
import org.tron.core.net.messagehandler.BlockMsgHandler;
import org.tron.core.net.messagehandler.ChainInventoryMsgHandler;
import org.tron.core.net.messagehandler.FetchInvDataMsgHandler;
import org.tron.core.net.messagehandler.InventoryMsgHandler;
import org.tron.core.net.messagehandler.PbftDataSyncHandler;
import org.tron.core.net.messagehandler.SyncBlockChainMsgHandler;
import org.tron.core.net.messagehandler.TransactionsMsgHandler;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerStatusCheck;
import org.tron.core.net.service.AdvService;
import org.tron.core.net.service.FetchBlockService;
import org.tron.core.net.service.SyncService;
import org.tron.protos.Protocol.ReasonCode;

@Slf4j(topic = "net")
@Component
public class TronNetService {

  @Autowired
  private ChannelManager channelManager;

  @Autowired
  private AdvService advService;

  @Autowired
  private SyncService syncService;

  @Autowired
  private PeerStatusCheck peerStatusCheck;

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
  private FetchBlockService fetchBlockService;

  private static final String TAG = "~";
  private static final int DURATION_STEP = 50;

  public void start() {
    channelManager.init();
    advService.init();
    syncService.init();
    peerStatusCheck.init();
    transactionsMsgHandler.init();
    fetchBlockService.init();
    logger.info("TronNetService start successfully");
  }

  public void stop() {
    logger.info("TronNetService closed start");
    channelManager.close();
    advService.close();
    syncService.close();
    peerStatusCheck.close();
    transactionsMsgHandler.close();
    fetchBlockService.close();
    logger.info("TronNetService closed successfully");
  }

  public int fastBroadcastTransaction(TransactionMessage msg) {
    return advService.fastBroadcastTransaction(msg);
  }

  public void broadcast(Message msg) {
    advService.broadcast(msg);
  }

  protected void onMessage(PeerConnection peer, TronMessage msg) {
    long startTime = System.currentTimeMillis();
    try {
      switch (msg.getType()) {
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
          throw new P2pException(TypeEnum.NO_SUCH_MESSAGE, msg.getType().toString());
      }
    } catch (Exception e) {
      processException(peer, msg, e);
    } finally {
      long costs = System.currentTimeMillis() - startTime;
      if (costs > DURATION_STEP) {
        logger.info("Message processing costs {} ms, peer: {}, type: {}, time tag: {}",
            costs, peer.getInetAddress(), msg.getType(), getTimeTag(costs));
        Metrics.histogramObserve(MetricKeys.Histogram.MESSAGE_PROCESS_LATENCY,
            costs / Metrics.MILLISECONDS_PER_SECOND, msg.getType().name());
      }
    }
  }

  private void processException(PeerConnection peer, TronMessage msg, Exception ex) {
    ReasonCode code;

    if (ex instanceof P2pException) {
      TypeEnum type = ((P2pException) ex).getType();
      switch (type) {
        case BAD_TRX:
          code = ReasonCode.BAD_TX;
          break;
        case BAD_BLOCK:
          code = ReasonCode.BAD_BLOCK;
          break;
        case NO_SUCH_MESSAGE:
        case MESSAGE_WITH_WRONG_LENGTH:
        case BAD_MESSAGE:
          code = ReasonCode.BAD_PROTOCOL;
          break;
        case SYNC_FAILED:
          code = ReasonCode.SYNC_FAIL;
          break;
        case UNLINK_BLOCK:
          code = ReasonCode.UNLINKABLE;
          break;
        case DB_ITEM_NOT_FOUND:
          code = ReasonCode.FETCH_FAIL;
          break;
        default:
          code = ReasonCode.UNKNOWN;
          break;
      }
      logger.warn("Message from {} process failed, {} \n type: {}, detail: {}",
          peer.getInetAddress(), msg, type, ex.getMessage());
    } else {
      code = ReasonCode.UNKNOWN;
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
}
