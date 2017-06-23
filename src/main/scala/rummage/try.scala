/* try.scala
 * 
 * Copyright (c) 2014-2015 linkedin.com
 * Copyright (c) 2014-2015 zman.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rummage

import scala.collection.{IterableLike, generic, mutable}
import scala.language.{higherKinds, postfixOps}
import scala.util.control.NoStackTrace
import scala.util.{Failure, Success, Try}

/**
 * An extension to the `scala.util.Try` interface to support aggregated try operations.
 *
 * This API extends Scala's `Try` to support situations where you have `Try[_ <: Iterable[_]]` and want to attempt an
 * operation on all elements of the `Iterable` contained in the `Try` and flatten the results of those operations into
 * a single `Try`.
 *
 * This API provides two mechanisms by which you can attempt operations on aggregations:
 *
 *  - `(_: Try[_ <: Iterable[_]]).onEach` exposes combinators that will *fail fast*, meaning that it will apply a
 * function to each member of the aggregate and will abort immediately if it encounters a failure on any member.
 *
 *  - `(_: Try[_ <: Iterable[_]]).onEvery` exposes combinators that will *fail eventually*, meaning that it will apply
 * a function to every member of the aggregate, collecting any failures it encounters and returning them after all
 * elements have been processed.
 *
 * Additionally, this API extends `scala.util.Failure` with two new failure abstractions:
 *
 *  - `Problem` provides a factory and extractor for failures that carry only a message and no stack trace.
 *
 *  - `Failures` provides a factory and extractor for failures that are only aggregations of other failures.
 */
object TryAll {

  /** Extensions to the `scala.util.Try` interface to support attempts over aggregations. */
  implicit class TryExtensions[T](val self: Try[T]) extends AnyVal {

    /** Creates a projection that exposes *fail-fast* combinators over the elements contained in a `Try`. */
    def onEach[E, C[XX] <: IterableLike[XX, C[XX]]](implicit ev: T <:< C[E]): TryProjection[E, C] =
      new TryEachProjection(self, ev)

    /** Creates a projection that exposes *fail-eventually* combinators over the elements contained in a `Try`. */
    def onEvery[E, C[XX] <: IterableLike[XX, C[XX]]](implicit ev: T <:< C[E]): TryProjection[E, C] =
      new TryEveryProjection(self, ev)

  }

  /** A projection over `scala.util.Try` that exposes combinators over aggregations. */
  trait TryProjection[E, C[XX] <: IterableLike[XX, C[XX]]] {

    /** Applies `f` to all elements in an aggregation, returning a single value containing the results. */
    def map[U, That](f: E => U)(implicit cbf: generic.CanBuildFrom[C[E], U, That]): Try[That]

    /** Applies `f` to all elements in an aggregation, returning a single value containing the flattened results. */
    def flatMap[U, That](f: E => Try[U])(implicit cbf: generic.CanBuildFrom[C[E], U, That]): Try[That]

  }

  /** A variation on `scala.util.Failure` that contains only a message and no stack trace. */
  object Problem {

    /** Creates a failure that contains only a message and a cause with no stack trace. */
    def apply[T](message: String, cause: Throwable): Failure[T] = apply(message, Some(cause))

    /** Creates a failure that contains only a message and an optional cause with no stack trace. */
    def apply[T](message: String, cause: Option[Throwable] = None): Failure[T] = Failure {
      val e = new ProblemException(message)
      cause foreach e.initCause
      e
    }

    /** Extracts the contents of a failure that contains only a message. */
    def unapply[T](failure: Failure[T]): Option[String] = failure.exception match {
      case e: ProblemException => Some(e.getMessage)
      case _ => None
    }

  }

  /** Throwable representation of a failure that contains only a message and an optional cause with no stack trace. */
  final class ProblemException(message: String)
    extends RuntimeException(message) with NoStackTrace

  /** A variation on `scala.util.Failure` that aggregates multiple other failures. */
  object Failures {

    /** Creates a failure that contains multiple other failures. */
    def apply[T](failures: Vector[Failure[_]]): Failure[T] = Failure(new FailuresException(failures))

    /** Extracts the contents of a failure that contains multiple other failures. */
    def unapply[T](failure: Failure[T]): Option[Vector[Failure[_]]] = failure.exception match {
      case e: FailuresException => Some(e.failures)
      case _ => None
    }

  }

  /** Throwable representation of a failure that contains multiple other failures. */
  final class FailuresException(val failures: Vector[Failure[_]])
    extends RuntimeException(s"${failures.size} failures") with NoStackTrace

  /** A projection that exposes aggregations inside an attempt in a fail-fast manner. */
  private class TryEachProjection[T, E, C[XX] <: IterableLike[XX, C[XX]]](self: Try[T], ev: T => C[E])
    extends TryProjection[E, C] {

    /* Fold over the elements, only evaluating an element if all previous elements succeeded. */
    override def map[U, That](f: E => U)(implicit cbf: generic.CanBuildFrom[C[E], U, That]) =
      self flatMap { value =>
        ((Success(cbf()): Try[mutable.Builder[U, That]]) /: ev(value)) { (builder, element) =>
          builder flatMap { b => Try {f(element)} map {b += _} }
        } map {_.result()}
      }

    /* Fold over the elements, only evaluating an element if all previous elements succeeded. */
    override def flatMap[U, That](f: E => Try[U])(implicit cbf: generic.CanBuildFrom[C[E], U, That]) =
      self flatMap { value =>
        ((Success(cbf()): Try[mutable.Builder[U, That]]) /: ev(value)) { (builder, element) =>
          builder flatMap { b => (Try {f(element)} flatten) map {b += _} }
        } map {_.result()}
      }

  }

  /** A projection that exposes aggregations inside an attempt in a fail-eventually manner. */
  private class TryEveryProjection[T, E, C[XX] <: IterableLike[XX, C[XX]]](self: Try[T], ev: T => C[E])
    extends TryProjection[E, C] {

    /* Fold over the elements, evaluating every element and collecting any failures. */
    override def map[U, That](f: E => U)(implicit cbf: generic.CanBuildFrom[C[E], U, That]) =
      self flatMap { value =>
        val (builder, failures) = ((cbf(), Vector[Failure[_]]()) /: ev(value)) { (state, element) =>
          val (builder, failures) = state
          Try {f(element)} match {
            case Success(value) => (builder += value, failures)
            case failure: Failure[_] => (builder, failures :+ failure)
          }
        }
        if (failures isEmpty) Success(builder.result()) else Failures(failures)
      }

    /* Fold over the elements, evaluating every element and collecting any failures. */
    override def flatMap[U, That](f: E => Try[U])(implicit cbf: generic.CanBuildFrom[C[E], U, That]) =
      self flatMap { value =>
        val (builder, failures) = ((cbf(), Vector[Failure[_]]()) /: ev(value)) { (state, element) =>
          val (builder, failures) = state
          Try {f(element)} flatten match {
            case Success(value) => (builder += value, failures)
            case failure: Failure[_] => (builder, failures :+ failure)
          }
        }
        if (failures isEmpty) Success(builder.result()) else Failures(failures)
      }

  }

}