package com.example;


// Standard Java imports
import java.io.IOException;

// Apache HttpComponents (Low-level client)
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;

// Elastic Low-Level Rest Client
import org.elasticsearch.client.RestClient;

// New Elastic Java API Client imports
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;

public class ElasticWriteNosslWriter {

    // Simple POJO to represent your data
    public static class UserData {
        public String name;
        public String message;

        public UserData() {} // Required for Jackson
        public UserData(String name, String message) {
            this.name = name;
            this.message = message;
        }
    }

    public static void main(String[] args) {
        String serverUrl = "http://localhost:9200";
        String user = "elastic";
        String password = "changeme"; // The one you set in Docker

        // 1. Setup Credentials Provider
        final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(user, password));

        // 2. Create the low-level RestClient
        RestClient restClient = RestClient.builder(HttpHost.create(serverUrl))
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                        .setDefaultCredentialsProvider(credentialsProvider))
                .build();

        // 3. Create the transport with a Jackson mapper
        RestClientTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper());

        // 4. Create the high-level API client
        ElasticsearchClient client = new ElasticsearchClient(transport);

        try {
            // Data to index
            UserData data = new UserData("John Doe", "Writing to Elastic 8 without SSL!");

            // 5. Perform the Index operation
            IndexResponse response = client.index(i -> i
                    .index("my-java-index")
                    .id("1")
                    .document(data)
            );

            System.out.println("Document Indexed! ID: " + response.id());
            System.out.println("Result: " + response.result().jsonValue());

        } catch (IOException e) {
            System.err.println("Error connecting to Elasticsearch: " + e.getMessage());
        } finally {
            // Close the client
            try {
                restClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}