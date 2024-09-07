package org.tron.program.yl;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.tron.api.WalletGrpc;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocol;

public class AccountCp {

  public static void cp(String ipPorts, String address)  throws Exception {
    String[] sz = ipPorts.trim().split(",");
    accountCp(sz[0].trim(), sz[1].trim(), address);
  }

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


}
