package org.clulab.processors.clulab.syntax

import org.clulab.processors.Sentence
import org.clulab.struct.DirectedGraph

/**
  * Generic interface for a syntactic dependency parser
  * User: mihais
  * Date: 7/11/17
  */
trait Parser {
  def parseSentence(sentence:Sentence):DirectedGraph[String]
}
