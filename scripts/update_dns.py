#!/usr/bin/env python3
import csv
import os
import requests

CLOUDFLARE_API_TOKEN = os.getenv("CLOUDFLARE_API_TOKEN", "YOUR_API_TOKEN")
ZONE_ID = "YOUR_ZONE_ID"
CSV_PATH = "dns.csv"

API_BASE = f"https://api.cloudflare.com/client/v4/zones/{ZONE_ID}/dns_records"
HEADERS = {
    "Authorization": f"Bearer {CLOUDFLARE_API_TOKEN}",
    "Content-Type": "application/json"
}

def parse_ttl(ttl_str):
    # Simple examples: "6 Hours", "1 Day", "1 Hour"
    parts = ttl_str.split()
    if len(parts) == 2:
        value, unit = parts
        value = int(value)
        if "Hour" in unit:
            return value * 3600
        elif "Day" in unit:
            return value * 86400
    return 300  # default fallback

def fetch_existing_records():
    resp = requests.get(API_BASE, headers=HEADERS)
    resp.raise_for_status()
    records = resp.json().get("result", [])
    # Create a dictionary keyed by (name, type, content) or (name, type, priority)
    existing = {}
    for r in records:
        key = (r["name"], r["type"], r.get("content"), str(r.get("priority", "")))
        existing[key] = r
    return existing

def ensure_record(domain, record_type, ttl_str, *rest):
    # For non-MX records: [domain, ttl, type, content]
    # For MX records:    [domain, ttl, type, priority, content]
    ttl = parse_ttl(ttl_str)
    content = None
    priority = None

    if record_type.upper() == "MX":
        priority, content = rest
    else:
        content = rest[0]

    name = domain  # If you need subdomains, combine here, e.g., f"{domain}.mapsmessaging.io"
    key = (name, record_type.upper(), content.strip(), str(priority or ""))

    return {
        "name": name,
        "type": record_type.upper(),
        "content": content.strip(),
        "ttl": ttl,
        "priority": int(priority) if priority else None
    }, key

def create_record(record_data):
    resp = requests.post(API_BASE, headers=HEADERS, json=record_data)
    resp.raise_for_status()

def update_record(record_id, record_data):
    update_url = f"{API_BASE}/{record_id}"
    resp = requests.put(update_url, headers=HEADERS, json=record_data)
    resp.raise_for_status()

def delete_record(record_id):
    delete_url = f"{API_BASE}/{record_id}"
    resp = requests.delete(delete_url, headers=HEADERS)
    resp.raise_for_status()

def main():
    existing = fetch_existing_records()
    desired = {}

    with open(CSV_PATH, newline='', encoding='utf-8') as csvfile:
        reader = csv.reader(csvfile)
        for row in reader:
            row = [col.strip() for col in row if col.strip()]  # remove empty columns
            if not row:
                continue

            domain, ttl_str, rtype = row[0], row[1], row[2]
            # Check if MX has 5 columns total, else 4
            if rtype.upper() == "MX":
                # domain, ttl, type, priority, content
                record_data, key = ensure_record(domain, rtype, ttl_str, row[3], row[4])
            else:
                # domain, ttl, type, content
                record_data, key = ensure_record(domain, rtype, ttl_str, row[3])
            desired[key] = record_data

    # Create or update existing
    for key, data in desired.items():
        if key not in existing:
            create_record(data)
        else:
            # Compare TTL, content, priority, etc.
            existing_rec = existing[key]
            needs_update = (
                    existing_rec["ttl"] != data["ttl"]
                    or existing_rec.get("content") != data["content"]
                    or str(existing_rec.get("priority", "")) != str(data.get("priority", ""))
            )
            if needs_update:
                update_record(existing_rec["id"], data)
        # Mark as handled
        if key in existing:
            del existing[key]

    # Anything left in existing is not in CSV => delete
    for leftover in existing.values():
        delete_record(leftover["id"])

if __name__ == "__main__":
    main()
