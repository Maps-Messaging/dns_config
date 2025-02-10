package io.mapsmessaging.infra.dns;

import io.mapsmessaging.infra.dns.impl.CloudflareDnsServerApi;
import io.mapsmessaging.infra.dns.impl.CloudflareNoOpDnsManager;
import io.mapsmessaging.infra.dns.logging.DnsInfraLogging;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class DnsInfraUpdater {

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final DnsServerApi dnsServerApi;
  private final Map<String, DnsRecord> existingRecords;
  private final Map<String, DnsRecord> desiredRecords;

  public DnsInfraUpdater(String dnsRecordPath, boolean dryRun) throws Exception {
    dnsServerApi = !dryRun? new CloudflareDnsServerApi() : new CloudflareNoOpDnsManager();
    logger.log(DnsInfraLogging.DNS_UPDATE_STARTED, dnsRecordPath, dryRun);
    desiredRecords = parseCsv(dnsRecordPath);
    existingRecords = dnsServerApi.fetchExistingRecords();
  }

  public void processRecords() throws Exception {
    for (Map.Entry<String, DnsRecord> entry : desiredRecords.entrySet()) {
      String key = entry.getKey();
      DnsRecord recordData = entry.getValue();

      if (!existingRecords.containsKey(key)) {
        // Create Record
        dnsServerApi.createRecord(recordData);
      } else {
        DnsRecord existingRecord = existingRecords.get(key);
        if (!existingRecord.isEqual(recordData)) {
          // Update Record
          DnsRecord existing = existingRecords.get(key);

          dnsServerApi.updateRecord(existingRecord.getId(), existing, recordData);
        }
        existingRecords.remove(key);
      }
    }

    // Delete leftover records
    for (DnsRecord leftover : existingRecords.values()) {
      dnsServerApi.deleteRecord(leftover.getId());
    }
    logger.log(DnsInfraLogging.DNS_UPDATE_COMPLETED);
  }

  private Map<String, DnsRecord> parseCsv(String path) throws IOException {
    Map<String, DnsRecord> records = new LinkedHashMap<>();
    List<String> lines = Files.readAllLines(Paths.get(path));
    for (String line : lines) {
      line = line.trim();
      if(line.isEmpty()) continue;
      DnsRecord dnsRecord = new DnsRecord(line);
      records.put(dnsRecord.generateKey(), dnsRecord);
    }
    logger.log(DnsInfraLogging.DNS_CSV_PARSED, records.size());
    return records;
  }

}
