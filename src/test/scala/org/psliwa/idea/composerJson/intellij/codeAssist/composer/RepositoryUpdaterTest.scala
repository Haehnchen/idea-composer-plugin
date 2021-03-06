package org.psliwa.idea.composerJson.intellij.codeAssist.composer

import com.intellij.openapi.application.ApplicationManager
import org.psliwa.idea.composerJson.composer.repository.{RepositoryInfo, InMemoryRepository, Repository, TestingRepositoryProvider}
import org.psliwa.idea.composerJson.intellij.codeAssist.InspectionTest
import org.junit.Assert._

class RepositoryUpdaterTest extends InspectionTest {

  def testGivenNotPackagistRepository_urlRepositoriesShouldBeEmpty() = {
    val url = "https://github.com/foobar/intermediate.git"
    checkInspection(
      s"""{
        |  "repositories": [
        |    {
        |      "type": "git",
        |      "url": "$url"
        |    }
        |  ]
        |}""".stripMargin
    )

    assertRepositories(List())
  }

  def testGivenComposerRepository_thereShouldBeOneUrlRepository() = {
    val url = "https://github.com/foobar/intermediate.git"
    checkInspection(
      s"""{
         |  "repositories": [
         |    {
         |      "type": "composer",
         |      "url": "$url"
         |    }
         |  ]
         |}""".stripMargin
    )

    assertRepositories(List(url+"/packages.json"))
  }

  def testGivenExcludedPackagistRepo_thereShouldNotBeIncludedPackagistRepo() = {
    checkInspection(
      s"""{
         |  "repositories": [
         |    {
         |      "packagist": false
         |    }
         |  ]
         |}""".stripMargin
    )

    assertRepositories(List(), includePackagist = false)
  }

  def testRepositoryInfoShouldBeClearedWhenRepositoriesPropertyWasRemoved() = {
    checkInspection(
      s"""{
         |  "repositories": [
         |    {
         |      "type": "composer",
         |      "url": "http://some-repo.org"
         |    }
         |  ]
         |}""".stripMargin
    )

    checkInspection(
      """
        |{}
      """.stripMargin
    )

    assertRepositories(List(), includePackagist = true)
  }

  def testGivenDirectlyIncludedPackagistRepo_thereShouldBeIncludedPackagistRepo() = {
    checkInspection(
      s"""{
         |  "repositories": [
         |    {
         |      "packagist": true
         |    }
         |  ]
         |}""".stripMargin
    )

    assertRepositories(List(), includePackagist = true)
  }

  def testGivenInlinePackage_thereShouldBeInlinePackageInRepository() = {
    checkInspection(
      s"""{
         |  "repositories": [
         |    {
         |      "type": "package",
         |      "package": {
         |        "name": "inline/package",
         |        "version": "1.0.8"
         |      }
         |    }
         |  ]
         |}""".stripMargin
    )

    assertRepositories(List(), includePackagist = true, Map("inline/package" -> List("1.0.8")))
  }

  def testGivenIncompleteInlinePackage_repositoryShouldBeEmpty() = {
    checkInspection(
      s"""{
         |  "repositories": [
         |    {
         |      "type": "package",
         |      "package": {
         |        "version": "inline/package"
         |      }
         |    }
         |  ]
         |}""".stripMargin
    )

    val repoInfos = getRepositoryInfo
    assertTrue(getRepositoryInfo.isDefined)

    val packages = getRepositoryInfo.get.repository.map(_.getPackages).getOrElse(List())
    assertEquals(List(), packages)
  }

  def testGivenTwoInlinePackageVersions_thereShouldBeBothVersionsInRepository() = {
    checkInspection(
      s"""{
         |  "repositories": [
         |    {
         |      "type": "package",
         |      "package": {
         |        "name": "inline/package",
         |        "version": "1.0.8"
         |      }
         |    },
         |    {
         |      "type": "package",
         |      "package": {
         |        "name": "inline/package",
         |        "version": "1.0.9"
         |      }
         |    }
         |  ]
         |}""".stripMargin
    )

    assertRepositories(List(), includePackagist = true, Map("inline/package" -> List("1.0.8", "1.0.9")))
  }

  private def assertRepositories(expectedUrls: List[String], includePackagist: Boolean = true, expectedPackages: Map[String,List[String]] = Map()): Unit = {
    val repoInfo = getRepositoryInfo

    assertEquals(1, getRepositoryProvider.infos.size)
    assertTrue(repoInfo.isDefined)
    assertEquals(expectedUrls, repoInfo.get.urls)
    assertEquals(includePackagist, repoInfo.get.packagist)
    val repository = repoInfo.get.repository.getOrElse(new InMemoryRepository[String](List()))
    assertRepository(repository, expectedPackages)
  }

  private def getRepositoryInfo: Option[RepositoryInfo] = {
    getRepositoryProvider.infos.get(myFixture.getFile.getVirtualFile.getCanonicalPath)
  }

  private def assertRepository[A](repository: Repository[A], expectedPackages: Map[String,List[String]]): Unit = {
    assertTrue(
      expectedPackages.forall{case(pkg, versions) => {
        repository.getPackages.contains(pkg) && versions.forall(repository.getPackageVersions(pkg).contains)
      }}
    )
  }

  override def setUp() = {
    super.setUp()
    clearRepositories()
  }

  private def clearRepositories() = {
    getRepositoryProvider.infos.clear()
  }

  private def getRepositoryProvider: TestingRepositoryProvider = {
    Option(ApplicationManager.getApplication.getComponent(classOf[PackagesLoader]))
      .map(_.repositoryProviderFor(myFixture.getProject))
      .map(_.asInstanceOf[TestingRepositoryProvider])
      .get
  }

  override def tearDown(): Unit = {
    clearRepositories()
    super.tearDown()
  }
}
