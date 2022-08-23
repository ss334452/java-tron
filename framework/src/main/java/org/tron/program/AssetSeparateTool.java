package org.tron.program;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.store.AccountStore;

@Slf4j(topic = "app")
@Component
public class AssetSeparateTool {

  @Autowired
  private AccountStore accountStore;

  public void run() throws Exception {
    long[] cnt = {0};
    long time = System.currentTimeMillis();
    accountStore.forEach(k -> {
      cnt[0]++;
      accountStore.put(k.getKey(), k.getValue());
      if (++cnt[0] % 100000 == 0) {
        logger.info("### cnt: {}, cost: {}", cnt[0],
                (System.currentTimeMillis() - time) / 60_000);
      }
    });
    logger.info("### finish cnt: {}, cost: {}", cnt[0],
            (System.currentTimeMillis() - time) / 60_000);
    Thread.sleep(10_000);
    System.exit(0);
  }
}
