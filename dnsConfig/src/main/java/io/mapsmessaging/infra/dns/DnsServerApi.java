package io.mapsmessaging.infra.dns;

import java.util.Map;

public interface DnsServerApi {
  Map<String, DnsRecord> fetchExistingRecords() throws Exception;
  void createRecord(DnsRecord recordData) throws Exception;
  void updateRecord(String recordId, DnsRecord existing,  DnsRecord recordData) throws Exception;
  void deleteRecord(String recordId) throws Exception;
}
