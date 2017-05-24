package io.akka.http.validation

import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{FunSuite, Matchers}
import akka.http.scaladsl.server.Directives._
import io.akka.http.validation.ValidationDirective._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{RejectionHandler, ValidationRejection}
/**
  * Created by Alex on 5/23/17.
  */
class ValidationDirectiveSuite extends FunSuite with ScalatestRouteTest with Matchers with SprayJsonSupport {

  case class Book(title: String, author: String, pagesQty: Int)
  implicit val bookFormat = jsonFormat3(Book)

  implicit def myRejectionHandler = RejectionHandler.newBuilder()
    .handle { case mvr @ ModelValidationRejection(_) =>
      complete(HttpResponse(
        StatusCodes.BadRequest,
        entity = HttpEntity(mvr.invalidFields.toJson.toString)
          .withContentType(ContentTypes.`application/json`)))
    }.handle { case vr: ValidationRejection =>
    complete(HttpResponse(
      StatusCodes.BadRequest,
      entity = HttpEntity(vr.message)
        .withContentType(ContentTypes.`application/json`)))
  }
    .result()

  val titleRule = FieldRule("title",
    (title: String) => if (title.isEmpty) true else false,
    "title can not be empty"
  )

  val pagesQtyRule = FieldRule("pagesQty",
    (qty: Int) => if (qty < 10) true else false,
    "page quantity must be more than 10"
  )

  val authorRule = FieldRule("author",
    (author: String) => if (author.isEmpty || author.length > 60) true else false,
    "author name length must be > 0 and < 60"
  )

  val ruleForNoneExistingField = FieldRule("absent",
    (author: String) => if (1 == 2) true else false,
    "none sence"
  )

  val routes = {
    pathPrefix("books") {
      post {
        entity(as[Book]) { book =>
          validateModel(book, titleRule, pagesQtyRule, authorRule) { validatedBook =>
            complete("ok")
          }
        }
      } ~ {
        put {
          entity(as[Book]) { book =>
            validateModel(book, ruleForNoneExistingField) { validatedBook =>
              complete("ok")
            }
          }
        }
      }
    }
  }

  test("Positive validation test") {
    Post("/books", Book("", "", 5)) ~> routes ~> check {
      assert(rejection === ModelValidationRejection(Set(
        FieldErrorInfo("title", "title can not be empty"),
        FieldErrorInfo("author", "author name length must be > 0 and < 60"),
        FieldErrorInfo("pagesQty", "page quantity must be more than 10")
      )))
    }
  }

  test("Check rule for none existing field") {
    Put("/books", Book("Nice Book", "Foo Bar", 100)) ~> routes ~> check {
      assert(rejection === ValidationRejection("No such field for validation: absent"))
    }
  }

}