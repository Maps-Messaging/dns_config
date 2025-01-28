package io.mapsmessaging.infra.dns;

import io.mapsmessaging.infra.dns.impl.CloudflareDnsServerApi;
import io.mapsmessaging.infra.dns.impl.CloudflareNoOpDnsManager;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class DnsInfraUpdater {

  private static final String CSV_PATH = "dns.csv";

  private final DnsServerApi dnsServerApi;
  private final Map<String, DnsRecord> existingRecords;
  private final Map<String, DnsRecord> desiredRecords;

  public DnsInfraUpdater(boolean dryRun) throws Exception {
    dnsServerApi = !dryRun? new CloudflareDnsServerApi() : new CloudflareNoOpDnsManager();
    existingRecords = dnsServerApi.fetchExistingRecords();
    desiredRecords = parseCsv();
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
  }

  private Map<String, DnsRecord> parseCsv() throws IOException {
    Map<String, DnsRecord> desiredRecords = new HashMap<>();
    List<String> lines = Files.readAllLines(Paths.get(CSV_PATH));
    for (String line : lines) {
      DnsRecord dnsRecord = new DnsRecord(line);
      desiredRecords.put(dnsRecord.generateKey(), dnsRecord);
    }
    return desiredRecords;
  }

  public static void main(String[] args) throws Exception {
    DnsInfraUpdater manager = new DnsInfraUpdater(false);
    manager.processRecords();
  }
}
