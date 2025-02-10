package io.mapsmessaging.infra.dns;

import org.junit.jupiter.api.Test;

import java.io.File;

class ApplyChangesTest {

  @Test
  void runUpdates() throws Exception {
    File file = new File(System.getProperty("user.dir") + "/zones/mapsmessaging_io/dns.csv");
    if(file.exists()) {
      System.err.println(file.getAbsolutePath());
    }
    DnsInfraUpdater update = new DnsInfraUpdater(file.getAbsolutePath(),true);
    update.processRecords();
  }
}
