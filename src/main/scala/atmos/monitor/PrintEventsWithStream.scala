/* PrintEventsWithStream.scala
 * 
 * Copyright (c) 2013-2014 linkedin.com
 * Copyright (c) 2013-2015 zman.io
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
package atmos.monitor

import java.io.PrintStream

/**
 * An event monitor that prints information about retry events to a stream.
 *
 * @param stream The stream that this event monitor prints to.
 * @param retryingAction The action that is performed when a retrying event is received.
 * @param interruptedAction The action that is performed when an interrupted event is received.
 * @param abortedAction The action that is performed when an aborted event is received.
 */
case class PrintEventsWithStream(
  stream: PrintStream,
  retryingAction: PrintAction = PrintEventsWithStream.defaultRetryingAction,
  interruptedAction: PrintAction = PrintEventsWithStream.defaultInterruptedAction,
  abortedAction: PrintAction = PrintEventsWithStream.defaultAbortedAction)
  extends PrintEvents {

  /* Pass the message to the underlying stream. */
  override def printMessage(message: String) = stream.println(message)

  /* Pass the message and throwable to the underlying stream. */
  override def printMessageAndStackTrace(message: String, thrown: Throwable) = stream synchronized {
    stream.println(message)
    thrown.printStackTrace(stream)
  }

}

/**
 * Definitions associated with event monitors that print information about retry events as text.
 */
object PrintEventsWithStream {

  import PrintAction._

  /** The action that is performed by default when a retrying event is received. */
  val defaultRetryingAction: PrintAction = PrintMessage

  /** The action that is performed by default when an interrupted event is received. */
  val defaultInterruptedAction: PrintAction = PrintMessageAndStackTrace

  /** The action that is performed by default when an aborted event is received. */
  val defaultAbortedAction: PrintAction = PrintMessageAndStackTrace

}
