/*
Copyright 2020 The Regents of the University of California (Regents)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package firrtl_interpreter

import org.scalatest.{Matchers, FreeSpec}

class StopBehaviorSpec extends FreeSpec with Matchers {
  val input =
    """
      |circuit myRisc :
      |  module mockRegFileOut :
      |    input clk : Clock
      |    input reset : UInt<1>
      |    input io_addr : UInt<8>
      |    output io_DataO : UInt<32>
      |
      |    node T_3 = eq(io_addr, UInt<1>("h0")) @[RunTimeAssertSpec.scala 16:16]
      |    node GEN_0 = validif(T_3, UInt<8>("h80")) @[RunTimeAssertSpec.scala 16:28]
      |    node T_6 = eq(T_3, UInt<1>("h0")) @[RunTimeAssertSpec.scala 16:28]
      |    node T_8 = eq(reset, UInt<1>("h0")) @[RunTimeAssertSpec.scala 19:11]
      |    node T_10 = eq(reset, UInt<1>("h0")) @[RunTimeAssertSpec.scala 20:22]
      |    io_DataO <= GEN_0
      |    printf(clk, and(and(and(UInt<1>("h1"), T_6), T_8), UInt<1>("h1")), "STOP:Read at the wrong Register\n") @[RunTimeAssertSpec.scala 19:11]
      |    stop(clk, and(and(and(UInt<1>("h1"), T_6), T_10), UInt<1>("h1")), 47) @[RunTimeAssertSpec.scala 20:22]
      |
      |  module myRisc :
      |    input clk : Clock
      |    input reset : UInt<1>
      |    input io_wrData : UInt<32>
      |    output io_valid : UInt<1>
      |    output io_out : UInt<32>
      |
      |    inst mockRegFileOut_1 of mockRegFileOut @[RunTimeAssertSpec.scala 36:20]
      |    node rci = bits(io_wrData, 23, 16) @[RunTimeAssertSpec.scala 42:18]
      |    node T_4 = eq(rci, UInt<8>("hff")) @[RunTimeAssertSpec.scala 44:13]
      |    node GEN_0 = validif(T_4, UInt<1>("h1")) @[RunTimeAssertSpec.scala 44:27]
      |    node GEN_1 = validif(T_4, mockRegFileOut_1.io_DataO) @[RunTimeAssertSpec.scala 44:27]
      |    node T_7 = eq(T_4, UInt<1>("h0")) @[RunTimeAssertSpec.scala 44:27]
      |    node GEN_2 = mux(T_7, UInt<1>("h0"), GEN_0) @[RunTimeAssertSpec.scala 47:14]
      |    io_valid <= GEN_2
      |    io_out <= GEN_1
      |    mockRegFileOut_1.io_addr <= UInt<5>("h10")
      |    mockRegFileOut_1.clk <= clk
      |    mockRegFileOut_1.reset <= reset
      |
    """.stripMargin

  "Stop should abort interpreter immediately" in {
    val tester = new InterpretiveTester(input)

    tester.poke("reset", 0)
    tester.poke("io_wrData", (0 << 24) + (255 << 16))
    println(s"reset value is ${tester.interpreter.circuitState.getValue("reset")}")

    intercept[StopException] {
      tester.step(1)
    }

    tester.report()
    tester.reportString should include ("Failed: Stop result 47")
  }
}
