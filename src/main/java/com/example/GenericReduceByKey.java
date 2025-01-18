package com.example;

import org.apache.spark.sql.*;
import org.apache.spark.sql.types.*;

import java.util.*;

public class GenericReduceByKey {

    public static Dataset<Row> reduceByKey(Dataset<Row> dataset,
                                           String keyField,
                                           List<String> fieldsForMaxReduction) {
        // Group dataset by the key field
        RelationalGroupedDataset groupedDataset = dataset.groupBy(keyField);

        // Dynamically build aggregation expressions based on the schema
        List<Column> aggregationExpressions = new ArrayList<>();
        StructType schema = dataset.schema();

        for (StructField field : schema.fields()) {
            String fieldName = field.name();

            if (fieldName.equals(keyField)) {
                continue; // Skip the key field
            }

            if (fieldsForMaxReduction.contains(fieldName)) {
                // Max aggregation for specified fields
                aggregationExpressions.add(functions.max(functions.col(fieldName)).alias(fieldName));
            } else {
                // First non-null value for all other fields
                aggregationExpressions.add(functions.first(functions.col(fieldName), true).alias(fieldName));
            }
        }
        // Perform the aggregation
        Column[] columnsArray = aggregationExpressions.toArray(new Column[0]);
        return groupedDataset.agg(columnsArray[0],Arrays.copyOfRange(columnsArray, 1, columnsArray.length));
    }

    public static void main(String[] args) {
        // Initialize Spark Session
        SparkSession spark = SparkSession.builder()
                .appName("Generic Reduce By Key Example")
                .master("local[*]")
                .getOrCreate();

        // Sample data
        List<Row> data = Arrays.asList(
                RowFactory.create("key1", 10.0, "value1", 100, "aaaa"),
                RowFactory.create("key1", 15.0, null, null, null),
                RowFactory.create("key2", null, "value3", 200, "bbb")
        );

        // Define the schema
        StructType schema = new StructType(new StructField[]{
                new StructField("key", DataTypes.StringType, false, Metadata.empty()),
                new StructField("score", DataTypes.DoubleType, true, Metadata.empty()),
                new StructField("description", DataTypes.StringType, true, Metadata.empty()),
                new StructField("extra", DataTypes.IntegerType, true, Metadata.empty()),
                new StructField("kaka", DataTypes.StringType, true, Metadata.empty())
        });

        // Create Dataset<Row>
        Dataset<Row> dataset = spark.createDataFrame(data, schema);

        // Define fields for max reduction
        List<String> fieldsForMaxReduction = Arrays.asList("score");

        // Perform reduce by key
        Dataset<Row> reducedDataset = reduceByKey(dataset, "key", fieldsForMaxReduction);

        // Show the results
        reducedDataset.show();

        // Stop Spark
        spark.stop();
    }
}