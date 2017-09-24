/**
 * Wire
 * Copyright (C) 2017 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.waz.zclient.tracking

import com.waz.api.EphemeralExpiration
import com.waz.api.Invitations.PersonalToken
import com.waz.model.ConversationData.ConversationType
import com.waz.model.{ConversationData, Mime}
import com.waz.utils.returning
import com.waz.zclient.controllers.SignInController.{Email, Login, Register, SignInMethod}
import com.waz.zclient.tracking.ContributionEvent.Action
import org.json.JSONObject

sealed trait TrackingEvent {
  val name: String
  val props: Option[JSONObject]
}

case class OptEvent(enabled: Boolean) extends TrackingEvent {
  override val name = s"settings.opted_${if (enabled) "in" else "out"}_tracking"
  override val props = None
}

//TODO - handle generic invitation tokens
case class SignInEvent(method: SignInMethod, invitation: Option[PersonalToken]) extends TrackingEvent {
  override val name = method.signType match {
    case Register => "registration.succeeded"
    case Login    => "account.logged_in"
  }
  override val props = Some(returning (new JSONObject()) { o =>
    val input = if (method.inputType == Email) "email" else "phone"
    val context = method.signType match {
      case Login                          => input
      case Register if invitation.isEmpty => input
      case _                              => s"personal_invite_$input"
      //TODO - handle generic invitation tokens
//      case _                              => s"generic_invite_$input"
    }
    o.put("context", context)
  })
}

case class ContributionEvent(action: Action, conversationType: ConversationType, ephExp: EphemeralExpiration, withOtto: Boolean) extends TrackingEvent {
  override val name = "contributed"

  override val props = Some(returning(new JSONObject()) { o =>
    o.put("action",               action.name)
    o.put("conversation_type",    if (conversationType == ConversationType.Group) "group" else "1:1")
    o.put("with_otto",            withOtto)
    o.put("is_ephemeral",         ephExp != EphemeralExpiration.NONE) //TODO is this flag necessary?
    o.put("ephemeral_expiration", ephExp.duration().toSeconds.toString)
  })
}

object ContributionEvent {
  case class Action(name: String)
  object Action {
    lazy val Text      = Action("text")
    lazy val Ping      = Action("ping")
    lazy val AudioCall = Action("audio_call")
    lazy val VideoCall = Action("video_call")
    lazy val Photo     = Action("photo")
    lazy val Audio     = Action("audio")
    lazy val Video     = Action("video")
    lazy val File      = Action("file")
    lazy val Location  = Action("location")
  }

  def apply(action: Action, conv: ConversationData, withOtto: Boolean): ContributionEvent =
    ContributionEvent(action, conv.convType, conv.ephemeral, withOtto)

  def fromMime(mime: Mime) = {
    import Action._
    mime match {
      case Mime.Image() => Photo
      case Mime.Audio() => Audio
      case Mime.Video() => Video
      case _            => File
    }
  }
}

/*
TODO remove these
only here for easier reference to old event names
REGISTRATION_WEEK("registration__week"),
CONNECT__HAS_CONTACT("connect__has_contact"),

SESSION_FIRST_SESSION("firstSession"),
SESSION_SEARCHED_FOR_PEOPLE("searchedPeople"),

APP_LAUNCH_MECHANISM("mechanism"),

CONNECT_INVITATION_METHOD("invitationMethod"),

GROUP_CONVERSATION_CREATED("groupConversationCreated"),

REGISTRATION_SHARE_CONTACTS_ALLOWED("allowed"),

REGISTRATION_FAIL_REASON("reason"),

SIGN_IN_AFTER_PASSWORD_RESET("afterPasswordReset"),
SIGN_IN_ERROR_CODE("reason"),

BLOCKING("blocking"),
UNBLOCKING("unBlocking"),

NEW_MEMBERS("new_members"),
REMOVED_CONTACT("removedContact"),
LEAVE_GROUP("leaveGroup"),
CONFIRMATION_RESPONSE("confirmationResponse"),

PRIVACY_POLICY_SOURCE("source"),
TOS_SOURCE("source"),
RESET_PASSWORD_LOCATION("resetLocation"),

CALLING_VERSION("version"),
CALLING_DIRECTION("direction"),
CONVERSATION_TYPE("conversation_type"),
CALLING_CONVERSATION_PARTICIPANTS("conversation_participants"),
CALLING_CALL_PARTICIPANTS("call_participants"),
CALLING_MAX_CALL_PARTICIPANTS("max_call_participants"),
CALLING_END_REASON("reason"),
CALLING_APP_IS_ACTIVE("app_is_active"),
WITH_OTTO("with_otto"),
WITH_BOT("with_bot"),
IS_LAST_MESSAGE("is_last_message"),
IS_EPHEMERAL("is_ephemeral"),
EPHEMERAL_EXPIRATION("ephemeral_expiration"),

FILE_SIZE_BYTES("file_size_bytes"),

IS_RESENDING("is_resending"),
FROM_SEARCH("from_search"),

FIELD("field"),
ACTION("action"),
VIEW("view"),
CONTEXT("context"),
STATE("state"),
DESCRIPTION("description"),
SOURCE("source"),
TARGET("target"),
TYPE("type"),
METHOD("method"),
OUTCOME("outcome"),
SECTION("section"),
POSITION("position"),
ERROR("error"),
ERROR_MESSAGE("error_message"),
VALUE("value"),
USER("user"),
EFFECT("effect"),
LENGTH("length"),
BY_USERNAME("by_username_only"),
DURATION("duration"),

AVS("avs"),

NAVIGATION_HINT_VISIBLE("hint_visible"),

EXCEPTION_TYPE("exceptionType"),
EXCEPTION_DETAILS("exceptionDetails"),

// AN-4011: Temporary attributes
REFERRAL_TOKEN("REFERRAL_TOKEN"),
USER_TOKEN("USER_TOKEN"),
SKETCH_SOURCE("sketch_source"),

DAY("day"),
MONTH("month"),
YEAR("year"),

GCM_SUCCESS("successful_gcm_notifications"),
GCM_FAILED("failed_gcm_notifications"),
GCM_RE_REGISTER("registration_retries"),
TOTAL_PINGS("total_pings"),
RECEIVED_PONGS("received_pongs"),
PING_INTERVAL("ping_interval"),

IS_EMPTY("is_empty"),
WITH_SEARCH_RESULT("with_search_result"),

AVS_METRICS_FULL("avs_metrics_full")*/
