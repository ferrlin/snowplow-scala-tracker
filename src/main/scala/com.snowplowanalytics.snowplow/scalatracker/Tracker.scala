/*
 * Copyright (c) 2015 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics.snowplow.scalatracker

import java.util.UUID

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

import emitters.TEmitter

/**
 * Tracker class
 *
 * @param emitters Sequence of emitters to which events are passed
 * @param namespace Tracker namespace
 * @param appId ID of the application
 * @param encodeBase64 Whether to encode JSONs
 */
class Tracker(emitters: Seq[TEmitter], namespace: String, appId: String, encodeBase64: Boolean = true) {

  private val Version = s"scala-${generated.ProjectSettings.version}"

  private var subject: Subject = new Subject()

  private def getTimestamp(timestamp: Option[Long]): Long = timestamp match {
    case None => System.currentTimeMillis()
    case Some(t) => t * 1000
  }

  /**
   * Pass the assembled payload to every emitter
   *
   * @param payload
   */
  private def track(payload: Payload) {
    val event = payload.get
    emitters foreach {
      e => e.input(event)
    }
  }

  /**
   * Add contexts and timestamp to the payload
   *
   * @param payload
   * @param contexts
   * @param timestamp
   * @return payload with additional data
   */
  private def completePayload(
    payload: Payload,
    contexts: Seq[SelfDescribingJson],
    timestamp: Option[Long]): Payload = {

    payload.add("eid", UUID.randomUUID().toString)

    if (! contexts.isEmpty) {

      val contextsEnvelope = SelfDescribingJson(
        "iglu:com.snowplowanalytics.snowplow/contexts/jsonschema/1-0-1",
        contexts
        )

      payload.addJson(contextsEnvelope.toJObject, encodeBase64, "cx", "co")
    }

    if (!payload.nvPairs.contains("dtm")) {
      payload.add("dtm", getTimestamp(timestamp).toString)
    }

    payload.add("tv", Version)
    payload.add("tna", namespace)
    payload.add("aid", appId)

    payload.addDict(subject.getSubjectInformation())

    payload
  }

  /**
   * Track a Snowplow unstructured event
   *
   * @param unstructEvent self-describing JSON for the event
   * @param contexts
   * @param timestamp
   * @return The tracker instance
   */
  def trackUnstructEvent(
    unstructEvent: SelfDescribingJson,
    contexts: Seq[SelfDescribingJson] = Nil,
    timestamp: Option[Long] = None): Tracker = {

    val payload = new Payload()

    payload.add("e", "ue")

    val envelope = SelfDescribingJson(
      "iglu:com.snowplowanalytics.snowplow/unstruct_event/jsonschema/1-0-0",
      unstructEvent)

    payload.addJson(envelope.toJObject, encodeBase64, "ue_px", "ue_pr")

    track(completePayload(payload, contexts, timestamp))

    this
  }

  /**
   * Set the Subject for the tracker
   * The subject's configuration will be attached to every event
   *
   * @param subject
   * @return The tracker instance
   */
  def setSubject(subject: Subject): Tracker = {
    this.subject = subject
    this
  }
}
