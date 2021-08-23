package cli

import org.scalatest.{FunSuite, Matchers}

class UpdatePricesTest extends FunSuite with Matchers {

  test("testPrettyRoundPrice") {
    UpdatePrices.prettyRoundPrice(2.23) should be(2.25)

    UpdatePrices.prettyRoundPrice(1.003) should be(1)

    UpdatePrices.prettyRoundPrice(14.85) should be(14.85)
  }

  test("should update price") {
    UpdatePrices.updatePrice(14.85) should be(16.35)

    UpdatePrices.updatePrice(49.75) should be(49.75)

    UpdatePrices.updatePrice(27.50) should be(27.5)


  }

}
