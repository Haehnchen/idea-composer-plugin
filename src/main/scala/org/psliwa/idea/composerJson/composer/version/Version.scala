package org.psliwa.idea.composerJson.composer.version

import org.psliwa.idea.composerJson.util.CharOffsetFinder._
import org.psliwa.idea.composerJson.util.OffsetFinder.ImplicitConversions._

object Version {

  type SemVer = String

  def alternativesForPrefix(prefix: String)(version: String): List[String] = {
    trySemantic(version)
      .map(semanticWildcards(prefix))
      .orElse(
        Some(List(version))
          .filter(_ => !semanticVersionRequired(prefix))
      )
      .getOrElse(List())
  }

  def isGreater(version1: String, version2: String): Boolean = {
    def isSemVer(v: String): Boolean = !v.exists(_.isLetter)
    val version1IsSemVer = isSemVer(version1)
    val version2IsSemVer = isSemVer(version2)

    if(version1IsSemVer && !version2IsSemVer) true
    else if(!version1IsSemVer && version2IsSemVer) false
    else if(version1IsSemVer && version2IsSemVer) {
      val dotsCount1 = version1.count(_ == '.')
      val dotsCount2 = version2.count(_ == '.')

      if(dotsCount1 == dotsCount2) {
        version1.replace("*", "999999").compareTo(version2.replace("*", "999999")) >= 0
      } else {
        dotsCount1 >= dotsCount2
      }
    }
    else version1.compareTo(version2) >= 0
  }

  def equivalentsFor(version: Constraint): Seq[Constraint] = {
    nsrEquivalent(version).toList
  }

  private def nsrEquivalent(version: Constraint): Option[Constraint] = {
    
    def incrementVersion(version: SemanticVersion): Option[SemanticVersion] = {
      version.dropLast
        .map(_.incrementLast)
        .flatMap(_.append(0))
    }
    
    Option(version.replace {
      //~ support
      case OperatorConstraint(ConstraintOperator.~, SemanticConstraint(versionFrom), _) => {
        incrementVersion(versionFrom.ensureParts(2))
          .map(versionTo => versionRange(versionFrom.ensureParts(2), versionTo.fillZero))
      }
      //example: >=1.2 <3.0.0 to ~1.2
      case VersionRange(versionFrom, versionTo) if versionFrom.dropZeros.partsNumber < 3 && incrementVersion(versionFrom.ensureExactlyParts(2)).exists(_.fillZero == versionTo.fillZero) => {
        Some(OperatorConstraint(ConstraintOperator.~, SemanticConstraint(versionFrom.dropZeros.ensureParts(2)), ""))
      }
      //example: >=1.2.1 <1.3.0 to ~1.2.1
      case VersionRange(versionFrom, versionTo) if incrementVersion(versionFrom.fillZero).exists(_.fillZero == versionTo.fillZero) => {
        Some(OperatorConstraint(ConstraintOperator.~, SemanticConstraint(versionFrom.ensureParts(3)), ""))
      }
      //^ support for pre-release: ^0.3.1 to >=0.3.1 <0.4.0
      case OperatorConstraint(ConstraintOperator.^, SemanticConstraint(versionFrom), _) if versionFrom.partsNumber > 1 && versionFrom.major == 0 => {
        incrementVersion(versionFrom.ensureParts(3))
          .map(versionTo => versionRange(versionFrom, versionTo.fillZero))
      }
      //^ support
      case OperatorConstraint(ConstraintOperator.^, SemanticConstraint(versionFrom), _) => {
        incrementVersion(versionFrom.ensureExactlyParts(2))
          .map(versionTo => versionRange(versionFrom, versionTo.fillZero))
      }
      //example: >=1.2.1 <2.0.0 to ^1.2.1
      case VersionRange(versionFrom, versionTo) if versionFrom.partsNumber == 3 && incrementVersion(versionFrom.ensureExactlyParts(2)).exists(_.fillZero == versionTo.fillZero) => {
        Some(OperatorConstraint(ConstraintOperator.^, SemanticConstraint(versionFrom), ""))
      }
      case _ => None
    }).filter(_ != version)
  }

  private def versionRange(versionFrom: SemanticVersion, versionTo: SemanticVersion): Constraint = {
    LogicalConstraint(
      List(
        OperatorConstraint(ConstraintOperator.>=, SemanticConstraint(versionFrom)),
        OperatorConstraint(ConstraintOperator.<, SemanticConstraint(versionTo))
      ),
      LogicalOperator.AND,
      " "
    )
  }

  private def semanticVersionRequired(text: String) = {
    findOffsetReverse('~' || '^' || '>' || '<' || '=' || ' ')(text.length-1)(text)
      .flatMap(ensure(not(' '))(_)(text))
      .isDefined
  }

  private def trySemantic(s: String): Option[SemVer] = "^v?(\\d)(\\.\\d)+$".r.findFirstIn(s)

  private def semanticWildcards(prefix: String)(version: SemVer): List[String] = {

    implicit val text = prefix.reverse

    val * = Wildcards.asterix _
    val nsr = Wildcards.nsr _

    findOffset('^' || '~' || '>' || '<' || '=' || ' ')(0)
      .flatMap(ensure(not(' '))(_))
      .map(_ => alternativesForSemantic(version, List(nsr), includeNotNormalized = false))
      .getOrElse(alternativesForSemantic(version, List(*)))
  }

  private def alternativesForSemantic(version: SemVer, providers: List[(String,List[String]) => List[String]], includeNotNormalized: Boolean = true): List[String] = {
    val normalized = if (version(0) == 'v') version.drop(1) else version

    val originalVersions = if(includeNotNormalized) uniqList(version, normalized) else List(normalized)

    originalVersions ++ normalized.split("\\.")
      .foldLeft(List[List[String]]())((aas, a) => {
        aas match {
          case Nil => List(a) :: aas
          case h::_ => (a :: h) :: aas
        }
      })
      .map(_.reverse)
      .flatMap(semVer => providers.flatMap(_(normalized, semVer)))
  }

  private def uniqList(a: String, b: String): List[String] = if(a == b) List(a) else List(a, b)

  private object Wildcards {

    def asterix(version: String, subVersion: List[String]): List[String] = {
      if(version.count(_ == '.') + 1 == subVersion.length) Nil
      else List(subVersion.mkString(".")+".*")
    }

    def nsr(version: String, subVersion: List[String]): List[String] = {
      if(subVersion.length == 1 || version.count(_ == '.') + 1 == subVersion.length) Nil
      else List(subVersion.mkString("."))
    }
  }

  private object VersionRange {
    def unapply(x: Constraint): Option[(SemanticVersion, SemanticVersion)] = x match {
      case LogicalConstraint(
        List(OperatorConstraint(ConstraintOperator.>=, SemanticConstraint(versionFrom), _), OperatorConstraint(ConstraintOperator.<, SemanticConstraint(versionTo), _)),
        LogicalOperator.AND,
        _
      ) => Some((versionFrom, versionTo))
      case _ => None
    }
  }
}
