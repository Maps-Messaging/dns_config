package io.mapsmessaging.infra.dns;

import com.google.gson.JsonObject;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class DnsRecord {

  private String id;
  private String domain;
  private String type;
  private int ttl;
  private String content;
  private int priority;
  private boolean proxy;

  public DnsRecord(JsonObject json) {
    String zone="mapsmessaging.io";
    if(json.has("zone_name")) {
      zone = json.get("zone_name").getAsString();
    }
    this.id = json.get("id").getAsString();
    this.domain = json.get("name").getAsString();
    if(domain.length() > zone.length() && domain.endsWith(zone)){
      domain = domain.substring(0, domain.lastIndexOf(zone));
    }
    if(domain.endsWith(".")){
      domain = domain.substring(0, domain.length() - 1);
    }
    this.ttl = json.get("ttl").getAsInt();
    this.content = json.get("content").getAsString();
    this.type = json.get("type").getAsString();
    if(json.has("priority")) {
      this.priority = json.get("priority").getAsInt();
    }
    else{
      priority = -1;
    }
    proxy = json.get("proxied").getAsBoolean();
    clean();
  }

  public DnsRecord(String line) {
    String[] parts = line.split(",");
    if (parts.length < 4) return;
    domain = parts[0].trim();
    ttl = parseTtl(parts[1].trim());
    type = parts[2].trim().toUpperCase();

    if (type.equalsIgnoreCase("MX")) {
      priority = Integer.parseInt(parts[3].trim());
      content =  parts[4].trim();
    } else {
      priority = -1;
      content = parts[3].trim();
    }
    if(type.equalsIgnoreCase("TXT")) {
      if(!content.startsWith("\"")){
        content = "\"" + content;
      }
      if(!content.endsWith("\"")){
        content = content + "\"";
      }
    }
    proxy = false;
    if(parts.length == 5 && !type.equalsIgnoreCase("MX")) {
      String[] params = parts[4].trim().split(":");
      if(params.length == 2 && params[0].equalsIgnoreCase("proxy")) {
        proxy = params[1].equalsIgnoreCase("true");
      }
    }
    clean();
  }

  private void clean(){
    if(content.endsWith(".")){
      content = content.substring(0, content.length() - 1);
    }
  }

  public String generateKey() {
    StringBuilder key = new StringBuilder();
    key.append(type);
    key.append('|');
    key.append(domain);
    key.append('|');
    key.append(content);
    if(priority != -1) {
      key.append('|');
      key.append(priority);
    }
    return key.toString().toLowerCase();
  }

  public JsonObject toJson() {
    JsonObject recordData = new JsonObject();
    recordData.addProperty("name", domain);
    recordData.addProperty("type", type.toUpperCase());
    recordData.addProperty("ttl", ttl);
    if(proxy){
      recordData.addProperty("proxied", proxy);
    }
    if (type.equalsIgnoreCase("MX")) {
      recordData.addProperty("priority", priority);
    }
    recordData.addProperty("content", content);
    return recordData;
  }

  public boolean isEqual(DnsRecord record) {
    boolean val = (this.ttl == record.ttl || ttl == 1 || record.ttl == 1);
    val = val && this.content.equalsIgnoreCase(record.content);
    if(type.equalsIgnoreCase("MX")) {
      val = val && this.priority == record.priority;
    }
    return val;
  }


  private int parseTtl(String ttlStr) {
    String[] parts = ttlStr.split(" ");
    if (parts.length == 2) {
      int value = Integer.parseInt(parts[0]);
      String unit = parts[1].toLowerCase();
      if (unit.contains("hour")) return value * 3600;
      if (unit.contains("day")) return value * 86400;
    }
    return 300; // default TTL
  }

  public String diff(DnsRecord record) {
    StringBuilder diff = new StringBuilder();
    if(!domain.equals(record.domain)){
      diff.append("domain ").append(domain).append(" does not match ").append(record.domain).append("\n");
    }
    if(!type.equals(record.type)){
      diff.append("type ").append(type).append(" does not match ").append(record.type).append("\n");
    }
    if(!content.equals(record.content)){
      diff.append("content ").append(content).append(" does not match ").append(record.content).append("\n");
    }
    if(priority != record.priority){
      diff.append("priority ").append(priority).append(" does not match ").append(record.priority).append("\n");
    }
    if(ttl != record.ttl){
      diff.append("ttl ").append(ttl).append(" does not match ").append(record.ttl).append("\n");
    }
    return diff.toString();
  }
}
