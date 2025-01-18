

spark-submit \
--class com.example.MySparkApp \
--master local[*] \
--deploy-mode client \
--jars /path/to/elasticsearch-hadoop-7.10.0.jar \
--conf spark.executor.memory=2g \
--conf spark.driver.memory=1g \
/path/to/my-spark-app.jar \
spark-submit  --class com.example.SparkToElasticsearch.class  /mnt/c/Users/a1234/projects/MySpark1/targetMySpark1-1.0-SNAPSHOT-jar-with-dependencies.jar