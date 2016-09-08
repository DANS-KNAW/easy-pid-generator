/**
 * Copyright (C) 2015-2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.pid

import java.util.UUID
import java.util.concurrent.{BlockingQueue, TimeUnit}

import com.hazelcast.core.HazelcastInstanceNotActiveException
import org.slf4j.LoggerFactory
import rx.lang.scala.{Observable, Scheduler}
import rx.lang.scala.schedulers.NewThreadScheduler
import rx.lang.scala.subscriptions.CompositeSubscription

import scala.concurrent.duration.Duration

package object microservice {

  type ResponseDatastructure = String
  type Response = (UUID, ResponseDatastructure, ResponseMessage)

  val log = LoggerFactory.getLogger(getClass)

  implicit class ObserveBlockingQueue[T](val queue: BlockingQueue[T]) extends AnyVal {
    def observe(timeout: Duration, scheduler: Scheduler = NewThreadScheduler())(running: () => Boolean): Observable[T] = {
      Observable(subscriber => {
        val worker = scheduler.createWorker
        val subscription = worker.scheduleRec {
          try {
            if (running()) {
              Option(queue.poll(timeout.toMillis, TimeUnit.MILLISECONDS)).foreach(t => {
                log.trace(s"received new item: $t")
                subscriber.onNext(t)
              })
            }
            else {
              log.trace("no longer running; completing the stream")
              subscriber.onCompleted()
            }
          }
          catch {
            case e: HazelcastInstanceNotActiveException => subscriber.onCompleted()
            case e: Throwable =>
              log.debug(s"exception caught while polling the queue: ${e.getClass.getSimpleName} - ${e.getMessage}", e)
              subscriber.onError(e)
          }
        }
        subscriber.add(CompositeSubscription(subscription, worker))
      })
    }
  }
}
