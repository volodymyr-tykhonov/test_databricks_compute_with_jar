/*
This project is a simple example of how to use the Databricks Connect Scala client to run on
serverless or on a Databricks cluster.
 */
package com.examples

//import com.databricks.connect.DatabricksSession
import org.apache.spark.sql.{SparkSession, functions => F}
import org.apache.spark.sql.functions.udf

object Main {
  def main(args: Array[String]): Unit = {
    println("Starting the testing")

    val spark = getSession()


    // test writing to a table in the current catalog and schema, which should work both on serverless and cluster
    val tableName = getFromArgs(args, "table-name").getOrElse("tmp_table")    
    println(s"Using table name: $tableName")
    setCatalogAndSchema(spark, args)
    println("Showing range ...")
    spark.range(10).write.mode("overwrite").saveAsTable(tableName)

    // test reading from a table in the current catalog and schema and persisting to a volume, which should work both on serverless and cluster
    val optVolume = for {
      catalog <- getFromArgs(args, "catalog")
      schema <- getFromArgs(args, "schema")
      volume <- getFromArgs(args, "volume")
    } yield {
      val folderName = getFromArgs(args, "folder-name").getOrElse("tmp_folder")
      val path = s"/Volumes/$catalog/$schema/$volume/$folderName"
      println(s"Using folder: $path")
      path
    }
    optVolume match {
      case Some(volumePath) =>
        println(s"Using volume path: $volumePath")
        val df = spark.read.table(tableName).withColumn("volume", F.lit(volumePath))
        df.show()
        df.write.format("delta").mode("overwrite").save(volumePath)
      case None =>
        println("Volume not provided, skipping volume access test.")
    }

    // test RDD operations, which should work on job cluster    
    if (getFromArgs(args, "test-rdd").map(_.toLowerCase).contains("true")) {
      println("Running RDD tests ...")
      val sc = spark.sparkContext

      val nums = sc.parallelize(1 to 10)
      println(s"parallelize(1 to 10).collect() = ${nums.collect().mkString(", ")}")

      val doubled = nums.map(_ * 2)
      println(s"map(_ * 2).collect() = ${doubled.collect().mkString(", ")}")

      val evens = nums.filter(_ % 2 == 0)
      println(s"filter(_ % 2 == 0).collect() = ${evens.collect().mkString(", ")}")

      val sum = nums.reduce(_ + _)
      println(s"reduce(_ + _) = $sum")

      val wordRdd = sc.parallelize(Seq("hello world", "hello spark", "rdd test"))
      val wordCount = wordRdd
        .flatMap(_.split(" "))
        .map(w => (w, 1))
        .reduceByKey(_ + _)
        .collect()
      println(s"word count = ${wordCount.mkString(", ")}")

      println("RDD tests done.")
    }

  }

  private def setCatalogAndSchema(spark: SparkSession, args: Array[String]): Unit = {
    getFromArgs(args, "catalog").foreach { catalog =>
      spark.sql(s"USE CATALOG $catalog")
      println(s"Using catalog: $catalog")
    }

    getFromArgs(args, "schema").foreach { schema =>
      spark.sql(s"USE SCHEMA $schema")
      println(s"Using schema: $schema")
    }
  }

  private def getFromArgs(args: Array[String], key: String): Option[String] = {
    args.sliding(2, 2).collectFirst {
      case Array(k, v) if k == s"--$key" => v
    }
  }

  def getSession(): SparkSession = {
    SparkSession.builder().getOrCreate()
  }
}
