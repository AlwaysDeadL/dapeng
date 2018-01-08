package com.dapeng.soa

import com.github.dapeng.soa.scala.{CalculateServiceClient, PrintServiceAsyncClient, PrintServiceClient}
//需要添加这个引用
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}
/**
  * Created by lihuimin on 2018/1/3.
  */
object TestScalaClient {



  def main(args: Array[String]): Unit = {
    System.setProperty("soa.zookeeper.host", "192.168.99.100:2181")

    //测试同步
     val client = new PrintServiceClient
     val result = client.printInfo2("test")
    println(result)


    //测试异步调用
    val asyncClient = new PrintServiceAsyncClient
    val asyncResult: Future[String] = asyncClient.printInfo2("test", 20000)
    asyncResult onComplete {
      case Success(value) => println(value)
      case Failure(t) => println("An error has occured: " + t.getMessage)
    }

    val scalaClient = new CalculateServiceClient
    val calResult = scalaClient.calcualteWordCount("sd","sdd");
    println("result:"+ calResult);
  }

}
