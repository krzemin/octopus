package octopus

import org.scalatest.{MustMatchers, WordSpec}


class FieldPathSpec extends WordSpec with MustMatchers{

  val PNil = FieldPath.empty

  "FieldPath" when {

    "asString" should {

      "support field names" in {

        (FieldLabel('label1) :: PNil).asString mustBe "label1"
      }

      "support collection indexes in array notation" in {

        (FieldLabel('label1) :: CollectionIndex(5) :: FieldLabel('label2) :: PNil).asString mustBe
          "label1[5].label2"
      }

      "support map keys in array notation" in {

        (FieldLabel('label1) :: MapKey("some key") :: FieldLabel('label2) :: PNil).asString mustBe
          "label1[some key].label2"
      }
    }
  }

}
