package com.example;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.io.Serializable;

public class SparkToElasticsearchElastic8NoSSL implements Serializable {
    public static void main(String[] args) {
        // Initialize SparkSession
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
                .format("org.elasticsearch.spark.sql")
                .option("es.nodes", "localhost")
                .option("es.port", "9200")
                .option("es.nodes.wan.only", "true")
                // --- SSL CONFIGURATION ---
                .option("es.net.ssl", "false")
//                .option("es.net.ssl.cert.allow.self.signed", "true") // Trust local dev certs
                // --- AUTHENTICATION ---
                .option("es.net.http.auth.user", "elastic")
                .option("es.net.http.auth.pass", "strongpassword123") // Use your generated password
                // --- ADDITIONAL SETTINGS ---
                .option("es.nodes.wan.only", "true") // Essential for local testing
                .option("es.resource", "employee")  // Define the index and type
                .mode("overwrite")
                .save();

        // Stop SparkSession
        spark.stop();
    }
}