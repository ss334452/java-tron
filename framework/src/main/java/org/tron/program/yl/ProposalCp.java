package org.tron.program.yl;

import lombok.Getter;
import lombok.Setter;
import org.tron.common.utils.JsonUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

public class ProposalCp {

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



  public static void work(String ipPorts) {
    String[] sz = ipPorts.split(",");
    for (int i = 0; i < sz.length - 1; i++) {
      try {
        parametersCp(sz[i].trim(), sz[i + 1].trim());
        proposalsCp(sz[i].trim(), sz[i + 1].trim());
      } catch (Exception e) {
        System.out.println(e + ", " + sz[i + 1]);
      }
    }
  }

}
