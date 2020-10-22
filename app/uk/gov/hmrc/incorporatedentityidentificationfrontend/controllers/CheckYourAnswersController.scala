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

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.incorporatedentityidentificationfrontend.config.AppConfig
import uk.gov.hmrc.incorporatedentityidentificationfrontend.controllers.errorpages.{routes => errorRoutes}
import uk.gov.hmrc.incorporatedentityidentificationfrontend.httpparsers.ValidateIncorporatedEntityDetailsHttpParser.{DetailsMatched, DetailsMismatch}
import uk.gov.hmrc.incorporatedentityidentificationfrontend.services.{IncorporatedEntityInformationService, JourneyService, ValidateIncorporatedEntityDetailsService}
import uk.gov.hmrc.incorporatedentityidentificationfrontend.views.html.check_your_answers_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.ExecutionContext

@Singleton
class CheckYourAnswersController @Inject()(journeyService: JourneyService,
                                           incorporatedEntityInformationService: IncorporatedEntityInformationService,
                                           validateIncorporatedEntityDetailsService: ValidateIncorporatedEntityDetailsService,
                                           mcc: MessagesControllerComponents,
                                           view: check_your_answers_page,
                                           val authConnector: AuthConnector)
                                          (implicit val config: AppConfig,
                                           executionContext: ExecutionContext) extends FrontendController(mcc) with AuthorisedFunctions {

  def show(journeyId: String): Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        val identifiers = for {
          companyProfile <- incorporatedEntityInformationService.retrieveCompanyProfile(journeyId)
          ctutr <- incorporatedEntityInformationService.retrieveCtutr(journeyId)
        } yield {
          (companyProfile, ctutr)
        }

        identifiers.flatMap {
          case (Some(companyProfile), Some(ctutr)) =>
            val getServiceName = journeyService.getJourneyConfig(journeyId).map(_.pageConfig.optServiceName.getOrElse(config.defaultServiceName))

            getServiceName.map {
              serviceName =>
                Ok(
                  view(
                    serviceName,
                    routes.CheckYourAnswersController.submit(journeyId),
                    ctutr,
                    companyProfile.companyNumber,
                    journeyId
                  )
                )
            }
          case _ =>
            throw new InternalServerException("No data stored")
        }
      }
  }

  def submit(journeyId: String): Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        val identifiers = for {
          companyProfile <- incorporatedEntityInformationService.retrieveCompanyProfile(journeyId)
          ctutr <- incorporatedEntityInformationService.retrieveCtutr(journeyId)
        } yield {
          (companyProfile, ctutr)
        }

        identifiers.flatMap {
          case (Some(companyProfile), Some(ctutr)) =>
            validateIncorporatedEntityDetailsService.validateIncorporatedEntityDetails(companyProfile.companyNumber, ctutr).flatMap {
              case DetailsMatched =>
                incorporatedEntityInformationService.storeIdentifiersMatch(journeyId, identifiersMatch = true).map {
                  _ =>
                    Redirect(routes.BusinessVerificationController.startBusinessVerificationJourney(journeyId))
                }
              case DetailsMismatch =>
                incorporatedEntityInformationService.storeIdentifiersMatch(journeyId, identifiersMatch = false).map {
                  _ => Redirect(errorRoutes.CtutrMismatchController.show(journeyId))
                }
              case _ =>
                throw new InternalServerException("Incorporated entity details not found")
            }
          case _ =>
            throw new InternalServerException("No data stored")
        }
      }
  }

}
