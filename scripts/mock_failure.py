import requests
import json
import time
import random
from datetime import datetime, timezone
from concurrent.futures import ThreadPoolExecutor

# Configuration
API_URL = "http://localhost:8080/api/v1/signals/ingest"
NUM_SIGNALS = 1000
CONCURRENCY = 50

COMPONENTS = [
    {"id": "RDBMS_PRIMARY_01", "type": "RDBMS"},
    {"id": "CACHE_CLUSTER_01", "type": "DISTRIBUTED_CACHE"},
    {"id": "MCP_HOST_99", "type": "MCP"}
]

ERRORS = ["CONNECTION_TIMEOUT", "OUT_OF_MEMORY", "DEADLOCK_DETECTED", "DISK_FULL"]
SEVERITIES = ["P0", "P1", "P2"]

def send_signal(signal_data):
    try:
        response = requests.post(
            API_URL, 
            json=signal_data, 
            headers={"Content-Type": "application/json"},
            timeout=5
        )
        return response.status_code
    except Exception as e:
        return str(e)

def generate_signal():
    component = random.choice(COMPONENTS)
    return {
        "componentId": component["id"],
        "componentType": component["type"],
        "errorCode": random.choice(ERRORS),
        "severity": random.choice(SEVERITIES),
        "message": "Simulated failure for load testing",
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "metadata": {
            "region": "us-east-1",
            "host": f"ip-10-0-1-{random.randint(1, 255)}"
        }
    }

def main():
    print(f" Starting load test: sending {NUM_SIGNALS} signals with concurrency {CONCURRENCY}")
    start_time = time.time()
    
    signals = [generate_signal() for _ in range(NUM_SIGNALS)]
    
    success_count = 0
    with ThreadPoolExecutor(max_workers=CONCURRENCY) as executor:
        results = executor.map(send_signal, signals)
        
        for result in results:
            if result == 202:
                success_count += 1
                
    end_time = time.time()
    duration = end_time - start_time
    
    print(f"\n Results:")
    print(f"Total time: {duration:.2f} seconds")
    print(f"Throughput: {NUM_SIGNALS / duration:.2f} requests/second")
    print(f"Successful requests: {success_count}/{NUM_SIGNALS}")

if __name__ == "__main__":
    main()
