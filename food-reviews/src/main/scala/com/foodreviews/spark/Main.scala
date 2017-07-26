package com.foodreviews.spark

import io.gatling.app.Gatling
import io.gatling.core.config.GatlingPropertiesBuilder
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.functions.{concat, desc, lit}
import org.apache.spark.sql.{DataFrame, SparkSession}

object Main {

  val spark =
    SparkSession
      .builder()
      .appName("Food Reviews")
      .config("spark.master", "local")
      .getOrCreate()

  import spark.implicits._

  def main(args: Array[String]): Unit = {

    // Set the log level to only print errors
    Logger.getLogger("org").setLevel(Level.ERROR)

    // Creating Spark data frame from file
    val df = spark.sqlContext.read
      .format("com.databricks.spark.csv") // Use pre-defined CSV data format
      .option("header", "true") // Use first line of all files as header
      .option("inferSchema", "true") // Automatically infer data types
      .load("../amazon-fine-foods/Reviews.csv")
    // The 'amazon-fine-foods' dir must be on the same level with 'food-reviews' dir

    args match {
      case Array("mostActiveUsers") => mostActiveUsers(df)
      case Array("mostCommentedFood") => mostCommentedFood(df)
      case Array("mostUsedWords") => mostUsedWords(df)
      case _ => mostActiveUsers(df); mostCommentedFood(df); mostUsedWords(df)
    }

    // Closing Spark session
    spark.stop()

    if (args.contains("translate=true"))
      translate
  }

  def mostActiveUsers(df: DataFrame) = {
    df.select($"ProfileName").groupBy($"ProfileName").count().orderBy(desc("count")).limit(1000).orderBy("ProfileName").show(1000)
  }
  def mostCommentedFood(df: DataFrame) =
    df.select($"ProductId").groupBy($"ProductId").count().orderBy(desc("count")).limit(1000).orderBy("ProductId").show(1000)

  def mostUsedWords(df: DataFrame) = {
    // Will be counting words for Summary and Text together, that is why use concat
    val summaryAndText = df.select(concat($"Summary", lit(" "), $"Text"))

    // Splitting text into words (by anything but words and apostrophes)
    val words = summaryAndText.flatMap(_.toString().toLowerCase().split("[^\\w']+").filter(_ != ""))

    // Grouping by words, counting instances in each group, ordering by count
    words.groupBy("value").count().orderBy(desc("count")).limit(1000).orderBy("value").show(1000)
  }

  def translate = {
    // This sets the class for the Simulation we want to run.
    val simClass = classOf[TranslateReviews].getName

    val props = new GatlingPropertiesBuilder
    props.sourcesDirectory("./src/main/scala")
    props.binariesDirectory("./target/scala-2.11/classes")
    props.simulationClass(simClass)
    Gatling.fromMap(props.build)
  }

}
