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

import org.apache.commons.codec.binary.Base64

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

import scala.collection.mutable.{Map => MMap}

/**
 * Contains the map of key-value pairs making up an event
 */
class Payload {

  val Encoding = "UTF-8"

  val nvPairs = MMap[String, String]()

  /**
   * Add a key-value pair
   *
   * @param name
   * @param value
   */
  def add(name: String, value: String) {
    if (!name.isEmpty && name != null && !value.isEmpty && value != null) {
      nvPairs += (name -> value)
    }
  }

  /**
   * Add a map of key-value pairs one by one
   *
   * @param dict
   */
  def addDict(dict: Map[String, String]) {
    dict foreach {
      case (k, v) => add(k, v)
    }
  }

  /**
   * Stringify a JSON and add it
   *
   * @param json
   * @param encodeBase64 Whether to base 64 encode the JSON
   * @param typeWhenEncoded Key to use if encodeBase64 is true
   * @param typeWhenNotEncoded Key to use if encodeBase64 is false
   */
  def addJson(
    json: JObject,
    encodeBase64: Boolean,
    typeWhenEncoded: String,
    typeWhenNotEncoded: String) {

    val jsonString = compact(render(json))

    if (encodeBase64) {
      add(typeWhenEncoded, new String(Base64.encodeBase64(jsonString.getBytes(Encoding)), Encoding))
    } else {
      add(typeWhenNotEncoded, jsonString)
    }
  }

  /**
   * Return the key-value pairs making up the event as an immutable map
   *
   * @return Event map
   */
  def get(): Map[String, String] = Map(nvPairs.toList: _*)

}
