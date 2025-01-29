package io.mapsmessaging.infra.dns.logging;

import io.mapsmessaging.logging.Category;
import io.mapsmessaging.logging.LEVEL;
import io.mapsmessaging.logging.LogMessage;
import lombok.Getter;

@Getter
public enum DnsInfraLogging  implements LogMessage {
  DNS_UPDATE_STARTED(LEVEL.WARN, INFRA_CATEGORY.DNS , "Starting DNS update using {} as source. DryRun = {}"),
  DNS_UPDATE_COMPLETED(LEVEL.WARN, INFRA_CATEGORY.DNS , "Finished DNS update"),
  DNS_CSV_PARSED(LEVEL.WARN, INFRA_CATEGORY.DNS , "Parsed {} records from the source file"),

  DNS_RECORD_CREATED(LEVEL.WARN, INFRA_CATEGORY.DNS , "Creating new DNS record {}"),
  DNS_RECORD_UPDATED(LEVEL.WARN, INFRA_CATEGORY.DNS , "Updated DNS record {} to {}"),
  DNS_RECORD_DELETED(LEVEL.WARN, INFRA_CATEGORY.DNS , "Deleted DNS record {}"),
  DNS_REQUEST_FAILED(LEVEL.WARN, INFRA_CATEGORY.DNS , "DNS request {} failed with code {}, Record: {}");

  private final String message;
  private final LEVEL level;
  private final Category category;
  private final int parameterCount;

  DnsInfraLogging(LEVEL level, INFRA_CATEGORY category, String message) {
    this.message = message;
    this.level = level;
    this.category = category;
    int location = message.indexOf("{}");
    int count = 0;
    while (location != -1) {
      count++;
      location = message.indexOf("{}", location + 2);
    }
    this.parameterCount = count;
  }

  public enum INFRA_CATEGORY implements Category {
    DNS("Dns");

    @Getter
    private final String description;

    public String getDivision(){
      return "Infra";
    }

    INFRA_CATEGORY(String description) {
      this.description = description;
    }
  }
}
