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

import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._
import uk.gov.hmrc.incorporatedentityidentificationfrontend.assets.TestConstants._
import uk.gov.hmrc.incorporatedentityidentificationfrontend.models.CompanyProfile
import uk.gov.hmrc.incorporatedentityidentificationfrontend.stubs.{AuthStub, IncorporatedEntityIdentificationStub}
import uk.gov.hmrc.incorporatedentityidentificationfrontend.utils.ComponentSpecHelper
import uk.gov.hmrc.incorporatedentityidentificationfrontend.views.ConfirmBusinessNameViewTests

import scala.concurrent.ExecutionContext.Implicits.global


class ConfirmBusinessNameControllerISpec extends ComponentSpecHelper
  with ConfirmBusinessNameViewTests
  with IncorporatedEntityIdentificationStub
  with AuthStub {

  override def afterEach(): Unit = {
    super.afterEach()
    journeyConfigRepository.drop
  }

  "GET /confirm-business-name" when {
    "the company exists in Companies House" should {
      "return ok" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        await(insertJourneyConfig(
          journeyId = testJourneyId,
          continueUrl = testContinueUrl,
          optServiceName = None,
          deskProServiceId = testDeskProServiceId,
          signOutUrl = testSignOutUrl
        ))

        val jsonBody = Json.toJsObject(CompanyProfile(testCompanyName, testCompanyNumber, testDateOfIncorporation))
        stubRetrieveCompanyProfileFromBE(testJourneyId)(status = OK, body = jsonBody)

        lazy val result: WSResponse = get(s"$baseUrl/$testJourneyId/confirm-business-name")

        result.status mustBe OK
      }

      "redirect to sign in page" when {
        "the user is UNAUTHORISED" in {
          stubAuthFailure()
          val jsonBody = Json.toJsObject(CompanyProfile(testCompanyName, testCompanyNumber, testDateOfIncorporation))
          stubRetrieveCompanyProfileFromBE(testJourneyId)(status = OK, body = jsonBody)

          lazy val result: WSResponse = get(s"$baseUrl/$testJourneyId/confirm-business-name")

          result.status mustBe SEE_OTHER
        }
      }

      "return a view" when {
        "there is no serviceName passed in the journeyConfig" should {
          lazy val insertConfig = insertJourneyConfig(
            journeyId = testJourneyId,
            continueUrl = testContinueUrl,
            optServiceName = None,
            deskProServiceId = testDeskProServiceId,
            signOutUrl = testSignOutUrl
          )
          lazy val authStub = stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
          lazy val stub = stubRetrieveCompanyProfileFromBE(testJourneyId)(
            status = OK,
            body = Json.toJsObject(CompanyProfile(testCompanyName, testCompanyNumber, testDateOfIncorporation))
          )
          lazy val result = get(s"$baseUrl/$testJourneyId/confirm-business-name")

          testConfirmBusinessNameView(result, stub, authStub, insertConfig, testCompanyName)
          testServiceName(testDefaultServiceName, result, authStub, insertConfig)
        }

        "there is a serviceName passed in the journeyConfig" should {
          lazy val insertConfig = insertJourneyConfig(
            journeyId = testJourneyId,
            continueUrl = testContinueUrl,
            optServiceName = Some(testCallingServiceName),
            deskProServiceId = testDeskProServiceId,
            signOutUrl = testSignOutUrl
          )
          lazy val authStub = stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
          lazy val stub = stubRetrieveCompanyProfileFromBE(testJourneyId)(
            status = OK,
            body = Json.toJsObject(CompanyProfile(testCompanyName, testCompanyNumber, testDateOfIncorporation))
          )
          lazy val result = get(s"$baseUrl/$testJourneyId/confirm-business-name")

          testConfirmBusinessNameView(result, stub, authStub, insertConfig, testCompanyName)
          testServiceName(testCallingServiceName, result, authStub, insertConfig)
        }
      }
    }

    "the company doesn't exist in the backend database" should {
      "show technical difficulties page" in {
        stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
        await(insertJourneyConfig(
          journeyId = testJourneyId,
          continueUrl = testContinueUrl,
          optServiceName = None,
          deskProServiceId = testDeskProServiceId,
          signOutUrl = testSignOutUrl
        ))
        stubRetrieveCompanyProfileFromBE(testJourneyId)(status = NOT_FOUND)

        lazy val result: WSResponse = get(s"$baseUrl/$testJourneyId/confirm-business-name")

        result.status mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "POST /confirm-business-name" should {
    "redirect to Capture CTUTR Page" in {
      stubAuth(OK, successfulAuthResponse(Some(testInternalId)))
      lazy val result = post(s"$baseUrl/$testJourneyId/confirm-business-name")()

      result must have(
        httpStatus(SEE_OTHER),
        redirectUri(routes.CaptureCtutrController.show(testJourneyId).url)
      )
    }
  }

}

