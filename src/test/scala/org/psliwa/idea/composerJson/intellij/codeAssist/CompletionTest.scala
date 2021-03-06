package org.psliwa.idea.composerJson.intellij.codeAssist

import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import org.junit.Assert._
import org.psliwa.idea.composerJson.ComposerJson

abstract class CompletionTest extends LightPlatformCodeInsightFixtureTestCase {

  protected def suggestions(
    contents: String,
    expectedSuggestions: Array[String],
    unexpectedSuggestions: Array[String] = Array()
  ): Unit = suggestions(assertContainsElements(_, _:_*))(contents, expectedSuggestions, unexpectedSuggestions)

  protected def orderedSuggestions(
    contents: String,
    expectedSuggestions: Array[String],
    unexpectedSuggestions: Array[String] = Array()
  ): Unit = suggestions(assertContainsOrdered(_, _:_*))(contents, expectedSuggestions, unexpectedSuggestions)

  protected def suggestions(
    containsElements: (java.util.List[String], Array[String]) => Unit
  )(
    contents: String, 
    expectedSuggestions: Array[String],
    unexpectedSuggestions: Array[String]
  ) = {
    myFixture.configureByText(ComposerJson, contents)
    myFixture.completeBasic()

    val lookupElements = myFixture.getLookupElementStrings

    assertNotNull(lookupElements)
    containsElements(lookupElements, expectedSuggestions)
    assertDoesntContain(lookupElements, unexpectedSuggestions:_*)
  }

  protected def completion(contents: String, expected: String) = {
    myFixture.configureByText(ComposerJson, contents)
    myFixture.completeBasic()

    myFixture.checkResult(expected.replace("\r", ""))
  }
}
