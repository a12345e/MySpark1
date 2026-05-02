package com.example;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.io.Serializable;

public class SparkToElasticsearchElasticSSLandCERT implements Serializable {
    public static void main(String[] args) {
        //
        //System.setProperty("javax.net.debug", "ssl,handshake");
        // curl -u elastic:changeme -k https://localhost:9200
        //openssl x509 -in ca.crt -out clean-ca.crt
        //keytool -import -file clean-ca.crt -alias elasticsearch -keystore elastic-truststore.jks -storepass mypassword
        System.setProperty("javax.net.ssl.trustStore", "C:/users/a1234/projects/MySpark1/certs/elastic-truststore.jks");
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
                .save("my_index1");

        // Stop SparkSession
        spark.stop();
    }
}