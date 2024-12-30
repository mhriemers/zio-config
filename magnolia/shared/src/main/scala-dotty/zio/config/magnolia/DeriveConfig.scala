package zio.config.magnolia

import zio.Config.*
import zio.config.*
import zio.config.derivation.*
import zio.config.magnolia.DeriveConfig.*
import zio.{Chunk, Config, LogLevel, NonEmptyChunk}

import java.net.URI
import java.time.{LocalDate, LocalDateTime, LocalTime, OffsetDateTime, *}
import java.util.UUID
import scala.annotation.{targetName, threadUnsafe}
import scala.compiletime.*
import scala.deriving.*

final case class DeriveConfig[A](desc: Config[A], metadata: Option[DeriveConfig.Metadata] = None) {
  def ??(description: String): DeriveConfig[A] =
    describe(description)

  def describe(description: String): DeriveConfig[A] =
    DeriveConfig(desc.??(description))

  def map[B](f: A => B): DeriveConfig[B] =
    DeriveConfig(desc.map(f))

  def mapAttempt[B](f: A => B): DeriveConfig[B] =
    DeriveConfig(desc.mapAttempt(f))

  def mapOrFail[B](f: A => Either[Config.Error, B]): DeriveConfig[B] =
    DeriveConfig(desc.mapOrFail(f))
}

object DeriveConfig {

  def apply[A](implicit ev: DeriveConfig[A]): DeriveConfig[A] =
    ev

  def from[A](desc: Config[A]) =
    DeriveConfig(desc, None)

  sealed trait Metadata {
    def originalName: String = this match {
      case Metadata.Object(name, _)    => name.originalName
      case Metadata.Product(name, _)   => name.originalName
      case Metadata.Coproduct(name, _) => name.originalName
    }

    def alternativeNames: List[String] = this match {
      case Metadata.Object(_, _)       => Nil
      case Metadata.Product(name, _)   => name.alternativeNames
      case Metadata.Coproduct(name, _) => name.alternativeNames
    }
  }

  object Metadata {
    final case class Object[T](name: ProductName, constValue: T)              extends Metadata
    final case class Product(name: ProductName, fields: List[FieldName])      extends Metadata
    final case class Coproduct(name: CoproductName, metadata: List[Metadata]) extends Metadata
  }

  final case class FieldName(originalName: String, alternativeNames: List[String], descriptions: List[String])
  final case class ProductName(originalName: String, alternativeNames: List[String], descriptions: List[String])
  final case class CoproductName(
    originalName: String,
    alternativeNames: List[String],
    descriptions: List[String],
    typeDiscriminator: Option[String]
  )

  given DeriveConfig[Secret]         = DeriveConfig.from(secret)
  given DeriveConfig[OffsetDateTime] = DeriveConfig.from(offsetDateTime)
  given DeriveConfig[LogLevel]       = DeriveConfig.from(logLevel)
  given DeriveConfig[String]         = DeriveConfig.from(string)
  given DeriveConfig[Boolean]        = DeriveConfig.from(boolean)
  given DeriveConfig[Int]            = DeriveConfig.from(int)
  given DeriveConfig[BigInt]         = DeriveConfig.from(bigInt)
  given DeriveConfig[Float]          = DeriveConfig.from(float)
  given DeriveConfig[Double]         = DeriveConfig.from(double)
  given DeriveConfig[BigDecimal]     = DeriveConfig.from(bigDecimal)
  given DeriveConfig[URI]            = DeriveConfig.from(uri)
  given DeriveConfig[zio.Duration]   = DeriveConfig.from(duration)
  given DeriveConfig[LocalDate]      = DeriveConfig.from(localDate)
  given DeriveConfig[LocalTime]      = DeriveConfig.from(localTime)
  given DeriveConfig[LocalDateTime]  = DeriveConfig.from(localDateTime)
  given DeriveConfig[Byte]           = DeriveConfig(Config.byte)
  given DeriveConfig[Short]          = DeriveConfig(Config.short)
  given DeriveConfig[UUID]           = DeriveConfig(Config.uuid)
  given DeriveConfig[Long]           = DeriveConfig(Config.long)

  given optDesc[A](using ev: DeriveConfig[A]): DeriveConfig[Option[A]] =
    DeriveConfig.from(ev.desc.optional)

