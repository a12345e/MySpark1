package com.example;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientOptions;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.RestHighLevelClientBuilder;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.HashMap;
import java.util.Map;

public class WriteIntoElasticSearch8WithSSLAndCert {

    private SSLContext sslContextFromCa(String caPath) throws Exception {
        // Load PEM (X.509)
        // Strip OpenSSL "Bag Attributes" headers before parsing
        String pemContent = new String(Files.readAllBytes(Paths.get(caPath)), StandardCharsets.UTF_8);
        int beginIdx = pemContent.indexOf("-----BEGIN CERTIFICATE-----");
        if (beginIdx == -1) throw new IllegalArgumentException("No PEM certificate block found in: " + caPath);
        byte[] pemBytes = pemContent.substring(beginIdx).getBytes(StandardCharsets.UTF_8);

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate ca;
        try (ByteArrayInputStream is = new ByteArrayInputStream(pemBytes)) {
            ca = cf.generateCertificate(is);
        }

        // Create in-memory truststore and add CA
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("ca", ca);

        // Build SSL context from that truststore
        return SSLContexts.custom()
                .loadTrustMaterial(trustStore, null)
                .build();
    }
    private  RestClient createRestClient() throws Exception {

        // Load CA certificate (PKCS12 from certutil)
        SSLContext sslContext = sslContextFromCa("c:\\create-certs\\certs\\clean-ca.crt");

        BasicCredentialsProvider creds = new BasicCredentialsProvider();
        creds.setCredentials(
                new AuthScope("localhost", 9200),
                new UsernamePasswordCredentials("elastic", "changeme")
        );

        return RestClient.builder(
                        new HttpHost("localhost", 9200, "https")
                )
                .setHttpClientConfigCallback((HttpAsyncClientBuilder httpClientBuilder) ->
                        httpClientBuilder
                                .setSSLContext(sslContext)
                                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                                .setDefaultCredentialsProvider(creds)
                )
                .build();
    }

    private ElasticsearchTransport createElasticsearchTransport() throws Exception {
        return  new RestClientTransport(createRestClient(), new JacksonJsonpMapper(), new RestClientOptions(RequestOptions.DEFAULT));
    }

    private ElasticsearchClient createElasticsearchClient(ElasticsearchTransport transport) throws Exception {
        return new ElasticsearchClient(transport);
    }
    private RestHighLevelClient createRestHighLevelClient() throws Exception {
        return new RestHighLevelClientBuilder(createRestClient())
                .setApiCompatibilityMode(true)
                .build();
    }

    private void writeWithNativeElasticApi() throws Exception {
        ElasticsearchTransport transport = createElasticsearchTransport();
        ElasticsearchClient client = createElasticsearchClient(transport);

        Product product = new Product("iphone", 999);

        IndexResponse response = client.index(
                i -> i.index("new_api_index").id("1").document(product));

        System.out.println("Indexed into : new_api_index" + response.version());
    }
    private void writeWithNativeElasticLegacyApi() throws Exception {
        RestHighLevelClient legacyClient = createRestHighLevelClient();
        // 4. Use it like normal (Legacy 7.x style)
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("user", "kimchy");
        jsonMap.put("message", "trying out the legacy client on v8");

        IndexRequest request = new IndexRequest("legacy_api_index").source(jsonMap);
        System.out.println("Indexed into : old_api " +legacyClient.index(request, RequestOptions.DEFAULT).getVersion());
        legacyClient.close();
    }
    private void writeWithSpark(){
        //System.setProperty("javax.net.debug", "ssl,handshake");
        // curl -u elastic:changeme -k https://localhost:9200
        //openssl x509 -in ca.crt -out clean-ca.crt
        //keytool -import -file clean-ca.crt -alias elasticsearch -keystore elastic-truststore.jks -storepass mypassword
        System.setProperty("javax.net.debug", "ssl,handshake");
        System.setProperty("javax.net.ssl.trustStore", "C:/create-certs/certs/elastic-truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "mypassword");
//        // Initialize SparkSession
        SparkSession spark = SparkSession.builder()
                .appName("Spark to Elasticsearch")
                .master("local[*]")
                .config("spark.es.nodes", "localhost") // Set your Elasticsearch node URL
                .config("spark.es.port", "9200") // Set your Elasticsearch port
                .config("spark.es.index.auto.create", "true")
                .getOrCreate();

        // Example Dataset (replace with your own)
        Dataset<Row> df = spark.createDataFrame(
                java.util.Arrays.asList(
                        RowFactory.create("Alice", 25, "Engineer"),
                        RowFactory.create("Bob", null, "Data Scientist"),
                        RowFactory.create("Charlie", 30, null),
                        RowFactory.create(null, 22, "Designer")
                ),
                new StructType(new StructField[]{
                        new StructField("name", DataTypes.StringType, true, Metadata.empty()),
                        new StructField("age", DataTypes.IntegerType, true, Metadata.empty()),
                        new StructField("occupation", DataTypes.StringType, true, Metadata.empty())
                })
        );

        // Step 1: Write to Elasticsearch excluding null values
        df.write()
                .format("es")
                .option("es.nodes", "localhost") // Just the host
                .option("es.port", "9200")
                .option("es.net.ssl", "true")
                //.option("es.net.ssl.cert.allow.self.signed", "true")

                // Use the full Windows path with double backslashes
                // This tells Spark to stop trying to be smart and just talk to the local node
                .option("es.nodes.wan.only", "true")

                // Auth
                .option("es.net.http.auth.user", "elastic")
                .option("es.net.http.auth.pass", "changeme")
                .mode("overwrite")
                .save("spark_index");
        System.out.println("Indexed into : spark_index ");
        // Stop SparkSession
        spark.stop();

    }
    public class Product {
        public String name;
        public int price;

        public Product(String name, int price) {
            this.name = name;
            this.price = price;
        }
    }

    private void run() throws Exception {
        writeWithSpark();
        writeWithNativeElasticApi();
        writeWithNativeElasticLegacyApi();
    }
    public static void main(String[] args) throws Exception {
        new WriteIntoElasticSearch8WithSSLAndCert().run();
    }
}
