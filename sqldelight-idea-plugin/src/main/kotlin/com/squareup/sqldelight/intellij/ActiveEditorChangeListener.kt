package com.squareup.sqldelight.intellij

import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.psi.PsiDocumentManager
import com.squareup.sqldelight.core.lang.SqlDelightFile

internal class ActiveEditorChangeListener : FileEditorManagerListener {

  override fun selectionChanged(event: FileEditorManagerEvent) {
    val editor = (event.oldEditor as TextEditor?)?.editor ?: return
    val project = editor.project ?: return
    val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) as? SqlDelightFile? ?: return

    FileGeneratorService.getInstance(project).generateFiles(file)
  }
}
