package org.psliwa.idea.composerJson.json

import org.junit.Assert._
import org.junit.Test

import scala.language.implicitConversions
import SchemaConversions._

class SchemaTest {

  @Test
  def parseEmptyObject() = {
    assertSchemaEquals(
      SObject(Map(), additionalProperties = true),
      Schema.parse(
        """
          {
            "type":"object",
            "properties":{}
          }
        """
      )
    )
  }

  @Test
  def parseEmptyObject_additionalPropertiesAreNotAllowed() = {
    assertSchemaEquals(
      SObject(Map(), additionalProperties = false),
      Schema.parse(
        """
          {
            "type":"object",
            "properties":{},
            "additionalProperties": false
          }
        """
      )
    )
  }

  @Test
  def parseSchemaWithOnlyScalarValues() = {
    assertSchemaEquals(
      SObject(
        Map(
          "stringProp",
          "numberProp" -> SNumber,
          "booleanProp" -> SBoolean
        )
      ),
      Schema.parse("""
        {
         "type": "object",
         "properties": {
           "stringProp": {"type":"string"},
           "numberProp": {"type":"integer"},
           "booleanProp": {"type":"boolean"}
         }
        }
      """)
    )
  }

  @Test
  def parseSchemaWithRequiredAndDescribedProperties() = {
    assertSchemaEquals(
      SObject(
        Map(
          "stringProp" -> Property(SString(), required=true, "some description")
        )
      ),
      Schema.parse("""
        {
         "type": "object",
         "properties": {
           "stringProp": {"type":"string", "required": true, "description": "some description" }
         }
        }
       """)
    )
  }

  @Test
  def parseSchemaWithScalarArrayValues() = {
    assertSchemaEquals(
      SObject(Map(
        "arrayNumberProp" -> SArray(SNumber),
        "arrayStringProp" -> SArray(SString()),
        "arrayBooleanProp" -> SArray(SBoolean)
      )),
      Schema.parse(
        """
          | {
          |   "type": "object",
          |   "properties": {
          |     "arrayNumberProp": {
          |       "type": "array",
          |       "items": { "type": "integer" }
          |     },
          |     "arrayStringProp": {
          |       "type": "array",
          |       "items": { "type": "string" }
          |     },
          |     "arrayBooleanProp": {
          |       "type": "array",
          |       "items": { "type": "boolean" }
          |     }
          |   }
          | }
        """.stripMargin
      )
    )
  }

  @Test
  def parseStringSchemaWithFormat() = {
    assertSchemaEquals(
      SObject(Map(
        "property" -> SString(UriFormat)
      )),
      Schema.parse(
        """
          |{
          |  "type": "object",
          |  "properties": {
          |    "property": { "type": "string", "format": "uri" }
          |  }
          |}
        """.stripMargin
      )
    )
  }

  @Test
  def parseSchemaWithNestedArrayValues() = {
    assertSchemaEquals(
      SObject(Map(
        "arrayNumberProp" -> SArray(SArray(SNumber)),
        "arrayBooleanProp" -> SArray(SArray(SBoolean))
      )),
      Schema.parse(
        """
          | {
          |   "type": "object",
          |   "properties": {
          |     "arrayNumberProp": {
          |       "type": "array",
          |       "items": {
          |         "type": "array",
          |         "items": {
          |           "type": "integer"
          |         }
          |       }
          |     },
          |     "arrayBooleanProp": {
          |       "type": "array",
          |       "items": {
          |         "type": "array",
          |         "items": {
          |           "type": "boolean"
          |         }
          |       }
          |     }
          |   }
          | }
        """.stripMargin
      )
    )
  }


  @Test
  def parseSchemaWithArrayOfObjects() = {
    assertSchemaEquals(
      SObject(Map(
        "arrayProp" -> SArray(SObject(Map(
          "stringProp"
        )))
      )),
      Schema.parse(
        """
          | {
          |   "type": "object",
          |   "properties": {
          |     "arrayProp": {
          |       "type": "array",
          |       "items": {
          |         "type": "object",
          |         "properties": {
          |           "stringProp": {
          |             "type": "string"
          |           }
          |         }
          |       }
          |     }
          |   }
          | }
        """.stripMargin
      )
    )
  }

  @Test
  def parseSchemaWithStringChoice() = {
    assertSchemaEquals(
      SObject(Map(
        "enumProp" -> SStringChoice(List("value1", "value2"))
      )),
      Schema.parse(
        """
          | {
          |   "type": "object",
          |   "properties": {
          |     "enumProp": {
          |       "enum": [ "value1", "value2" ]
          |     }
          |   }
          | }
        """.stripMargin
      )
    )
  }

