/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.incorporatedentityidentificationfrontend.controllers

import play.api.test.Helpers._
import uk.gov.hmrc.incorporatedentityidentificationfrontend.assets.TestConstants._
import uk.gov.hmrc.incorporatedentityidentificationfrontend.stubs.{AuthStub, IncorporatedEntityIdentificationStub}
import uk.gov.hmrc.incorporatedentityidentificationfrontend.utils.ComponentSpecHelper
import uk.gov.hmrc.incorporatedentityidentificationfrontend.views.CaptureCtutrViewTests


class CaptureCtutrControllerISpec extends ComponentSpecHelper
  with CaptureCtutrViewTests
  with IncorporatedEntityIdentificationStub
  with AuthStub {

  "GET /ct-utr" should {
    "return OK" in {
      await(insertJourneyConfig(
        journeyId = testJourneyId,
        authInternalId = testInternalId,
        continueUrl = testContinueUrl,
        optServiceName = None,
        deskProServiceId = testDeskProServiceId,
        signOutUrl = testSignOutUrl
      ))
      stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
      lazy val result = get(s"$baseUrl/$testJourneyId/ct-utr")

      result.status mustBe OK
    }

    "return a view" when {
      "there is no serviceName passed in the journeyConfig" should {
        lazy val insertConfig = insertJourneyConfig(
          journeyId = testJourneyId,
          authInternalId = testInternalId,
          continueUrl = testContinueUrl,
          optServiceName = None,
          deskProServiceId = testDeskProServiceId,
          signOutUrl = testSignOutUrl
        )
        lazy val authStub = stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        lazy val result = get(s"$baseUrl/$testJourneyId/ct-utr")

        testCaptureCtutrView(result, authStub, insertConfig)
        testServiceName(testDefaultServiceName, result, authStub, insertConfig)
      }

      "there is a serviceName passed in the journeyConfig" should {
        lazy val insertConfig = insertJourneyConfig(
          journeyId = testJourneyId,
          authInternalId = testInternalId,
          continueUrl = testContinueUrl,
          optServiceName = Some(testCallingServiceName),
          deskProServiceId = testDeskProServiceId,
          signOutUrl = testSignOutUrl
        )
        lazy val authStub = stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        lazy val result = get(s"$baseUrl/$testJourneyId/ct-utr")

        testCaptureCtutrView(result, authStub, insertConfig)
        testServiceName(testCallingServiceName, result, authStub, insertConfig)
      }
    }

    "redirect to sign in page" when {
      "the user is not logged in" in {
        await(insertJourneyConfig(
          journeyId = testJourneyId,
          authInternalId = testInternalId,
          continueUrl = testContinueUrl,
          optServiceName = None,
          deskProServiceId = testDeskProServiceId,
          signOutUrl = testSignOutUrl
        ))
        stubAuthFailure()
        lazy val result = get(s"$baseUrl/$testJourneyId/ct-utr")

        result.status mustBe SEE_OTHER
        result.header(LOCATION) mustBe Some(s"/bas-gateway/sign-in?continue_url=%2Fidentify-your-incorporated-business%2F$testJourneyId%2Fct-utr&origin=incorporated-entity-identification-frontend")
      }
    }

    "return NOT_FOUND" when {
      "the journeyId does not match what is stored in the journey config database" in {
        await(insertJourneyConfig(
          journeyId = testJourneyId + "1",
          authInternalId = testInternalId,
          continueUrl = testContinueUrl,
          optServiceName = None,
          deskProServiceId = testDeskProServiceId,
          signOutUrl = testSignOutUrl
        ))
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))

        lazy val result = get(s"$baseUrl/$testJourneyId/ct-utr")

        result.status mustBe NOT_FOUND
      }

      "the auth internal ID does not match what is stored in the journey config database" in {
        await(insertJourneyConfig(
          journeyId = testJourneyId,
          authInternalId = testInternalId + "1",
          continueUrl = testContinueUrl,
          optServiceName = None,
          deskProServiceId = testDeskProServiceId,
          signOutUrl = testSignOutUrl
        ))
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))

        lazy val result = get(s"$baseUrl/$testJourneyId/ct-utr")

        result.status mustBe NOT_FOUND
      }

      "neither the journey ID or auth internal ID are found in the journey config database" in {
        await(insertJourneyConfig(
          journeyId = testJourneyId + "1",
          authInternalId = testInternalId + "1",
          continueUrl = testContinueUrl,
          optServiceName = None,
          deskProServiceId = testDeskProServiceId,
          signOutUrl = testSignOutUrl
        ))
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))

        lazy val result = get(s"$baseUrl/$testJourneyId/ct-utr")

        result.status mustBe NOT_FOUND
      }
    }

    "throw an Internal Server Exception" when {
      "the user does not have an internal ID" in {
        stubAuth(OK, successfulAuthResponse(None))

        lazy val result = get(s"$baseUrl/$testJourneyId/ct-utr")

        result.status mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "POST /ct-utr" when {
    "a valid ctutr is submitted" should {
      "store ctutr and redirect to Check Your Answers page" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        stubStoreCtutr(testJourneyId, testCtutr)(status = OK)

        val result = post(s"$baseUrl/$testJourneyId/ct-utr")("ctutr" -> testCtutr)

        result must have(
          httpStatus(SEE_OTHER),
          redirectUri(routes.CheckYourAnswersController.show(testJourneyId).url)
        )

      }
    }

    "no ctutr is submitted" should {
      "return a bad request" in {
        await(insertJourneyConfig(
          journeyId = testJourneyId,
          authInternalId = testInternalId,
          continueUrl = testContinueUrl,
          optServiceName = None,
          deskProServiceId = testDeskProServiceId,
          signOutUrl = testSignOutUrl
        ))
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        lazy val result = post(s"$baseUrl/$testJourneyId/ct-utr")("ctutr" -> "")

        result.status mustBe BAD_REQUEST
      }

      lazy val insertConfig = insertJourneyConfig(
        journeyId = testJourneyId,
        authInternalId = testInternalId,
        continueUrl = testContinueUrl,
        optServiceName = None,
        deskProServiceId = testDeskProServiceId,
        signOutUrl = testSignOutUrl
      )
      lazy val authStub = stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
      lazy val result = post(s"$baseUrl/$testJourneyId/ct-utr")("ctutr" -> "")

      testCaptureCtutrErrorMessagesNoCtutr(result, authStub, insertConfig)
    }

    "an invalid ctutr is submitted" should {
      "return a bad request" in {
        await(insertJourneyConfig(
          journeyId = testJourneyId,
          authInternalId = testInternalId,
          continueUrl = testContinueUrl,
          optServiceName = None,
          deskProServiceId = testDeskProServiceId,
          signOutUrl = testSignOutUrl
        ))
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        lazy val result = post(s"$baseUrl/$testJourneyId/ct-utr")("ctutr" -> "123456789")

        result.status mustBe BAD_REQUEST
      }

      lazy val insertConfig = insertJourneyConfig(
        journeyId = testJourneyId,
        authInternalId = testInternalId,
        continueUrl = testContinueUrl,
        optServiceName = None,
        deskProServiceId = testDeskProServiceId,
        signOutUrl = testSignOutUrl
      )
      lazy val authStub = stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
      lazy val result = post(s"$baseUrl/$testJourneyId/ct-utr")("ctutr" -> "123456789")

      testCaptureCtutrErrorMessagesInvalidCtutr(result, authStub, insertConfig)
    }
  }

}
