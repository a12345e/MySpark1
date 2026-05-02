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
                RowFactory.create("A","keya1", 10.0, "value1", 100),
                RowFactory.create("A","keya1", 16.0, null, null),
                RowFactory.create("A","keya2", 17.0, null, null),
                RowFactory.create("B","keyb2", null, "value210", 200),
                RowFactory.create("B","keyb2", null, "value250", 200),
                RowFactory.create("B","key3", null, "value3", 200)
        );
        // Define the schema
        StructType schema = new StructType(new StructField[]{
                new StructField("source", DataTypes.StringType, false, Metadata.empty()),
                new StructField("key", DataTypes.StringType, false, Metadata.empty()),
                new StructField("score", DataTypes.DoubleType, true, Metadata.empty()),
                new StructField("description", DataTypes.StringType, true, Metadata.empty()),
                new StructField("extra", DataTypes.IntegerType, true, Metadata.empty())
        });

        // Create Dataset<Ro
        Dataset<Row> dataset = spark.createDataFrame(data, schema);


        Dataset<Row> reducedDataset = dataset.groupBy("key").agg(
                functions.max("score").alias("max_score"),  // Max reduction for "score"
                functions.first("description", true).alias("first_description"),  // First non-null "description"
                functions.first("extra", true).alias("first_extra")  // First non-null "extra"
        );

        // Reduce by key


        // Show results
        reducedDataset.show();

        // Stop Spark
        spark.stop();
    }
}