  @Test
  def parseSchemaWithOr() = {
    assertSchemaEquals(
      SObject(Map(
        "orProp" -> SOr(List(SBoolean, SString()))
      )),
      Schema.parse(
        """
          | {
          |   "type": "object",
          |   "properties": {
          |     "orProp": {
          |       "oneOf": [
          |         { "type": "boolean" },
          |         { "type": "string" }
          |       ]
          |     }
          |   }
          | }
        """.stripMargin
      )
    )
  }

  @Test
  def parseSchemaWithInlineOr() = {
    assertSchemaEquals(
      SObject(Map(
        "orProp" -> SOr(List(SString(), SNumber))
      )),
      Schema.parse(
        """
          | {
          |   "type": "object",
          |   "properties": {
          |     "orProp": {
          |       "type": ["string", "integer" ]
          |     }
          |   }
          | }
        """.stripMargin
      )
    )
  }

  @Test
  def parseSchemaWithWildcardObject() = {
    assertSchemaEquals(
      SObject(Map()),
      Schema.parse(
        """
          | { "type": "object" }
        """.stripMargin
      )
    )
  }

  @Test
  def parseSchemaWithWildcardArray() = {
    assertSchemaEquals(
      SObject(Map(
        "arrProp" -> SArray(SAny)
      )),
      Schema.parse(
        """
          | {
          |   "type": "object",
          |   "properties": {
          |     "arrProp": { "type": "array" }
          |   }
          | }
        """.stripMargin
      )
    )
  }

  @Test
  def parsePackagesType() = {
    assertSchemaEquals(
      SObject(Map(
        "require" -> SPackages
      )),
      Schema.parse(
        """
          | {
          |   "type": "object",
          |   "properties": {
          |     "require": { "type": "packages" }
          |   }
          | }
        """.stripMargin
      )
    )
  }

  @Test
  def givenSchemaIsInvalid_expectNone() = {
    assertEquals(None, Schema.parse(
      """
        | { "type": "invalid" }
      """.stripMargin
    ))
  }

  @Test
  def parseSchemaWithPathProperty() = {
    assertSchemaEquals(
      SObject(Map(
        "name" -> SFilePath(existingFilePath = true)
      )),
      Schema.parse(
        """
          | {
          |   "type": "object",
          |   "properties": {
          |     "name": { "type": "filePath" }
          |   }
          | }
        """.stripMargin
      )
    )
  }

  @Test
  def parseSchemaWithPathProperty_filePathCouldNotExist() = {
    assertSchemaEquals(
      SObject(Map(
        "name" -> SFilePath(existingFilePath = false)
      )),
      Schema.parse(
        """
          | {
          |   "type": "object",
          |   "properties": {
          |     "name": { "type": "filePath", "existingFilePath": false }
          |   }
          | }
        """.stripMargin
      )
    )
  }

  @Test
  def parseSchemaWithRefs_refsShouldBeResolved() = {
    assertSchemaEquals(
      SObject(Map(
        "name" -> SObject(Map(
          "street" -> SString(),
          "number" -> SNumber
        ))
      )),
      Schema.parse(
        """
          | {
          |   "type": "object",
          |   "properties": {
          |     "name": { "$ref": "#/definitions/address" }
          |   },
          |   "definitions": {
          |     "address": {
          |       "type": "object",
          |       "properties": {
          |         "street": { "type": "string" },
          |         "number": { "type": "integer" }
          |       }
          |     }
          |   }
          | }
        """.stripMargin
      )
    )
  }

  @Test
  def parseSchemaWithRefs_givenInvalidRef_parsingShouldFail() = {
    assertEquals(None, Schema.parse(
      """
        | {
        |   "type": "object",
        |   "properties": {
        |     "name": { "$ref": "#/definitions/invalid" }
        |   },
        |   "definitions": {
        |     "address": {
        |       "type": "object",
        |       "properties": {
        |         "street": { "type": "string" },
        |         "number": { "type": "integer" }
        |       }
        |     }
        |   }
        | }
      """.stripMargin
    ))
  }

  @Test
  def parseObjectSchema_givenEnumRef_expectedValidObject() = {
    assertSchemaEquals(
      SObject(Map(
        "license" -> SStringChoice(List("a", "b"))
      )),
      Schema.parse(
        """
          | {
          |   "type": "object",
          |   "properties": {
          |     "license": { "$ref": "#/definitions/license" }
          |   },
          |   "definitions": {
          |     "license": { "enum": [ "a", "b" ] }
          |   }
          | }
        """.stripMargin
      )
    )
  }

  def assertSchemaEquals(expected: Schema, actual: Option[Schema]) = assertEquals(Some(expected), actual)
}
