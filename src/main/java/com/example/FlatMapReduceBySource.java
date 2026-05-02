package com.example;
import static org.apache.spark.sql.functions.col;
import org.apache.spark.HashPartitioner;
import org.apache.spark.api.java.*;
import org.apache.spark.api.java.function.*;
import org.apache.spark.sql.*;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import scala.Tuple2;

import java.util.*;


public class FlatMapReduceBySource {
    public static void main(String[] args)
    {


        SparkSession spark = SparkSession.builder()
                .appName("Generic Reduce By Key Example")
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



        Dataset<Row> df = spark.createDataFrame(data, schema);
        List<String> sources = df.select("source")
                .distinct()
                .javaRDD()
                .map(row -> row.getString(0))
                .collect();

        for( String source: sources){
            Dataset<Row> current_df = df.where(col("source").equalTo(source));
            JavaPairRDD<String, Map<String, Object>> pairRDD = current_df.javaRDD().flatMapToPair(row -> {
                List<Tuple2<String, Map<String, Object>>> pairs = new ArrayList<>();

                // Create a map of all values in the row
                Map<String, Object> rowMap = new HashMap<>();
                String[] columns = row.schema().fieldNames();
                for (String col : columns) {
                    rowMap.put(col, row.getAs(col));
                }

                // Emit multiple pairs from a single row
                // Key = column name, Value = entire row as a map
                pairs.add(new Tuple2<String, Map<String, Object>>("name", rowMap));
                pairs.add(new Tuple2<String, Map<String, Object>>("phone", rowMap));

                return pairs.iterator();
            });
            JavaPairRDD<String, Map<String, Object>> reduced = pairRDD.reduceByKey((map1,map2) -> {
                return new HashMap<>(map1);
            });

            reduced.foreach(pair -> System.out.println(pair._1 + " => " + pair._2));
        }



        // Print result



            spark.stop();

    }
}