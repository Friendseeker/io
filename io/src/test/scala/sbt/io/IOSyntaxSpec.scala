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

import java.io.{ File => JFile }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sbt.io.syntax._

class IOSyntaxSpec extends AnyFlatSpec with Matchers {
  "file(...)" should "create File" in {
    file(".") shouldBe (new JFile("."))
  }
  "file(...) / \"a\"" should "create File" in {
    (file("project") / "build.properties") shouldBe
      new JFile(new JFile("project"), "build.properties")
  }
  "file(...) glob \"*.properties\"" should "create PathFinder" in {
    IO.withTemporaryDirectory { dir =>
      IO.write(new JFile(dir, "foo.txt"), "foo")
      IO.write(new JFile(dir, "bar.json"), "{}")
      (dir glob "*.txt").get() shouldBe Seq(new JFile(dir, "foo.txt"))
    }
  }
  "get" should "work with PathLister and PathFinder" in IO.withTemporaryDirectory { dir =>
    assert((dir: PathLister).get() == (dir: PathFinder).get())
  }
}
