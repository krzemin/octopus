package octopus

import org.scalatest.{MustMatchers, WordSpec}
import octopus.syntax._

class NormalizationSpec
  extends WordSpec with MustMatchers with Fixtures {

  "Normalization" when {

    "given primitive types" should {

      "normalize with identity function" in {

        1.normalize mustBe 1
        "abc".normalize mustBe "abc"
        true.normalize mustBe true
        10.0.normalize mustBe 10.0
        10.0f.normalize mustBe 10.0f
        'x'.normalize mustBe 'x'
        3.toByte.normalize mustBe 3.toByte
        66.toShort.normalize mustBe 66.toShort
        ().normalize mustBe (())
      }
    }

  }


}
