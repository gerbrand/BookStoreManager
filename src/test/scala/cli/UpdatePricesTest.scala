package cli

import org.scalatest.{FunSuite, Matchers}

class UpdatePricesTest extends FunSuite with Matchers {

  test("testPrettyRoundPrice") {
    UpdatePrices.prettyRoundPrice(2.23) should be(2.25)

    UpdatePrices.prettyRoundPrice(1.003) should be(1)
  }

}
