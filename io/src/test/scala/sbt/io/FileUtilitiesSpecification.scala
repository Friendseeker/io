/*
 * sbt IO
 * Copyright Scala Center, Lightbend, and Mark Harrah
 *
 * Licensed under Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 * See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 */

package sbt.io

import java.io.File
import org.scalacheck._, Arbitrary.arbitrary, Prop._

object WriteContentSpecification extends Properties("Write content") {
  sys.props.put("jna.nosys", "true")

  property("Round trip string") = forAll(writeAndCheckString _)
  property("Round trip bytes") = forAll(writeAndCheckBytes _)
  property("Write string overwrites") = forAll(overwriteAndCheckStrings _)
  property("Write bytes overwrites") = forAll(overwriteAndCheckBytes _)
  property("Append string appends") = forAll(appendAndCheckStrings _)
  property("Append bytes appends") = forAll(appendAndCheckBytes _)
  property("Unzip doesn't stack overflow") = largeUnzip()
  property("Unzip errors given parent traversal") = testZipSlip()

  implicit lazy val validChar: Arbitrary[Char] = Arbitrary(
    for (i <- Gen.choose(0, 0xd7ff)) yield i.toChar
  )

  implicit lazy val validString: Arbitrary[String] = Arbitrary(
    arbitrary[List[Char]] map (_.mkString)
  )

  private def largeUnzip() = {
    testUnzip[Product]
    testUnzip[scala.tools.nsc.Global]
    true
  }

  private def testZipSlip() = {
    val badFile0 = new File("io/src/test/resources/zip-slip.zip")
    val badFile1 = new File("src/test/resources/zip-slip.zip")
    val badFile =
      if (badFile0.exists()) badFile0
      else badFile1
    try {
      unzipFile(badFile)
      false
    } catch {
      case e: RuntimeException => e.getMessage.contains("outside of the target directory")
    }
  }

  private def testUnzip[T](implicit mf: Manifest[T]) =
    unzipFile(IO.classLocationFileOption(mf.runtimeClass).getOrElse(sys.error(s"$mf")))

  private def unzipFile(jar: File) = IO.withTemporaryDirectory(tmp => IO.unzip(jar, tmp))

  // make the test independent of underlying platform and allow any unicode character in Strings to be encoded
  val charset = IO.utf8

  private def writeAndCheckString(s: String) =
    withTemporaryFile { file =>
      IO.write(file, s, charset)
      IO.read(file, charset) == s
    }

  private def writeAndCheckBytes(b: Array[Byte]) =
    withTemporaryFile { file =>
      IO.write(file, b)
      IO.readBytes(file) sameElements b
    }

  private def overwriteAndCheckStrings(a: String, b: String) =
    withTemporaryFile { file =>
      IO.write(file, a, charset)
      IO.write(file, b, charset)
      IO.read(file, charset) == b
    }

  private def overwriteAndCheckBytes(a: Array[Byte], b: Array[Byte]) =
    withTemporaryFile { file =>
      IO.write(file, a)
      IO.write(file, b)
      IO.readBytes(file) sameElements b
    }

  private def appendAndCheckStrings(a: String, b: String) =
    withTemporaryFile { file =>
      IO.append(file, a, charset)
      IO.append(file, b, charset)
      IO.read(file, charset) == (a + b)
    }

  private def appendAndCheckBytes(a: Array[Byte], b: Array[Byte]) =
    withTemporaryFile { file =>
      IO.append(file, a)
      IO.append(file, b)
      IO.readBytes(file) sameElements (a ++ b)
    }

  private def withTemporaryFile[T](f: File => T): T =
    IO.withTemporaryDirectory(dir => f(new File(dir, "out")))
}
