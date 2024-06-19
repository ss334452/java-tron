package org.tron.core.net.peer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.common.client.WalletGrpcClient;
import org.tron.common.es.ExecutorServiceManager;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.JsonUtil;
import org.tron.core.Wallet;
import org.tron.core.config.Parameter.NetConstants;
import org.tron.core.config.args.Args;
import org.tron.core.net.TronNetDelegate;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.ReasonCode;

@Slf4j(topic = "net")
@Component
public class PeerStatusCheck {

  @Autowired
  private TronNetDelegate tronNetDelegate;

  private final String name = "peer-status-check";

  private ScheduledExecutorService peerStatusCheckExecutor =  ExecutorServiceManager
      .newSingleThreadScheduledExecutor(name);

  private int blockUpdateTimeout = 30_000;

  public void init() {
    peerStatusCheckExecutor.scheduleWithFixedDelay(() -> {
      try {
        statusCheck();
      } catch (Exception e) {
        logger.error("Check peers status processing failed", e);
      }
    }, 5, 2, TimeUnit.SECONDS);
  }

  public void close() {
    ExecutorServiceManager.shutdownAndAwaitTermination(peerStatusCheckExecutor, name);
  }

  public void statusCheck() {

    long now = System.currentTimeMillis();

    tronNetDelegate.getActivePeer().forEach(peer -> {

      boolean isDisconnected = false;

      if (peer.isNeedSyncFromPeer()
          && peer.getBlockBothHaveUpdateTime() < now - blockUpdateTimeout) {
        logger.warn("Peer {} not sync for a long time", peer.getInetAddress());
        isDisconnected = true;
      }

      if (!isDisconnected) {
        isDisconnected = peer.getAdvInvRequest().values().stream()
            .anyMatch(time -> time < now - NetConstants.ADV_TIME_OUT);
        if (isDisconnected) {
          logger.warn("Peer {} get avd message timeout", peer.getInetAddress());
        }
      }

      if (!isDisconnected) {
        isDisconnected = peer.getSyncBlockRequested().values().stream()
            .anyMatch(time -> time < now - NetConstants.SYNC_TIME_OUT);
        if (isDisconnected) {
          logger.warn("Peer {} get sync message timeout", peer.getInetAddress());
        }
      }

      if (isDisconnected) {
        peer.disconnect(ReasonCode.TIME_OUT);
      }
    });
  }


  public static class  Parameter {
    @Getter
    @Setter
    String key;
    @Getter
    @Setter
    Long value;
  }

  public static class Parameters {
    @Getter
    @Setter
    List<Parameter> chainParameter;
  }

  public static class  Proposal {
    @Getter
    @Setter
    Long proposal_id;
    @Getter
    @Setter
    String state;
    @Getter
    @Setter
    Long expiration_time;
    @Getter
    @Setter
    Long create_time;
    @Getter
    @Setter
    Object proposer_address;
    @Getter
    @Setter
    Object parameters;
    @Getter
    @Setter
    Object approvals;
    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Proposal)) {
        return false;
      }
      return proposal_id.equals(((Proposal) o).getProposal_id())
        && state.equals(((Proposal) o).getState())
        && expiration_time.equals(((Proposal) o).getExpiration_time())
        && create_time.equals(((Proposal) o).getCreate_time());
    }
    @Override
    public String toString() {
      return proposal_id + "," + state + "," + create_time + "," + expiration_time;
    }
  }

  public static class  Proposals {
    @Getter
    @Setter
    List<Proposal> proposals;
  }

  public static String get(String url) throws Exception {
    URLConnection urlConnection = new URL(url).openConnection();
    BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
    return in.readLine();
  }

  public static void parametersCp(String ipPort1, String ipPort2) throws Exception {
    String url1 = "http://" + ipPort1 + "/wallet/getchainparameters";;
    String url2 = "http://" + ipPort2 + "/wallet/getchainparameters";
    String s1 = get(url1);
    String s2 = get(url2);
    Parameters ps1 = JsonUtil.json2Obj(s1, Parameters.class);
    Parameters ps2 = JsonUtil.json2Obj(s2, Parameters.class);
    System.out.println("parameters:" + ipPort1 + ", " + ipPort2
      + ", size: " + ps1.getChainParameter().size() + ", " + ps2.getChainParameter().size());
    for (Parameter p1 : ps1.getChainParameter()) {
      for (Parameter p2 : ps2.getChainParameter()) {
        if (p1.getKey().equals(p2.getKey())) {
          if ((p1.getValue() == null && p2.getValue() != null) ||
            (p1.getValue() != null && !p1.getValue().equals(p2.getValue()))) {
            System.out.println("### key: " + p1.getKey()
              + ", value: " + p1.getValue() + ", " + p2.getValue());
          }
        }
      }
    }
  }
  public static void proposalsCp(String ipPort1, String ipPort2) throws Exception {
    String url1 = "http://" + ipPort1 + "/wallet/listproposals";;
    String url2 = "http://" + ipPort2 + "/wallet/listproposals";
    String s1 = get(url1);
    String s2 = get(url2);
    Proposals ps1 = JsonUtil.json2Obj(s1, Proposals.class);
    Proposals ps2 = JsonUtil.json2Obj(s2, Proposals.class);
    System.out.println("proposals: " + ipPort1 + ", " + ipPort2
      + ", size: " + ps1.getProposals().size() + ", " + ps2.getProposals().size());
    for (Proposal p1 : ps1.getProposals()) {
      for (Proposal p2 : ps2.getProposals()) {
        if (p1.getProposal_id().equals(p2.getProposal_id())) {
          if (!p1.equals(p2)) {
            System.out.println("###: " + p1);
            System.out.println("###: " + p2);
          }
        }
      }
    }
  }