  given eitherConfig[A, B](using evA: DeriveConfig[A], evB: DeriveConfig[B]): DeriveConfig[Either[A, B]] =
    DeriveConfig.from(evA.desc.orElseEither(evB.desc))

  given listDesc[A](using ev: DeriveConfig[A]): DeriveConfig[List[A]] =
    DeriveConfig.from(listOf(ev.desc))

  given seqDesc[A](using ev: DeriveConfig[A]): DeriveConfig[Seq[A]] =
    DeriveConfig.from(listOf(ev.desc).map(_.toSeq))

  given setDesc[A](using ev: DeriveConfig[A]): DeriveConfig[Set[A]] =
    DeriveConfig.from(setOf(ev.desc))

  given vectorDesc[A](using ev: DeriveConfig[A]): DeriveConfig[Vector[A]] =
    DeriveConfig.from(vectorOf(ev.desc))

  given chunkDesc[A](using ev: DeriveConfig[A]): DeriveConfig[Chunk[A]] =
    DeriveConfig.from(chunkOf(ev.desc))

  given nonEmptyChunkDesc[A](using ev: DeriveConfig[A]): DeriveConfig[NonEmptyChunk[A]] =
    DeriveConfig.from(nonEmptyChunkOf(ev.desc))

  given mapDesc[A](using ev: DeriveConfig[A]): DeriveConfig[Map[String, A]] =
    DeriveConfig.from(table(ev.desc))

  inline def summonDeriveConfigForCoProduct[T <: Tuple]: List[DeriveConfig[Any]] =
    inline erasedValue[T] match {
      case _: EmptyTuple => Nil
      case _: (t *: ts)  =>
        val desc = summonInline[DeriveConfig[t]]
        DeriveConfig[Any](
          desc.desc,
          desc.metadata
        ) :: summonDeriveConfigForCoProduct[ts]
    }

  inline def summonDeriveConfigAll[T <: Tuple]: List[DeriveConfig[_]] =
    inline erasedValue[T] match {
      case _: EmptyTuple => Nil
      case _: (t *: ts)  => summonInline[DeriveConfig[t]] :: summonDeriveConfigAll[ts]
    }

  inline def labelsOf[T <: Tuple]: List[String] =
    inline erasedValue[T] match {
      case _: EmptyTuple => Nil
      case _: (t *: ts)  => constValue[t].toString :: labelsOf[ts]
    }

  inline def customNamesOf[T]: List[String] =
    inline Macros.nameOf[T] match {
      case Nil   => Nil
      case names => names.map(_.name)
    }

  inline def customFieldNamesOf[T]: Map[String, name] =
    inline Macros.fieldNameOf[T] match {
      case Nil   => Map.empty[String, name]
      case names => names.flatMap { case (str, nmes) => nmes.map(name => (str, name)) }.toMap
    }

  inline given derived[T](using m: Mirror.Of[T]): DeriveConfig[T] =
    inline m match
      case _: Mirror.SumOf[T] =>
        val coproductName: CoproductName =
          CoproductName(
            originalName = constValue[m.MirroredLabel],
            alternativeNames = customNamesOf[T],
            descriptions = Macros.documentationOf[T].map(_.describe),
            typeDiscriminator = Macros.discriminator[T].headOption.map(_.keyName)
          )

        val subClassDescriptions = summonDeriveConfigForCoProduct[m.MirroredElemTypes]
        val desc                 = mergeAllProducts(subClassDescriptions.map(castTo[DeriveConfig[T]]), coproductName.typeDiscriminator)

        DeriveConfig.from(tryAllKeys(desc.desc, None, coproductName.alternativeNames))

      case m: Mirror.ProductOf[T] =>
        val productName =
          ProductName(
            originalName = constValue[m.MirroredLabel],
            alternativeNames = customNamesOf[T],
            descriptions = Macros.documentationOf[T].map(_.describe)
          )

        val originalFieldNamesList = labelsOf[m.MirroredElemLabels]
        val customFieldNameMap     = customFieldNamesOf[T]
        val documentations         = Macros.fieldDocumentationOf[T].toMap
        val fieldNames             = mapOriginalNames(originalFieldNamesList, documentations, customFieldNameMap)

        @threadUnsafe lazy val fieldConfigsWithDefaultValues = {
          val fieldConfigs          = summonDeriveConfigAll[m.MirroredElemTypes].asInstanceOf[List[DeriveConfig[Any]]]
          val fieldAndDefaultValues = Macros.defaultValuesOf[T].toMap
          addDefaultValues(fieldAndDefaultValues, originalFieldNamesList, fieldConfigs)
        }

