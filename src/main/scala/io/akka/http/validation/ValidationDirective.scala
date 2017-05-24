package io.akka.http.validation

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.{Directive1, ValidationRejection}
import spray.json.DefaultJsonProtocol
import akka.http.scaladsl.server.Directives._

import scala.util.{Failure, Success, Try}

/**
  * Created by Alex on 5/23/17.
  */
object ValidationDirective extends SprayJsonSupport with DefaultJsonProtocol {

  import akka.http.scaladsl.server.Rejection

  final case class FieldRule[-M](fieldName: String, isInvalid: M => Boolean, errorMsg: String)
  final case class FieldErrorInfo(name: String, error: String)
  final case class ModelValidationRejection(invalidFields: Set[FieldErrorInfo]) extends Rejection

  implicit val validatedFieldFormat = jsonFormat2(FieldErrorInfo)

  private def caseClassFields[M <: Any](obj: AnyRef): Seq[(String, M)] = {
    val metaClass = obj.getClass
    metaClass.getDeclaredFields.map {
      field => {
        field.setAccessible(true)
        (field.getName, field.get(obj).asInstanceOf[M])
      }
    }
  }

  def validateModel[T, M <: Any](model: T, rules: FieldRule[M]*): Directive1[T] = {
    import scala.collection.mutable.Set
    val errorsSet: Set[FieldErrorInfo] = Set[FieldErrorInfo]()
    val keyValuePairs: Seq[(String, M)] = caseClassFields(model.asInstanceOf[AnyRef])
    Try {
      rules.map { rule =>
        keyValuePairs.find(_._1 == rule.fieldName) match {
          case None => throw new IllegalArgumentException(s"No such field for validation: ${rule.fieldName}")
          case Some(pair) => {
            if (rule.isInvalid(pair._2)) errorsSet += FieldErrorInfo(rule.fieldName, rule.errorMsg)
          }
        }
      }
      errorsSet.toSet[FieldErrorInfo]
    } match {
      case Success(set) => if (set.isEmpty) provide(model) else reject(ModelValidationRejection(set))
      case Failure(ex) => reject(ValidationRejection(ex.getMessage))
    }
  }
}