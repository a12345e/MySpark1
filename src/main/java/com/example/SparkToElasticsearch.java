package com.example;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

public class SparkToElasticsearch {
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
                .option("es.resource", "employee")  // Define the index and type
                .option("es.write.null", "true")
                .option("spark.es.nodes", "localhost")
                .option("es.nodes.wan.only", "true")
                .option("spark.es.port", "9200")// Exclude null values
                .mode("overwrite")  // Define the write mode (overwrite, append, etc.)
                .save();

        // Stop SparkSession
        spark.stop();
    }
}