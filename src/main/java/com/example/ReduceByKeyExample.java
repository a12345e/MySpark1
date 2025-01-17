package com.example;

import org.apache.spark.sql.*;
import org.apache.spark.sql.types.*;
import org.apache.spark.sql.Row;
import java.util.*;

public class ReduceByKeyExample {

    public static void main(String[] args) {
        // Initialize Spark Session
        SparkSession spark = SparkSession.builder()
                .appName("Reduce By Key Example")
                .master("local[*]")
                .getOrCreate();

        // Sample data
        List<Row> data = Arrays.asList(
                RowFactory.create("key1", 10.0, "value1", 100),
                RowFactory.create("key1", 15.0, null, null),
                RowFactory.create("key2", null, "value3", 200)
        );

        // Define the schema
        StructType schema = new StructType(new StructField[]{
                new StructField("key", DataTypes.StringType, false, Metadata.empty()),
                new StructField("score", DataTypes.DoubleType, true, Metadata.empty()),
                new StructField("description", DataTypes.StringType, true, Metadata.empty()),
                new StructField("extra", DataTypes.IntegerType, true, Metadata.empty())
        });

        // Create Dataset<Row>
        Dataset<Row> dataset = spark.createDataFrame(data, schema);

        // Reduce by key
        Dataset<Row> reducedDataset = dataset.groupBy("key").agg(
                functions.max("score").alias("max_score"),  // Max reduction for "score"
                functions.first("description", true).alias("first_description"),  // First non-null "description"
                functions.first("extra", true).alias("first_extra")  // First non-null "extra"
        );

        // Show results
        reducedDataset.show();

        // Stop Spark
        spark.stop();
    }
}