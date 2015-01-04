package upickle
import utest._
import Fns._, Implicits._, TupleImplicits._

object JsonTests extends TestSuite{
  val tests = TestSuite{
    val ugly =
      """
        |[
        |    "JSON Test Pattern pass1",
        |    {"object with 1 member":["array with 1 element"]},
        |    {},
        |    [],
        |    -42,
        |    true,
        |    false,
        |    {
        |        "integer": 1234567890,
        |        "real": -9876.543210,
        |        "e": 0.123456789e-12,
        |        "E": 1.234567890E+34,
        |        "":  23456789012E66,
        |        "zero": 0,
        |        "one": 1,
        |        "space": " ",
        |        "quote": "\"",
        |        "backslash": "\\",
        |        "controls": "\b\f\n\r\t",
        |        "slash": "/ & \/",
        |        "alpha": "abcdefghijklmnopqrstuvwyz",
        |        "ALPHA": "ABCDEFGHIJKLMNOPQRSTUVWYZ",
        |        "digit": "0123456789",
        |        "0123456789": "digit",
        |        "special": "`1~!@#$%^&*()_+-={':[,]}|;.</>?",
        |        "hex": "\u0123\u4567\u89AB\uCDEF\uabcd\uef4A",
        |        "true": true,
        |        "false": false,
        |        "array":[  ],
        |        "object":{  },
        |        "address": "50 St. James Street",
        |        "url": "http://www.JSON.org/",
        |        "comment": "// /* <!-- --",
        |        "# -- --> */": " ",
        |        " s p a c e d " :[1,2 , 3
        |
        |,
        |
        |4 , 5        ,          6           ,7        ],"compact":[1,2,3,4,5,6,7],
        |        "jsontext": "{\"object with 1 member\":[\"array with 1 element\"]}",
        |        "quotes": "&#34; \u005Cu0022 %22 0x22 034 &#x22;",
        |        "\/\\\"\uCAFE\uBABE\uAB98\uFCDE\ubcda\uef4A\b\f\n\r\t`1~!@#$%^&*()_+-=[]{}|;:',./<>?"
        |: "A key can be any string"
        |    },
        |    0.5 ,98.6
        |,
        |99.44
        |,
        |
        |1066,
        |1e1,
        |0.1e1,
        |1e-1,
        |1e00,2e+00,2e-00
        |,"rosebud"]
      """.stripMargin
    val parsed = json.read(ugly)

    "correctness" - {
      val unparsed = json.write(parsed)
      val reparsed = json.read(unparsed)
      for (json <- Seq(parsed, reparsed)){
        assert(
          json(0).value == "JSON Test Pattern pass1",
          json(7)("real").value == -9876.54321,
          json(7)("comment").value == "// /* <!-- --",
          json(7)("jsontext").value == "{\"object with 1 member\":[\"array with 1 element\"]}",
          json(18).value == "rosebud"
        )
      }
      (parsed(18), reparsed(18))
    }
/*
    "performance" - {
      "read" - {
        var n = 0
        val start = System.currentTimeMillis()
        var parsed: Js.Value = Js.Null
        while(System.currentTimeMillis() < start + 5000){
          parsed = Js.Null
          parsed = json.read(ugly)
          n += 1
        }
        n
      }
      "write" - {
        var n = 0
        val start = System.currentTimeMillis()
        var output = ""
        while(System.currentTimeMillis() < start + 5000){
          output = json.write(parsed)
          output = ""
          n += 1
        }
        n
      }
    }
*/
  }
}
