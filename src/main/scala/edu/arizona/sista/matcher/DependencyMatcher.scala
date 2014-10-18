package edu.arizona.sista.matcher

import scala.util.matching.Regex
import scala.util.parsing.combinator._
import edu.arizona.sista.processors.Sentence


case class TriggerMatcher(token: String) {
  def findAllIn(sentence: Sentence): Seq[Int] = {
    sentence.words.zipWithIndex filter (_._1 == token) map (_._2)
  }
}


trait DepMatcher {
  def findIn(sentence: Sentence, from: Int): Option[Int]

  // get dependencies for sentence if available
  protected def dependencies(sentence: Sentence) = sentence.dependencies match {
    case None => throw new Error("no dependencies in sentence")
    case Some(deps) => deps
  }
}


trait NameMatcher {
  def matches(edges: Seq[(Int, String)]): Seq[Int]
}


case class ExactNameMatcher(dep: String) extends NameMatcher {
  def matches(edges: Seq[(Int, String)]): Seq[Int] = {
    edges filter (_._2 == dep) map (_._1)
  }
}

case class RegexNameMatcher(rx: Regex) extends NameMatcher {
  def matches(edges: Seq[(Int, String)]): Seq[Int] = {
    edges filter (e => rx.findFirstIn(e._2).nonEmpty) map (_._1)
  }
}


object Direction extends Enumeration {
  type Direction = Value
  val Incoming, Outgoing = Value
}
import Direction._


case class DirectedDepMatcher(matcher: NameMatcher, direction: Direction) extends DepMatcher {
  def findIn(sentence: Sentence, from: Int): Option[Int] = {
    val deps = dependencies(sentence)
    val edges = if (direction == Incoming) deps.incomingEdges else deps.outgoingEdges
    val matches = matcher matches edges(from)
    if (matches.size == 1) Some(matches.head)
    else None
  }
}


case class PathMatcher(lhs: DepMatcher, rhs: DepMatcher) extends DepMatcher {
  def findIn(sentence: Sentence, from: Int): Option[Int] = {
    lhs.findIn(sentence, from) match {
      case None => None
      case Some(i) => rhs.findIn(sentence, i)
    }
  }
}


class DependencyMatcher(val pattern: String) {
  private var triggerFieldName = "trigger"
  private var _trigger: Option[TriggerMatcher] = None
  private var _arguments: Option[Map[String, DepMatcher]] = None

  def trigger = getFieldValue(_trigger)

  def arguments = getFieldValue(_arguments)

  parse(pattern)

  private def parse(pattern: String) {
    val fieldPat = """(\w+)\s*:\s*(.+)""".r
    val it = fieldPat findAllIn pattern map {
      case fieldPat(name, value) => (name -> value)
    }
    val fields = Map(it.toSeq: _*)
    _trigger = Some(TriggerMatcher(fields(triggerFieldName)))
    _arguments = Some(fields filterKeys (_ != triggerFieldName) mapValues Parser.parse)
  }

  private def getFieldValue[T](field: Option[T]) = field match {
    case None => throw new Error("object not initialized")
    case Some(value) => value
  }

  def findAllIn(sentence: Sentence): Seq[Map[String, Int]] = {
    trigger findAllIn sentence flatMap (i => applyRules(sentence, i))
  }

  def applyRules(sentence: Sentence, i: Int): Option[Map[String, Int]] = {
    val matches = arguments.keySet flatMap { name =>
      arguments(name).findIn(sentence, i) match {
        case None => None
        case Some(i) => Some(name -> i)
      }
    }
    if (matches.isEmpty) None
    else Some(matches.toMap)
  }

  private object Parser extends RegexParsers {
    def parse(input: String): DepMatcher = parseAll(matcher, input).get

    def token: Parser[String] = """\w+""".r

    def matcher: Parser[DepMatcher] = pathMatcher

    def exactMatcher: Parser[NameMatcher] = token ^^ {
      ExactNameMatcher(_)
    }

    def regexMatcher: Parser[NameMatcher] = regexMatch("""/(.*)/""".r) ^^ {
      case m => RegexNameMatcher(m.group(1).r)
    }

    def nameMatcher: Parser[NameMatcher] = exactMatcher | regexMatcher

    def outgoingMatcher: Parser[DepMatcher] = """>?""".r ~> nameMatcher ^^ {
      DirectedDepMatcher(_, Outgoing)
    }

    def incomingMatcher: Parser[DepMatcher] = "<" ~> nameMatcher ^^ {
      DirectedDepMatcher(_, Incoming)
    }

    def depMatcher: Parser[DepMatcher] = outgoingMatcher | incomingMatcher

    def pathMatcher: Parser[DepMatcher] = depMatcher ~ rep(depMatcher) ^^ {
      case m ~ rest => (m /: rest) {
        case (lhs, rhs) => PathMatcher(lhs, rhs)
      }
    }

    def regexMatch(r: Regex): Parser[Regex.Match] = new Parser[Regex.Match] {
      def apply(in: Input) = {
        val source = in.source
        val offset = in.offset
        val start = handleWhiteSpace(source, offset)
        (r findPrefixMatchOf (source.subSequence(start, source.length))) match {
          case Some(matched) =>
            Success(matched, in.drop(start + matched.end - offset))
          case None =>
            val found = if (start == source.length()) "end of source" else "`"+source.charAt(start)+"'"
            Failure("string matching regex `"+r+"' expected but "+found+" found", in.drop(start - offset))
        }
      }
    }
  }
}
