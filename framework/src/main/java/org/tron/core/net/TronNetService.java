package org.tron.core.net;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.message.Message;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.config.args.Args;
import org.tron.core.net.message.adv.TransactionMessage;
import org.tron.core.net.messagehandler.TransactionsMsgHandler;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerStatusCheck;
import org.tron.core.net.service.adv.AdvService;
import org.tron.core.net.service.fetchblock.FetchBlockService;
import org.tron.core.net.service.keepalive.KeepAliveService;
import org.tron.core.net.service.nodepersist.NodePersistService;
import org.tron.core.net.service.sync.SyncService;
import org.tron.p2p.P2pConfig;
import org.tron.p2p.P2pService;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

@Slf4j(topic = "net")
@Component
public class TronNetService {

  @Getter
  private static P2pConfig p2pConfig;

  @Getter
  private static P2pService p2pService;

  @Autowired
  private AdvService advService;

  @Autowired
  private SyncService syncService;

  @Autowired
  private PeerStatusCheck peerStatusCheck;

  @Autowired
  private TransactionsMsgHandler transactionsMsgHandler;

  @Autowired
  private FetchBlockService fetchBlockService;

  @Autowired
  private KeepAliveService keepAliveService;

  private CommonParameter parameter = Args.getInstance();

  @Autowired
  private P2pEventHandlerImpl p2pEventHandler;

  @Autowired
  private NodePersistService nodePersistService;

  public void start() {
    try {
      p2pConfig = getConfig();
      p2pService = new P2pService();
      p2pService.start(p2pConfig);
      p2pService.register(p2pEventHandler);
      advService.init();
      syncService.init();
      peerStatusCheck.init();
      transactionsMsgHandler.init();
      fetchBlockService.init();
      keepAliveService.init();
      nodePersistService.init();
      logger.info("Net service start successfully");
    } catch (Exception e) {
      logger.error("Net service start failed", e);
    }
  }

  public void close(){
    advService.close();
    syncService.close();
    peerStatusCheck.close();
    transactionsMsgHandler.close();
    fetchBlockService.close();
    keepAliveService.close();
    p2pService.close();
    nodePersistService.close();
    logger.info("Net service closed successfully");
  }

  public static List<PeerConnection> getPeers() {
    return P2pEventHandlerImpl.getPeers();
  }

  public void broadcast(Message msg) {
    advService.broadcast(msg);
  }

  public int fastBroadcastTransaction(TransactionMessage msg) {
    return advService.fastBroadcastTransaction(msg);
  }

  private List<InetSocketAddress> getInetSocketAddresses(List<String> list) {
    List<InetSocketAddress> addresses = new ArrayList<>();
    for (String s: list) {
      String sz[] = s.split(":");
      addresses.add(new InetSocketAddress(sz[0], Integer.getInteger(sz[1])));
    }
    return addresses;
  }

  private P2pConfig getConfig() {
    List<InetSocketAddress> seeds = new ArrayList<>();
    seeds.addAll(nodePersistService.dbRead());
    for (String s: parameter.getSeedNode().getIpList()) {
      String sz[] = s.split(":");
      seeds.add(new InetSocketAddress(sz[0], Integer.parseInt(sz[1])));
    }

    P2pConfig config = new P2pConfig();
    config.setSeedNodes(seeds);
    config.setActiveNodes(parameter.getActiveNodes());
    config.setTrustNodes(parameter.getPassiveNodes());
    config.setMaxConnections(parameter.getMaxConnections());
    config.setMinConnections(parameter.getMinConnections());
    config.setMaxConnectionsWithSameIp(parameter.getMaxConnectionsWithSameIp());
    config.setPort(parameter.getNodeListenPort());
    config.setVersion(parameter.getNodeP2pVersion());
    config.setDisconnectionPolicyEnable(false);
    config.setDiscoverEnable(true);
    return config;
  }
}