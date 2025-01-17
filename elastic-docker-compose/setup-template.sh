#!/bin/bash

# Wait until Elasticsearch is up
until curl -s -XGET "http://localhost:9200/_cluster/health?wait_for_status=yellow&timeout=60s" > /dev/null; do
  echo "Waiting for Elasticsearch to be available..."
  sleep 5
done

# Set the index template where all string fields will be mapped as keywords
curl -X PUT "http://localhost:9200/_template/custom_template" -H 'Content-Type: application/json' -d '{
    "index_patterns": ["*"],
    "mappings": {
      "dynamic_templates": [
        {
          "strings_as_keyword": {
            "match_mapping_type": "string",
            "mapping": {
              "type": "keyword"
            }
          }
        }
      ]
    }
}'