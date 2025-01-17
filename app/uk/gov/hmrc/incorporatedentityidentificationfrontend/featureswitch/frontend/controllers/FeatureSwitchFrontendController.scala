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

package uk.gov.hmrc.incorporatedentityidentificationfrontend.featureswitch.frontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.incorporatedentityidentificationfrontend.config.AppConfig
import uk.gov.hmrc.incorporatedentityidentificationfrontend.featureswitch.core.config.FeatureSwitching
import uk.gov.hmrc.incorporatedentityidentificationfrontend.featureswitch.frontend.services.FeatureSwitchRetrievalService
import uk.gov.hmrc.incorporatedentityidentificationfrontend.featureswitch.frontend.views.html.feature_switch
import uk.gov.hmrc.incorporatedentityidentificationfrontend.models.PageConfig
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.ExecutionContext

@Singleton
class FeatureSwitchFrontendController @Inject()(featureSwitchService: FeatureSwitchRetrievalService,
                                                featureSwitchView: feature_switch,
                                                mcc: MessagesControllerComponents
                                               )(implicit ec: ExecutionContext,
                                                 appConfig: AppConfig) extends FrontendController(mcc) with FeatureSwitching with I18nSupport {

  private val defaultPageConfig = PageConfig(None, "vrs", appConfig.vatRegFeedbackUrl, "/")

  def show: Action[AnyContent] = Action.async {
    implicit req =>
      featureSwitchService.retrieveFeatureSwitches().map {
        featureSwitches =>
          Ok(featureSwitchView(defaultPageConfig, featureSwitches, routes.FeatureSwitchFrontendController.submit))
      }
  }

  def submit: Action[Map[String, Seq[String]]] = Action.async(parse.formUrlEncoded) {
    implicit req =>
      featureSwitchService.updateFeatureSwitches(req.body.keys).map {
        featureSwitches =>
          Ok(featureSwitchView(defaultPageConfig, featureSwitches, routes.FeatureSwitchFrontendController.submit))
      }
  }
}
