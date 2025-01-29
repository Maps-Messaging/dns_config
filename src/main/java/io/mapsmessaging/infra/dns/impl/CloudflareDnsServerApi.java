package io.mapsmessaging.infra.dns.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.mapsmessaging.infra.dns.DnsRecord;
import io.mapsmessaging.infra.dns.DnsServerApi;
import com.google.gson.*;
import io.mapsmessaging.infra.dns.logging.DnsInfraLogging;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class CloudflareDnsServerApi implements DnsServerApi {
  private static final String CLOUDFLARE_API_TOKEN = getEnv("CLOUDFLARE_API_TOKEN");
  private static final String ZONE_ID = getEnv("CLOUDFLARE_ZONE_ID");
  private static final String API_BASE = "https://api.cloudflare.com/client/v4/zones/" + ZONE_ID + "/dns_records";
  private static final Gson GSON = new Gson();

  private final Logger logger = LoggerFactory.getLogger(getClass());

  public Map<String, DnsRecord> fetchExistingRecords() throws Exception {
    HttpURLConnection connection = (HttpURLConnection) new URL(API_BASE).openConnection();
    connection.setRequestMethod("GET");
    connection.setRequestProperty("Authorization", "Bearer " + CLOUDFLARE_API_TOKEN);
    connection.setRequestProperty("Content-Type", "application/json");

    int responseCode = connection.getResponseCode();
    if (responseCode != 200) {
      logger.log(DnsInfraLogging.DNS_REQUEST_FAILED, "Fetch", responseCode, "");
      throw new RuntimeException("Failed to fetch existing records: HTTP " + responseCode);
    }

    String response = new String(connection.getInputStream().readAllBytes());
    JsonObject jsonResponse = GSON.fromJson(response, JsonObject.class);
    JsonArray results = jsonResponse.getAsJsonArray("result");

    Map<String, DnsRecord> records = new HashMap<>();
    for (JsonElement element : results) {
      DnsRecord recordData = new DnsRecord(element.getAsJsonObject());
      records.put(recordData.generateKey(), recordData);
    }
    return records;
  }

  public void createRecord(DnsRecord recordData) throws Exception {
    HttpURLConnection connection = (HttpURLConnection) new URL(API_BASE).openConnection();
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Authorization", "Bearer " + CLOUDFLARE_API_TOKEN);
    connection.setRequestProperty("Content-Type", "application/json");
    connection.setDoOutput(true);

    String jsonString = GSON.toJson(recordData.toJson());
    try (OutputStream os = connection.getOutputStream()) {
      os.write(jsonString.getBytes());
    }

    int responseCode = connection.getResponseCode();
    if (responseCode != 200) {
      logger.log(DnsInfraLogging.DNS_REQUEST_FAILED, "Create", responseCode, recordData.toString() );
      throw new RuntimeException("Failed to create record: HTTP " + responseCode);
    }
    logger.log(DnsInfraLogging.DNS_RECORD_CREATED, recordData);
  }

  public void updateRecord(String recordId,  DnsRecord existing, DnsRecord recordData) throws Exception {
    HttpURLConnection connection = (HttpURLConnection) new URL(API_BASE + "/" + recordId).openConnection();
    connection.setRequestMethod("PUT");
    connection.setRequestProperty("Authorization", "Bearer " + CLOUDFLARE_API_TOKEN);
    connection.setRequestProperty("Content-Type", "application/json");
    connection.setDoOutput(true);

    try (OutputStream os = connection.getOutputStream()) {
      os.write(GSON.toJson(recordData.toJson()).getBytes());
    }

    int responseCode = connection.getResponseCode();
    if (responseCode != 200) {
      logger.log(DnsInfraLogging.DNS_REQUEST_FAILED, "Update", responseCode, recordData.toString() );
      throw new RuntimeException("Failed to update record: HTTP " + responseCode);
    }
    logger.log(DnsInfraLogging.DNS_UPDATE_STARTED, existing, recordData);
  }

  public void deleteRecord(String recordId) throws Exception {
    HttpURLConnection connection = (HttpURLConnection) new URL(API_BASE + "/" + recordId).openConnection();
    connection.setRequestMethod("DELETE");
    connection.setRequestProperty("Authorization", "Bearer " + CLOUDFLARE_API_TOKEN);

    int responseCode = connection.getResponseCode();
    if (responseCode != 200) {
      logger.log(DnsInfraLogging.DNS_REQUEST_FAILED, "Delete", responseCode, recordId );
      throw new RuntimeException("Failed to delete record: HTTP " + responseCode);
    }
    logger.log(DnsInfraLogging.DNS_RECORD_DELETED, recordId);
  }

  private static String getEnv(String key){
    String val = System.getenv(key);
    if(val == null){
      val = System.getProperty(key);
    }
    return val;
  }

}
