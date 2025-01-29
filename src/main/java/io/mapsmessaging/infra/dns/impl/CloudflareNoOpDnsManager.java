package io.mapsmessaging.infra.dns.impl;

import io.mapsmessaging.infra.dns.DnsRecord;
import io.mapsmessaging.infra.dns.logging.DnsInfraLogging;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;

public class CloudflareNoOpDnsManager extends CloudflareDnsServerApi {
  private final Logger logger = LoggerFactory.getLogger(getClass());


  @Override
  public void createRecord(DnsRecord recordData) throws Exception {
    logger.log(DnsInfraLogging.DNS_RECORD_CREATED, recordData);
  }

  @Override
  public void updateRecord(String recordId, DnsRecord existing, DnsRecord recordData) throws Exception {
    logger.log(DnsInfraLogging.DNS_UPDATE_STARTED, existing, recordData);
  }

  @Override
  public void deleteRecord(String recordId) throws Exception {
    logger.log(DnsInfraLogging.DNS_RECORD_DELETED, recordId);
  }
}
