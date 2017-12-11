/**
 * Copyright (C) 2015 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.pid

import java.util.concurrent.CountDownLatch

import scalaj.http.Http

// TODO make a automated test...
object concurrencytest extends App {

  def test(name: String, latch: CountDownLatch, done: CountDownLatch): Runnable = new Runnable {
    def run(): Unit = {
      println(s"test $name")
      latch.await()
      val response = Http("http://deasy.dans.knaw.nl:20140/create?type=doi").method("POST").asString
      println((name, response.code, response.body))
      done.countDown()
    }
  }

  val n = 10
  val latch = new CountDownLatch(1)
  val done = new CountDownLatch(n)

  for (i <- 1 to 10) {
    new Thread(test(s"test$i", latch, done)).start()
  }

  latch.countDown()
  done.await()
}
