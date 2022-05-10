/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.incorporatedentityidentificationfrontend.views

import uk.gov.hmrc.incorporatedentityidentificationfrontend.utils.ComponentSpecHelper

import scala.io.Source

class WelshLanguageISpec extends ComponentSpecHelper {

  val messageKeysEnglish: List[String] = getMessageKeys("messages").toList
  val messageKeysWelsh: List[String] = getMessageKeys("messages.cy").toList

  "English messages must have the same keys as Welsh messages" in {
    for (key <- messageKeysEnglish) {
      messageKeysWelsh.contains(key) mustBe true
    }
  }
  "Welsh messages must have the same keys as English messages" in {
    for (key <- messageKeysWelsh) {
      messageKeysEnglish.contains(key) mustBe true
    }
  }

  private def getMessageKeys(fileName: String) = {
    Source.fromResource(fileName)
      .getLines
      .map(_.trim)
      .filter(!_.startsWith("#"))
      .filter(_.nonEmpty)
      .map(_.split(' ').head)
  }

}
