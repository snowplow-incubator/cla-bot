/*
 * Copyright (c) 2018-2018 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
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
