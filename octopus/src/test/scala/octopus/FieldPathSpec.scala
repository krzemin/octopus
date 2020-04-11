package octopus

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec


class FieldPathSpec extends AnyWordSpec with Matchers{

  val PNil = FieldPath.empty

  "FieldPath" when {

    "asString" should {

      "support empty path" in {

        PNil.asString mustBe ""
      }

      "support field names" in {

        (FieldLabel(Symbol("label1")) :: PNil).asString mustBe "label1"
      }

      "support collection indexes in array notation" in {

        (FieldLabel(Symbol("label1")) :: CollectionIndex(5) :: FieldLabel(Symbol("label2")) :: PNil).asString mustBe
          "label1[5].label2"
      }

      "support map keys in array notation" in {

        (FieldLabel(Symbol("label1")) :: MapKey("some key") :: FieldLabel(Symbol("label2")) :: PNil).asString mustBe
          "label1[some key].label2"
      }
    }
  }

}
