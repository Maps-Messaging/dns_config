package io.mapsmessaging.infra.dns.impl;

import io.mapsmessaging.infra.dns.DnsRecord;

public class CloudflareNoOpDnsManager extends CloudflareDnsServerApi {
  @Override
  public void createRecord(DnsRecord recordData) throws Exception {
    System.err.println("createRecord "+recordData);
  }

  @Override
  public void updateRecord(String recordId, DnsRecord existing, DnsRecord recordData) throws Exception {
    System.err.println("updateRecord "+existing+" with "+recordData+"\n"+recordData.diff(existing));
  }

  @Override
  public void deleteRecord(String recordId) throws Exception {
    System.err.println("deleteRecord "+recordId);
  }
}
