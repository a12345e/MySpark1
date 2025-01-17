# Start with the official Elasticsearch image
FROM elasticsearch:7.13.1

# Create a non-root user and group

# Set ownership of the Elasticsearch directory
RUN chown -R elasticsearch:elasticsearch /usr/share/elasticsearch

# Switch to the non-root user

# Set environment variables
ENV discovery.type=single-node
ENV ES_JAVA_OPTS="-Xms512m -Xmx512m"

# Copy custom Elasticsearch configuration
COPY elasticsearch.yml /usr/share/elasticsearch/config/elasticsearch.yml

# Copy a custom initialization script
COPY setup-template.sh /usr/share/elasticsearch/config/setup-template.sh

# Ensure the script is executable
USER elasticsearch

# Execute the initialization script on startup
CMD ["/bin/bash", "-c", "bin/elasticsearch && /usr/share/elasticsearch/config/setup-template.sh"]

# Expose ports
EXPOSE 9200 9300