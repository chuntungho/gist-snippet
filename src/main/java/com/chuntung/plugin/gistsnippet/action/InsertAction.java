package com.chuntung.plugin.gistsnippet.action;

import com.chuntung.plugin.gistsnippet.view.InsertGistDialog;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

/**
 * Open dialog to select gist to insert into editor.
 */
public class InsertAction extends AnAction implements DumbAware {
    public InsertAction() {
        super(IconLoader.getIcon("/images/gist.png"));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            return;
        }

        boolean writable = editor.getDocument().isWritable();
        InsertGistDialog dialog = new InsertGistDialog(project, writable);

        if (dialog.showAndGet()) {
            String selectedText = dialog.getSelectedText();
            if (selectedText == null) {
                return;
            }

            if (!writable) {
                return;
            }

            CommandProcessor.getInstance().executeCommand(project,
                    () -> ApplicationManager.getApplication().runWriteAction(() -> insertIntoEditor(editor, selectedText)),
                    "Insert Gist", null);
        }
    }

    private void insertIntoEditor(Editor editor, String selectedText) {
        Document document = editor.getDocument();
        SelectionModel selectionModel = editor.getSelectionModel();
        int offsetStart;
        if (selectionModel.hasSelection()) {
            offsetStart = selectionModel.getSelectionStart();
            int offsetEnd = selectionModel.getSelectionEnd();
            document.replaceString(offsetStart, offsetEnd, selectedText);
        } else {
            offsetStart = editor.getCaretModel().getOffset();
            document.insertString(offsetStart, selectedText);
        }
        int len = selectedText.length();
        selectionModel.setSelection(offsetStart, offsetStart + len);
        editor.getCaretModel().moveToOffset(offsetStart + len);
        editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            // only available for editor popup menu
            event.getPresentation().setVisible(false);
        }
    }
}