//  public static String ipPorts = "18.140.46.255:50090, 54.179.25.90:50090";
  public static String ipPorts = "18.196.99.16:50051, 52.53.189.99:50051";

//  public static void main(String[] args)  throws Exception {
//    String[] sz = ipPorts.trim().split(",");
//    accountCp(sz[0].trim(), sz[1].trim(), "4171B0AF54E0A1182A5E0947D6A64F3B22740EF318");
//  }

  public static Protocol.Account getAccount(String host, String address) {
    ManagedChannel channelFull = ManagedChannelBuilder.forTarget(host)
      .usePlaintext().build();

    WalletGrpc.WalletBlockingStub blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    ByteString bytes = ByteString.copyFrom(ByteArray.fromHexString(address));

    return blockingStubFull.getAccount(Protocol.Account.newBuilder().setAddress(bytes).build());
  }

  public static void accountCp(String host1, String host2, String address) throws Exception {

    Protocol.Account a1 = getAccount(host1, address);
    Protocol.Account other = getAccount(host2, address);

    if (a1.equals(other)) {
      System.out.println("accounts is same.");
      return;
    }

    if (!a1.getAccountName()
      .equals(other.getAccountName()))
      System.out.println("getAccountName: " + a1.getAccountName() + ", " + other.getAccountName());
    if (!a1.getAddress()
      .equals(other.getAddress()))
      System.out.println("getAddress: " + a1.getAddress() + ", " + other.getAddress());
    if (a1.getBalance()
      != other.getBalance())
      System.out.println("getBalance: " + a1.getBalance() + ", " + other.getBalance());
    if (!a1.getVotesList()
      .equals(other.getVotesList()))
      System.out.println("getVotesList: " + a1.getVotesList() + ", " + other.getVotesList());
    if (!a1.getFrozenList()
      .equals(other.getFrozenList()))
      System.out.println("getFrozenList: " + a1.getFrozenList() + ", " + other.getFrozenList());
    if (a1.getNetUsage()
      != other.getNetUsage())
      System.out.println("getNetUsage: " + a1.getNetUsage() + ", " + other.getNetUsage());
    if (a1.getAcquiredDelegatedFrozenBalanceForBandwidth()
      != other.getAcquiredDelegatedFrozenBalanceForBandwidth())
      System.out.println("getAcquiredDelegatedFrozenBalanceForBandwidth: "
        + a1.getAcquiredDelegatedFrozenBalanceForBandwidth() + ", "
        + other.getAcquiredDelegatedFrozenBalanceForBandwidth());
    if (a1.getDelegatedFrozenBalanceForBandwidth()
      != other.getDelegatedFrozenBalanceForBandwidth())
      System.out.println("getDelegatedFrozenBalanceForBandwidth: "
        + a1.getDelegatedFrozenBalanceForBandwidth() + ", "
        + other.getDelegatedFrozenBalanceForBandwidth());
    if (a1.getOldTronPower()
      != other.getOldTronPower())
      System.out.println("getOldTronPower: " + a1.getOldTronPower() + ", " + other.getOldTronPower());
    if (a1.hasTronPower() != other.hasTronPower())
      System.out.println("hasTronPower: " + a1.hasTronPower() + ", " + other.hasTronPower());
    if (a1.hasTronPower()) {
      if (!a1.getTronPower()
        .equals(other.getTronPower()))
        System.out.println("getTronPower: " + a1.getTronPower() + ", " + other.getTronPower());
    }
    if (a1.getAssetOptimized()
      != other.getAssetOptimized())
      System.out.println("getAssetOptimized: " + a1.getAssetOptimized() + ", " + other.getAssetOptimized());
    if (a1.getCreateTime()
      != other.getCreateTime())
      System.out.println("getCreateTime: " + a1.getCreateTime() + ", " + other.getCreateTime());
    if (a1.getLatestOprationTime()
      != other.getLatestOprationTime())
      System.out.println("getLatestOprationTime: " + a1.getLatestOprationTime() + ", " + other.getLatestOprationTime());
    if (a1.getAllowance()
      != other.getAllowance())
      System.out.println("getAllowance: " + a1.getAllowance() + ", " + other.getAllowance());
    if (a1.getLatestWithdrawTime()
      != other.getLatestWithdrawTime())
      System.out.println("getLatestWithdrawTime: " + a1.getLatestWithdrawTime() + ", " + other.getLatestWithdrawTime());
    if (!a1.getCode()
      .equals(other.getCode()))
      System.out.println("getCode: " + a1.getCode() + ", " + other.getCode());
    if (a1.getIsWitness()
      != other.getIsWitness())
      System.out.println("getIsWitness: " + a1.getIsWitness() + ", " + other.getIsWitness());
    if (a1.getIsCommittee()
      != other.getIsCommittee())
      System.out.println("getIsCommittee: " + a1.getIsCommittee() + ", " + other.getIsCommittee());
    if (!a1.getFrozenSupplyList()
      .equals(other.getFrozenSupplyList()))
      System.out.println("getFrozenSupplyList: " + a1.getFrozenSupplyList() + ", " + other.getFrozenSupplyList());
    if (!a1.getAssetIssuedName()
      .equals(other.getAssetIssuedName()))
      System.out.println("getAssetIssuedName: " + a1.getAssetIssuedName() + ", " + other.getAssetIssuedName());
    if (!a1.getAssetIssuedID()
      .equals(other.getAssetIssuedID()))
      System.out.println("getAssetIssuedID: " + a1.getAssetIssuedID() + ", " + other.getAssetIssuedID());
    if (a1.getFreeNetUsage()
      != other.getFreeNetUsage())
      System.out.println("getFreeNetUsage: " + a1.getFreeNetUsage() + ", " + other.getFreeNetUsage());
    if (a1.getLatestConsumeTime()
      != other.getLatestConsumeTime())
      System.out.println("getLatestConsumeTime: " + a1.getLatestConsumeTime() + ", " + other.getLatestConsumeTime());
    if (a1.getLatestConsumeFreeTime()
      != other.getLatestConsumeFreeTime())
      System.out.println("getLatestConsumeFreeTime: " + a1.getLatestConsumeFreeTime() + ", " + other.getLatestConsumeFreeTime());
    if (!a1.getAccountId()
      .equals(other.getAccountId()))
      System.out.println("getAccountId: " + a1.getAccountId() + ", " + other.getAccountId());
    if (a1.getNetWindowSize()
      != other.getNetWindowSize())
      System.out.println("getNetWindowSize: " + a1.getNetWindowSize() + ", " + other.getNetWindowSize());
    if (a1.getNetWindowOptimized()
      != other.getNetWindowOptimized())
      System.out.println("getNetWindowOptimized: " + a1.getNetWindowOptimized() + ", " + other.getNetWindowOptimized());
    if (a1.hasAccountResource() != other.hasAccountResource())
      System.out.println("hasAccountResource: " + a1.hasAccountResource() + ", " + other.hasAccountResource());
    if (a1.hasAccountResource()) {
      if (!a1.getAccountResource()
        .equals(other.getAccountResource()))
        System.out.println("getAccountResource: " + a1.getAccountResource() + ", " + other.getAccountResource());
    }
    if (!a1.getCodeHash()
      .equals(other.getCodeHash()))
      System.out.println("getCodeHash: " + a1.getCodeHash() + ", " + other.getCodeHash());
    if (a1.hasOwnerPermission() != other.hasOwnerPermission())
      System.out.println("hasOwnerPermission: " + a1.hasOwnerPermission() + ", " + other.hasOwnerPermission());
    if (a1.hasOwnerPermission()) {
      if (!a1.getOwnerPermission()
        .equals(other.getOwnerPermission()))
        System.out.println("getOwnerPermission: " + a1.getOwnerPermission() + ", " + other.getOwnerPermission());
    }
    if (a1.hasWitnessPermission() != other.hasWitnessPermission())
      System.out.println("hasWitnessPermission: " + a1.hasWitnessPermission() + ", " + other.hasWitnessPermission());
    if (a1.hasWitnessPermission()) {
      if (!a1.getWitnessPermission()
        .equals(other.getWitnessPermission()))
        System.out.println("getWitnessPermission: " + a1.getWitnessPermission() + ", " + other.getWitnessPermission());
    }
    if (!a1.getActivePermissionList()
      .equals(other.getActivePermissionList()))
      System.out.println("getActivePermissionList: " + a1.getActivePermissionList() + ", " + other.getActivePermissionList());
    if (!a1.getFrozenV2List()
      .equals(other.getFrozenV2List()))
      System.out.println("getFrozenV2List: " + a1.getFrozenV2List() + ", " + other.getFrozenV2List());
    if (!a1.getUnfrozenV2List()
      .equals(other.getUnfrozenV2List()))
      System.out.println("getUnfrozenV2List: " + a1.getUnfrozenV2List() + ", " + other.getUnfrozenV2List());
    if (a1.getDelegatedFrozenV2BalanceForBandwidth()
      != other.getDelegatedFrozenV2BalanceForBandwidth())
      System.out.println("getDelegatedFrozenV2BalanceForBandwidth: "
        + a1.getDelegatedFrozenV2BalanceForBandwidth() + ", "
        + other.getDelegatedFrozenV2BalanceForBandwidth());
    if (a1.getAcquiredDelegatedFrozenV2BalanceForBandwidth()
      != other.getAcquiredDelegatedFrozenV2BalanceForBandwidth())
      System.out.println("getAcquiredDelegatedFrozenV2BalanceForBandwidth: "
        + a1.getAcquiredDelegatedFrozenV2BalanceForBandwidth() + ", "
        + other.getAcquiredDelegatedFrozenV2BalanceForBandwidth());
    if (!a1.getUnknownFields().equals(other.getUnknownFields()))
      System.out.println("getUnknownFields: " + a1.getUnknownFields() + ", " + other.getUnknownFields());
  }

  public static void main(String[] args) throws  Exception {
//    ManagedChannel channelFull = ManagedChannelBuilder.forTarget("18.163.230.203:50051")
//      .usePlaintext().build();
    ManagedChannel channelFull = ManagedChannelBuilder.forTarget("127.0.0.1:50060")
      .usePlaintext().build();

    WalletGrpc.WalletBlockingStub blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    long start = 62646961;

    long gap = 28800;

    long tmp = start;

    int txCnt = 0;

    int txTimeoutCnt = 0;

    int txTimeout3Cnt = 0;

    int txTimeout6Cnt = 0;

    int day = 0;

    while (tmp-- >= start - 90 * gap - 1) {
      GrpcAPI.NumberMessage message = GrpcAPI.NumberMessage.newBuilder().setNum(start).build();
      Protocol.Block block = blockingStubFull.getBlockByNum(message);
      long time = block.getBlockHeader().getRawData().getTimestamp();
      for (Protocol.Transaction t : block.getTransactionsList()) {
        if (t.getRawData().getExpiration() < time) {
          txTimeoutCnt++;
          logger.info("###time: " + t.getRawData().getExpiration() + ","
            + time + ", " + (t.getRawData().getExpiration() < time));
        }
        if (t.getRawData().getExpiration() < time + 3) {
          logger.info("###time3: " + t.getRawData().getExpiration() + ","
            + time + ", " + (t.getRawData().getExpiration() < time));
          txTimeout3Cnt++;
        }

        if (t.getRawData().getExpiration() < time + 6) {
          txTimeout6Cnt++;
        }

        txCnt++;
      }

      logger.info("### num: " +  (start - tmp)  + "," + txCnt + ","  + txTimeout6Cnt
        + ","  + txTimeout3Cnt
        + ","  + txTimeoutCnt);

      if ((start - tmp) % 28800 == 0) {
        logger.info("### day: " +  ++day  + "," + txCnt + ","  + txTimeout6Cnt
          + ","  + txTimeout3Cnt
          + ","  + txTimeoutCnt);
      }
    }

  }
}
