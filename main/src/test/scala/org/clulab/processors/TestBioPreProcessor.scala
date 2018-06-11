package org.clulab.processors

import org.clulab.utils.ScienceUtils
import org.scalatest.{FlatSpec, Matchers}

/**
  * Tests the preprocessor
  * User: mihais
  * Date: 8/14/15
  * Last Modified: Add tests for bibliographic reference removal.
 */
class TestBioPreProcessor extends FlatSpec with Matchers {

  val su = new ScienceUtils

  "ScienceUtils" should "convert Unicode to ASCII" in {
    val s = "\u2714alpha\u03B1\u25B6"
    val ps = su.replaceUnicodeWithAscii(s)
    ps should be ("valphaalpha>")
  }


  it should "remove simple BIB REF" in {
    val str = "These are known as Kremer bodies (Bernardi and Pandolfi, XREF_BIBR)."
    val rep = "These are known as Kremer bodies                                   ."
    val ppStr = su.removeBibRefs(str)
    ppStr should be (rep)
  }

  it should "remove BIB REF but leave other paren expression" in {
    val str = "These are referred to as PML nuclear bodies (PML-NBs) but are also known as PML oncogenic domains, nuclear dot 10 or Kremer bodies (Bernardi and Pandolfi, XREF_BIBR)."
    val rep = "These are referred to as PML nuclear bodies (PML-NBs) but are also known as PML oncogenic domains, nuclear dot 10 or Kremer bodies                                   ."
    val ppStr = su.removeBibRefs(str)
    ppStr should be (rep)
  }

  it should "remove complex BIB REF but leave other paren expressions" in {
    val str = "This tripartite structure contains a RING (really interesting new gene) zinc-finger, two additional zinc-finger motifs (B-box1 and B-box2) and a DUF 3583 domain containing a coiled-coil region (Borden et al., XREF_BIBR; Jensen et al., XREF_BIBR)."
    val rep = "This tripartite structure contains a RING (really interesting new gene) zinc-finger, two additional zinc-finger motifs (B-box1 and B-box2) and a DUF 3583 domain containing a coiled-coil region                                                     ."
    val ppStr = su.removeBibRefs(str)
    ppStr should be (rep)
  }

}
