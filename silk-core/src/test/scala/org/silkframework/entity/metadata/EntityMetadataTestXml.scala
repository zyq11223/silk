package org.silkframework.entity.metadata

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.entity.{Entity, EntitySchema, Path, Restriction}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.util.{DPair, Uri}

import scala.xml.Node

class EntityMetadataTestXml extends FlatSpec with Matchers {
  val schema = EntitySchema(typeUri = Uri(""), typedPaths = IndexedSeq(Path("path1").asStringTypedPath, Path("path2").asStringTypedPath), filter = Restriction.empty)

  implicit val throwableTag = classOf[Throwable]

  val testException = new Exception("test", new IllegalArgumentException("some cause"))
  val metadata = new EntityMetadataXml(
    Map(EntityMetadata.FAILURE_KEY -> LazyMetadataXml(testException, ExceptionSerializer()))
  )

  val entity1 = Entity("http://silk-framework.com/example", IndexedSeq(Seq("value1", "value2"), Seq("value3")), schema, IndexedSeq.empty, metadata)

  val entity2 = Entity("http://silk-framework.com/example", IndexedSeq(Seq("dÃ©clarÃ©s", "v2"), Seq("v3")), schema)

  it should "accept exception object as metadata and recognize it as a failed entity" in{
    val lazyMetadata = entity1.metadata.getLazyMetadata(EntityMetadata.FAILURE_KEY).asInstanceOf[LazyMetadataXml[Throwable]]
    val copy = lazyMetadata.copy(obj = None, serial = Some(lazyMetadata.serialized))
    copy.metadata.isDefined shouldBe true
    copy.metadata.get.getClass.getCanonicalName shouldBe testException.getClass.getCanonicalName
    copy.metadata.get.getMessage shouldBe testException.getMessage
    copy.metadata.get.getStackTrace.length shouldBe testException.getStackTrace.length
    copy.metadata.get.getCause should not be  null.asInstanceOf[Throwable]
    copy.metadata.get.getCause.getClass shouldBe testException.getCause.getClass
    copy.metadata.get.getCause.getMessage shouldBe testException.getCause.getMessage
    entity1.hasFailed shouldBe true
  }

  it should "be able to create an arbitrary metadata object and handle it normally" in{
    val serializer = new XmlMetadataSerializer[DPair[String]] {
      /**
        * The identifier used to define metadata objects in the map of [[EntityMetadata]]
        */
      override def metadataId: String = "pair_test_metadata"

      override def read(value: Node)(implicit readContext: ReadContext): DPair[String] = {
        val source = (value \ "Source").text.trim
        val target = (value \ "Target").text.trim
        new DPair[String](source, target)
      }

      override def write(value: DPair[String])(implicit writeContext: WriteContext[Node]): Node = {
        <Pair>
          <Source>{value.source}</Source>
          <Target>{value.target}</Target>
        </Pair>
      }
    }

    implicit val dpairTag = classOf[DPair[String]]
    implicit val Tag = classOf[LazyMetadataXml[DPair[String]]]
    val pair =  new DPair("s", "t")
    val metadata = new EntityMetadataXml(Map(serializer.metadataId -> LazyMetadataXml(pair, serializer)))
    val entity = entity2.copy(metadata = metadata)

    val lazyMetadata = entity.metadata.getLazyMetadata[DPair[String]](serializer.metadataId)
    lazyMetadata.serialized.toString.contains("<Target>t</Target>") shouldBe true
    lazyMetadata.metadata.get shouldEqual pair
  }
}