        mergeAllFields(
          fieldConfigsWithDefaultValues,
          productName,
          fieldNames,
          lst => m.fromProduct(Tuple.fromArray(lst.toArray[Any]))
        )

  private def mapOriginalNames(
    names: List[String],
    docs: Map[String, List[describe]],
    customFieldNames: Map[String, name]
  ): List[FieldName] =
    names.foldRight(List.empty[FieldName]) { (str, list) =>
      val alternativeNames = customFieldNames.get(str).map(v => List(v.name)).getOrElse(Nil)
      val descriptions     = docs.get(str).map(_.map(_.describe)).getOrElse(Nil)
      FieldName(str, alternativeNames, descriptions) :: list
    }

  def mergeAllProducts[T](
    allDescs: => List[DeriveConfig[T]],
    typeDiscriminator: Option[String]
  ): DeriveConfig[T] = {
    val desc =
      typeDiscriminator match {
        case None =>
          allDescs
            .map(desc =>
              desc.metadata match {
                case Some(Metadata.Product(productName, fields)) if (fields.nonEmpty) =>
                  tryAllKeys(desc.desc, Some(productName.originalName), productName.alternativeNames)
                case Some(_)                                                          => desc.desc
                case None                                                             => desc.desc
              }
            )
            .reduce(_ orElse _)

        case Some(keyName) =>
          Config
            .string(keyName)
            .switch(
              allDescs.flatMap { desc =>
                desc.metadata match {
                  case Some(Metadata.Object(name, value)) =>
                    List(name.originalName -> Config.Constant(value.asInstanceOf[T]))

                  case Some(m) =>
                    (m.originalName :: m.alternativeNames).map(_ -> desc.desc)

                  case None => Nil
                }
              }*
            )
      }

    DeriveConfig.from(desc)
  }

  def addDefaultValues(
    defaultValues: Map[String, Any],
    fieldNames: List[String],
    descriptors: List[DeriveConfig[Any]]
  ): List[DeriveConfig[_]] =
    descriptors.zip(fieldNames).map { case (desc, fieldName) =>
      defaultValues.get(fieldName) match {
        case Some(any) => DeriveConfig(desc.desc.withDefault(any), desc.metadata)
        case None      => desc
      }
    }

  @deprecated("use overloaded method without providing `g`")
  def mergeAllFields[T](
    allDescs: => List[DeriveConfig[_]],
    productName: ProductName,
    fieldNames: => List[FieldName],
    f: List[Any] => T,
    g: T => List[Any]
  ): DeriveConfig[T] =
    mergeAllFields[T](allDescs, productName, fieldNames, f)

  def mergeAllFields[T](
    allDescs: => List[DeriveConfig[_]],
    productName: ProductName,
    fieldNames: List[FieldName],
    f: List[Any] => T
  ): DeriveConfig[T] =
    if fieldNames.isEmpty then { // if there are no fields in the product then the value is the name of the product itself
      val tryAllPaths =
        (productName.originalName :: productName.alternativeNames)
          .map(n => zio.Config.constant(n))
          .reduce(_ orElse _)

      DeriveConfig(
        tryAllPaths.map[T](_ => f(Nil)),
        Some(Metadata.Object[T](productName, f(Nil))) // We propogate the info that product was actually an object
      )
    } else {
      val listOfDesc =
        fieldNames.zip(allDescs).map { case (fieldName, desc) =>
          val fieldDesc = tryAllKeys(desc.desc, Some(fieldName.originalName), fieldName.alternativeNames)
          fieldName.descriptions.foldRight(fieldDesc)((doc, desc) => desc ?? doc)
        }

      val descOfList =
        Config.collectAll(listOfDesc.head, listOfDesc.tail*)

      DeriveConfig(descOfList.map(f), Some(Metadata.Product(productName, fieldNames)))
    }

  def tryAllKeys[A](
    desc: Config[A],
    originalKey: Option[String],
    alternativeKeys: List[String]
  ): Config[A] =
    alternativeKeys match {
      case Nil  => originalKey.fold(desc)(desc.nested(_))
      case keys => keys.view.map(desc.nested(_)).reduce(_ orElse _)
    }

  def castTo[T](a: Any): T =
    a.asInstanceOf[T]
}
