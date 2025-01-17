#!/bin/bash

# Add a custom index template
curl -X PUT "http://localhost:9200/_template/my_template" -H "Content-Type: application/json" -d '{
  "index_patterns": ["custom-*"],
  "settings": {
    "number_of_shards": 1
  },
  "mappings": {
    "properties": {
      "message": { "type": "text" },
      "timestamp": { "type": "date" }
    }
  }
}' || echo "Template setup failed."

# Add other custom logic if needed
echo "Initialization script completed."