package clabot

import cats.NonEmptyParallel

// taken from https://github.com/typelevel/cats/issues/2233

sealed trait NonEmptyParallel1[F[_]] extends Serializable {
  type A[_]
  def nonEmptyParallel: NonEmptyParallel[F, A]
}

object NonEmptyParallel1 {
  type Aux[F[_], A0[_]] = NonEmptyParallel1[F] { type A[x] = A0[x] }

  implicit def nonEmptyParallel1Instance[F[_], A0[_]](
      implicit P: NonEmptyParallel[F, A0]): NonEmptyParallel1.Aux[F, A0] =
    new NonEmptyParallel1[F] {
      type A[x] = A0[x]
      val nonEmptyParallel = P
    }

  implicit def nonEmptyParallelFromNonEmptyParallel1[F[_]](
      implicit P: NonEmptyParallel1[F]): NonEmptyParallel[F, P.A] =
    P.nonEmptyParallel
}
