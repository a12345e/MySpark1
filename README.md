

spark-submit \
--class com.example.MySparkApp \
--master local[*] \
--deploy-mode client \
--jars /path/to/elasticsearch-hadoop-7.10.0.jar \
--conf spark.executor.memory=2g \
--conf spark.driver.memory=1g \
/path/to/my-spark-app.jar \
spark-submit  --class com.example.SparkToElasticsearchElastic8NoSSL.class  /mnt/c/Users/a1234/projects/MySpark1/targetMySpark1-1.0-SNAPSHOT-jar-with-dependencies.jar

==============================
build elasticsearch no ssl by user/password
============================
docker-compose down -v
docker container prune -f
docker-compose up -d
docker exec -it elasticsearch /usr/share/elasticsearch/bin/elasticsearch-reset-password -u kibana_system -i
set password as strongpassword123
docker-compose up -d --force-recreate kibana


==============================
build elasticsearch using ssl and certs
============================
prepare the certs once:
======
_Use powershell:
mkdir elastic-docker
cd elastic-docker
mkdir certs
docker run --rm -v ${PWD}/certs:/certs docker.elastic.co/elasticsearch/elasticsearch:8.13.4 bash -c "elasticsearch-certutil cert --silent --ca /certs/elastic-stack-ca.p12 --ca-pass '' --out /certs/elastic-certificates.p12 --pass ''"_
------------
clean: docker-compose down -v
---------
start elastic/kibana: docker-compose up -d
-------------
set the kibana_system password to what kibana expects: curl.exe -k -u elastic:changeme -X POST https://localhost:9200/_security/user/kibana_system/_password -H "Content-Type: application/json" --data-binary "@password.json"
--------------
docker compose restart kibana
-----------

