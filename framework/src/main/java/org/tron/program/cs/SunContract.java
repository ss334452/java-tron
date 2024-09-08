package org.tron.program.cs;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.protos.Protocol;
import org.tron.protos.contract.SmartContractOuterClass;

import java.util.HashMap;
import java.util.Map;

@Slf4j(topic = "app")
public class SunContract {


  public static Map<String, Integer> successMap = new HashMap<>();
  public static Map<String, Integer> failMap = new HashMap<>();

  public static void work() throws Exception {
//    ManagedChannel channelFull = ManagedChannelBuilder.forTarget("52.53.189.99:50051")
//      .usePlaintext().build();

    ManagedChannel channelFull = ManagedChannelBuilder.forTarget("127.0.0.1:50051")
      .usePlaintext().build();

    WalletGrpc.WalletBlockingStub blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    long start = 65043357;

    long gap = 28800;

    long tmp = start;

    int txCnt = 0;

    int day = 0;

    int sunCnt = 0;

    int addressCnt = 0;

    int addressSuccessCnt = 0;

    int attackSuccess = 0;

    int attackFail = 0;

    long tmpNumber = 0;

    String tmpWit = "";

    while (tmp-- >= start - 30 * gap - 1) {
      GrpcAPI.NumberMessage message = GrpcAPI.NumberMessage.newBuilder().setNum(tmp).build();
      Protocol.Block block = blockingStubFull.getBlockByNum(message);
      if (block == null) {
        logger.warn("block is null, end");
        continue;
      }

      long blockNum = block.getBlockHeader().getRawData().getNumber();
      int cnt = 0;

      for (Protocol.Transaction t : block.getTransactionsList()) {
        txCnt++;

        int type = t.getRawData().getContract(0).getType().getNumber();
        if (type != Protocol.Transaction.Contract.ContractType.TriggerSmartContract_VALUE) {
          continue;
        }

        SmartContractOuterClass.TriggerSmartContract contract = ContractCapsule.getTriggerContractFromTransaction(t);
        if (contract == null) {
          continue;
        }

        byte[] contractAddress = contract.getContractAddress().toByteArray();
        String key = Hex.encodeHexString(contractAddress);
        if (!"41FF7155B5DF8008FBF3834922B2D52430B27874F5".equals(key.toUpperCase())) {
          continue;
        }

        sunCnt++;


        byte[] bytes = TransactionCapsule.getOwner(t.getRawData().getContract(0));
        String address = Hex.encodeHexString(bytes);
        if ("41987C0191A1A098FFC9ADDC9C65D2C3D028B10CA3".equals(address.toUpperCase())) {
          addressCnt++;
          if (t.getRet(0).getContractRet() == Protocol.Transaction.Result.contractResult.SUCCESS) {
            addressSuccessCnt++;
            cnt++;
          }
        }
      }

      if (cnt > 0) {
        logger.info("block number: {}, sunCnt: {}, cnt: {}", blockNum, sunCnt, cnt);
        String wit = Hex.encodeHexString(block.getBlockHeader().getRawData().getWitnessAddress().toByteArray());
        if (cnt % 2 == 0) {
          attackSuccess++;
          success(wit);
        } else {
          if (tmpNumber == 0) {
            tmpNumber = blockNum;
            tmpWit = wit;
          } else if (tmpNumber + 1  ==  blockNum) {
            success(wit);
            tmpNumber = 0;
          } else {
            failMap.put(tmpWit, 1);
            attackFail++;
            tmpNumber = blockNum;
            tmpWit = wit;
          }
        }
      }

      if ((start - tmp) % 28800 == 0) {
        logger.info("### day: " +  ++day
          + ", txCnt: " + txCnt
          + ", sunCnt:"  + sunCnt
          + ", addressCnt: "  + addressCnt
          + ", addressSuccessCnt: "  + addressSuccessCnt
          + ", attackSuccess: "  + attackSuccess
          + ", attackFail: "  + attackFail);
      }
    }

    successMap.forEach((k, v) -> logger.info("successMap: {}, {}", k, v));
    failMap.forEach((k, v) -> logger.info("failMap: {}, {}", k, v));
  }

  public static void success(String wit) {
    Integer value = successMap.get(wit);
    if (value == null) {
      successMap.put(wit, 1);
    }else {
      successMap.put(wit, value + 1);
    }
  }

  public static void fail(String wit) {
    Integer value = failMap.get(wit);
    if (value == null) {
      failMap.put(wit, 1);
    }else {
      failMap.put(wit, value + 1);
    }
  }

}
