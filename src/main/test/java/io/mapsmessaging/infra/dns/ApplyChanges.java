package io.mapsmessaging.infra.dns;
import org.junit.jupiter.api.Test;

public class ApplyChanges {

  @Test
  void runUpdates() throws Exception {
    DnsInfraUpdater update = new DnsInfraUpdater(false);
    update.processRecords();
  }
}
