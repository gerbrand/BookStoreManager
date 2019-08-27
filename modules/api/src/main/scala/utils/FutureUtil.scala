package utils

import scala.collection.generic.CanBuildFrom
import scala.concurrent.{ExecutionContext, Future}

object FutureUtil {

  /**
   * Sequential traverse: http://stackoverflow.com/questions/28514621/is-there-a-build-in-slow-future-traverse-version
   */
  def sequentialTraverse[A, B, M[X] <: TraversableOnce[X]](
    traversable: M[A]
  )(
    mapper: A => Future[B]
  )(
    implicit
    canBuildFrom: CanBuildFrom[M[A], B, M[B]],
    executorContext: ExecutionContext
  ): Future[M[B]] =
    traversable
      .foldLeft(Future.successful(canBuildFrom(traversable))) { (out, a) =>
        for {
          result <- out
          b      <- mapper(a) // only execute mapper after the rest (result) has resolved
        } yield result += b
      }
      .map(_.result())
}
