/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.sqldelight.intellij.lang

import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.squareup.sqldelight.intellij.psi.SqliteElement

class SqliteFindUsagesProvider : FindUsagesProvider {
  override fun getWordsScanner() = null
  override fun canFindUsagesFor(psiElement: PsiElement) = psiElement.parent is SqliteElement

  override fun getHelpId(psiElement: PsiElement) = null
  override fun getDescriptiveName(element: PsiElement) =
      when (element) {
        is PsiFile -> element.name
        else -> element.text
      }

  override fun getNodeText(element: PsiElement, useFullName: Boolean) = element.parent.text
  override fun getType(element: PsiElement) =
      when (element.parent) {
        is SqliteElement.TableNameElement -> "sqlite table"
        is SqliteElement.ColumnAliasElement -> "sqlite column alias"
        is SqliteElement.TableAliasElement -> "sqlite table alias"
        is SqliteElement.ViewNameElement -> "sqlite view"
        else -> ""
      }
}
