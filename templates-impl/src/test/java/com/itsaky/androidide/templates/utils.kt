/*
 *  This file is part of AndroidIDE.
 *
 *  AndroidIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.itsaky.androidide.templates

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.itsaky.androidide.managers.PreferenceManager
import com.itsaky.androidide.preferences.internal.prefManager
import com.itsaky.androidide.templates.base.AndroidModuleTemplateBuilder
import com.itsaky.androidide.templates.base.ModuleTemplateBuilder
import com.itsaky.androidide.templates.base.ProjectTemplateBuilder
import com.itsaky.androidide.tooling.testing.findAndroidHome
import com.itsaky.androidide.utils.Environment
import com.itsaky.androidide.utils.FileProvider
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkConstructor
import java.io.File
import kotlin.reflect.KClass

val testProjectsDir: File by lazy {
  FileProvider.currentDir().resolve("build/templateTest").toFile()
}

fun mockPrefManager(configure: PreferenceManager.() -> Unit = {}) {
  mockkStatic(::prefManager)

  val manager = PreferenceManager(ApplicationProvider.getApplicationContext())
  every { prefManager } answers { manager }
  manager.configure()
}

fun mockTemplateBuilders() {
  mockTemplateBuilderConstructor(TemplateBuilder::class)
  mockTemplateBuilderConstructor(ProjectTemplateBuilder::class)
  mockTemplateBuilderConstructor(ModuleTemplateBuilder::class)
  mockTemplateBuilderConstructor(AndroidModuleTemplateBuilder::class)
}

private inline fun <reified T : TemplateBuilder<*>> mockTemplateBuilderConstructor(
  klass: KClass<T>
) {
  mockConstructors(klass) {
    every { anyConstructed<T>().templateName } returns -123
    every { anyConstructed<T>().thumb } returns -123
  }
}

fun mockTemplateDatas(useKts: Boolean) {
  mockTemplateDataConstructor(BaseTemplateData::class, useKts)
  mockTemplateDataConstructor(ProjectTemplateData::class, useKts)
  mockTemplateDataConstructor(ModuleTemplateData::class, useKts)
}

fun unmockTemplateDatas() {
  unmockkConstructor(BaseTemplateData::class)
  unmockkConstructor(ProjectTemplateData::class)
  unmockkConstructor(ModuleTemplateData::class)
}

private inline fun <reified T : BaseTemplateData> mockTemplateDataConstructor(
  kClass: KClass<T>, useKts: Boolean
) {
  mockConstructors(klass = kClass) {
    every { anyConstructed<T>().useKts } returns useKts
  }
}

private fun <T : Any> mockConstructors(klass: KClass<T>,
                                       configure: () -> Unit = {}
) {
  mockkConstructor(klass)
  configure()
}

fun testTemplate(name: String, generate: Boolean = true, builder: () -> Template
): Template {
  mockPrefManager()
  mockTemplateBuilders()
  testProjectsDir.apply {
    if (exists()) {
      delete()
    }
    mkdirs()
  }

  Environment.PROJECTS_DIR = testProjectsDir

  val template = builder()

  if (generate) {
    generateTemplateProject(name, template)
  }

  return template
}

private fun generateTemplateProject(name: String, template: Template
) {
  for (language in Language.values()) {
    val packageName = "com.itsaky.androidide.template.${language.lang}"
    run {
      val projectName = "${name}Project${language.name}WithoutKts"

      File(testProjectsDir, projectName).apply {
        if (exists()) delete()
      }

      // Test with language without Kotlin Script
      mockTemplateDatas(useKts = false)
      template.setupRootProjectParams(name = projectName,
        packageName = packageName, language = language)
      template.executeRecipe()
      unmockTemplateDatas()
    }

    run {
      val projectName = "${name}Project${language.name}WithKts"

      File(testProjectsDir, projectName).apply {
        if (exists()) delete()
      }

      // Test with language + Kotlin Script
      mockTemplateDatas(useKts = true)
      template.setupRootProjectParams(name = projectName,
        packageName = "${packageName}.kts", language = language)
      template.executeRecipe()
      unmockTemplateDatas()
    }
  }
}

fun Template.setupRootProjectParams(name: String = "TestTemplate",
                                    packageName: String = "com.itsaky.androidide.template",
                                    language: Language = Language.Kotlin,
                                    minSdk: Sdk = Sdk.Lollipop
) {
  val iterator = parameters.iterator()

  // name
  var param = iterator.next()
  (param as StringParameter).value = name

  // package
  param = iterator.next()
  (param as StringParameter).value = packageName

  // save location
  param = iterator.next()
  (param as StringParameter).value = testProjectsDir.absolutePath

  // language
  param = iterator.next()
  (param as EnumParameter<Language>).value = language

  // Min SDK
  param = iterator.next()
  (param as EnumParameter<Sdk>).value = minSdk
}

fun Template.executeRecipe() {
  TestRecipeExecutor().apply(recipe)
}

fun Collection<Parameter<*>>.assertParameterTypes(
  checker: (Int) -> KClass<out Parameter<*>>
) = assertTypes(checker)

fun Collection<Widget<*>>.assertWidgetTypes(
  checker: (Int) -> KClass<out Widget<*>>
) = assertTypes(checker)


fun <T : Any> Collection<T>.assertTypes(checker: (Int) -> KClass<out T>) {
  forEachIndexed { index, element ->
    assertThat(element).isInstanceOf(checker(index).java)
  }
}

private fun wrapperProcess(gradlew: File, vararg args: String) =
  ProcessBuilder(cmd(gradlew, *args)).apply {
    inheritIO()
    directory(gradlew.parentFile)
    environment().apply {
      put("JAVA_HOME", System.getProperty("java.home"))
      put("ANDROID_HOME", findAndroidHome())
    }
  }.start()

private fun cmd(gradlew: File, vararg args: String) =
  (if (isWindows()) mutableListOf("cmd", "/c", gradlew.absolutePath)
  else mutableListOf("bash", gradlew.absolutePath)).apply { addAll(args) }

private fun isWindows() =
  System.getProperty("os.name")?.contains("windows", ignoreCase = true) == true