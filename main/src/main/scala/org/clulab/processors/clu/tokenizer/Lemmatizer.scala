package org.clulab.processors.clu.tokenizer

import java.io.StringReader

import scala.collection.mutable
import scala.util.matching.Regex
import uk.ac.susx.informatics.Morpha
import EnglishLemmatizer._
import org.lemport.lemmatizer.rank.WordRankingLoadException
import org.lemport.lemmatizer.dictionary.DictionaryLoadException
import org.lemport.lemmatizer.lemma.{LemmatizeException, Lemmatizer => LemmatizerPT}

trait Lemmatizer {
  def lemmatizeWord(word:String, pos: String = null):String
}

class PortugueseLemmatizer extends Lemmatizer {
  override def lemmatizeWord(word: String, pos: String = null): String = {
    // TODO: add a proper lemmatizer here

    val lemmatizer = LemmatizerPT.getInstance();

    val lemma = lemmatizer.lemmatize(word, pos)
    lemma
    //word.toLowerCase
  }
}

class SpanishLemmatizer extends Lemmatizer {
  override def lemmatizeWord(word: String, pos: String = null): String = {
    // TODO: add a proper lemmatizer here
    word.toLowerCase
  }
}

class EnglishLemmatizer extends Lemmatizer {
  override def lemmatizeWord(word: String, pos: String = null): String = {
    if(parens.findFirstMatchIn(word).nonEmpty)
      return word.toLowerCase()

    val norm = normalizeForLemmatization(word).trim
    if(norm.isEmpty) return word.toLowerCase()

    val parts = norm.split(whitespaces)

    val result = new mutable.StringBuilder()
    for(part <- parts) {
      val morpha = new Morpha(new StringReader(part), false)

      var lemma = part
      try {
        lemma = morpha.next()
      } catch {
        case _:Throwable =>
      }

      if(result.nonEmpty) result.append(" ")
      result.append(lemma)
    }

    val output = result.toString()

    // in some cases, Morpha returns empty strings
    if(output.isEmpty) word.toLowerCase()
    else output
  }
}

object EnglishLemmatizer {
  /** Special characters to remove. */
  val remove: Regex = """[()\[\].,;:"']""".r

  val parens: Regex = """^(\-LRB\-)|(\-RRB\-)|(\-LSB\-)|(-RSB-)$""".r

  /** White spaces */
  val whitespaces: String = "\\s+"

  /** Remove special characters and lowercase the string. */
  def normalizeForLemmatization(word: String):String = EnglishLemmatizer.remove.replaceAllIn(word.trim.toLowerCase, "")
}