package munit

import scala.concurrent.Future
import scala.concurrent.Promise

class AsyncFixtureOrderSuite extends FunSuite {
  val latch = Promise[Unit]
  var completedFromTest = Option.empty[Boolean]
  var completedFromTeardown = Option.empty[Boolean]

  val latchOnTeardown = FunFixture.async[String](
    setup = { test => Future.successful(test.name) },
    teardown = { name =>
      implicit val ec = munitExecutionContext
      Future {
        completedFromTeardown = Some(latch.trySuccess(()));
      }
    }
  )

  override def afterAll(): Unit = {
    // promise was completed first form the test
    assertEquals(completedFromTest, Some(true))
    // and then from the teardown
    assertEquals(completedFromTeardown, Some(false))
  }

  latchOnTeardown.test("teardown runs only after test completes") { _ =>
    import scala.concurrent.ExecutionContext.Implicits.global
    Future {
      // Simulate some work here, which increases the certainty that this test
      // will fail by design and not by lucky scheduling if the happens-before
      // relationship between the test and teardown is removed.
      Thread.sleep(50)
      completedFromTest = Some(latch.trySuccess(()))
    }
  }
}
