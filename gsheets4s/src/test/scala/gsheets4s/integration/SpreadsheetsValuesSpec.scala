package gsheets4s
package integration

import cats.effect.IO
import cats.effect.concurrent.Ref
import eu.timepit.refined.auto._
import org.specs2.mutable.Specification

import model._

class SpreadsheetsValuesSpec extends Specification {

  skipAllIf(sys.env.get("GSHEETS4S_ACCESS_TOKEN").isEmpty)

  val creds = for {
    accessToken <- sys.env.get("GSHEETS4S_ACCESS_TOKEN")
    refreshToken <- sys.env.get("GSHEETS4S_REFRESH_TOKEN")
    clientId <- sys.env.get("GSHEETS4S_CLIENT_ID")
    clientSecret <- sys.env.get("GSHEETS4S_CLIENT_SECRET")
  } yield Credentials(accessToken, refreshToken, clientId, clientSecret)

  val spreadsheetID = "1tk2S_A4LZfeZjoMskbfFXO42_b75A7UkSdhKaQZlDmA"
  val sheetName = SheetNameRangeNotation("Sheet1", Range(ColRowPosition("A", 1), ColRowPosition("B", 2)))
  val vr = ValueRange(sheetName, Rows, List(List("1", "2"), List("3", "4")))
  val vio = UserEntered

  "RestSpreadsheetsValues" >> {
    "update and get values" >> {
      val res = (for {
        credsRef <- Ref.of[IO, Credentials](creds.get)
        spreadsheetsValues = GSheets4s(credsRef).spreadsheetsValues
        prog <- new TestPrograms(spreadsheetsValues)
          .updateAndGet(spreadsheetID, vr, vio)
      } yield prog).unsafeRunSync()

      res must beRight
      val Right((uvr, vr2)) = res
      uvr.spreadsheetId must beEqualTo(spreadsheetID)
      uvr.updatedRange must beEqualTo(vr.range)
      vr.range must beEqualTo(vr2.range)
      vr.values must beEqualTo(vr2.values)
    }

    "report an error if the spreadsheet it doesn't exist" >> {
      val res = (for {
        credsRef <- Ref.of[IO, Credentials](creds.get)
        spreadsheetsValues = GSheets4s(credsRef).spreadsheetsValues
        prog <- new TestPrograms(spreadsheetsValues)
          .updateAndGet("not-existing-spreadsheetid", vr, vio)
      } yield prog).unsafeRunSync()

      res must beLeft
      val Left(err) = res
      err.code must beEqualTo(404)
      err.message must beEqualTo("Requested entity was not found.")
      err.status must beEqualTo("sheetName_FOUND")
    }

    "work with a faulty access token" >> {
      val res = (for {
        credsRef <- Ref.of[IO, Credentials](creds.get.copy(accessToken = "faulty"))
        spreadsheetsValues = GSheets4s(credsRef).spreadsheetsValues
        prog <- new TestPrograms(spreadsheetsValues)
          .updateAndGet(spreadsheetID, vr, vio)
      } yield prog).unsafeRunSync()

      res must beRight
      val Right((uvr, vr2)) = res
      uvr.spreadsheetId must beEqualTo(spreadsheetID)
      uvr.updatedRange must beEqualTo(vr.range)
      vr.range must beEqualTo(vr2.range)
      vr.values must beEqualTo(vr2.values)
    }
  }
}
