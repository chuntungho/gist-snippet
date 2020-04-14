/*
 * Copyright (c) 2020 Tony Ho. Some rights reserved.
 */

package com.chuntung.plugin.gistsnippet.view;

import com.chuntung.plugin.gistsnippet.action.CustomComboBoxAction;
import com.chuntung.plugin.gistsnippet.action.DeleteAction;
import com.chuntung.plugin.gistsnippet.action.OpenInBrowserAction;
import com.chuntung.plugin.gistsnippet.dto.ScopeEnum;
import com.chuntung.plugin.gistsnippet.dto.SnippetNodeDTO;
import com.chuntung.plugin.gistsnippet.dto.SnippetRootNode;
import com.chuntung.plugin.gistsnippet.dto.api.GistDTO;
import com.chuntung.plugin.gistsnippet.dto.api.GistFileDTO;
import com.chuntung.plugin.gistsnippet.service.GistException;
import com.chuntung.plugin.gistsnippet.service.GistSnippetService;
import com.chuntung.plugin.gistsnippet.service.GithubAccountHolder;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.notification.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.labels.DropDownLink;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.StructureTreeModel;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.ui.treeStructure.actions.CollapseAllAction;
import com.intellij.ui.treeStructure.actions.ExpandAllAction;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeModelAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.github.authentication.GithubAuthenticationManager;
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class InsertGistDialog extends DialogWrapper {
    public static final String SPLIT_LEFT_WIDTH = "InsertGistDialog.yoursSplitLeftWidth";
    private JPanel mainPanel;
    private JBTabbedPane tabbedPane;
    private LinkLabel<?> yoursTabTitle;
    private JSplitPane yoursSplitPane;
    private JTree snippetTree;
    private Editor editor;

    private JButton searchButton;
    private JTextField textField1;
    private JComboBox languageComboBox;
    private JComboBox sortByComboBox;

    private Project project;
    private final boolean insertable;
    private GistSnippetService service;

    private SnippetRootNode snippetRoot;
    private StructureTreeModel<CustomTreeStructure> snippetStructure;
    private CustomComboBoxAction scopeAction;
    private CustomComboBoxAction typeAction;
    Icon ownIcon = IconLoader.getIcon("/images/own.png");
    Icon starredIcon = IconLoader.getIcon("/images/starred.png");

    // remember last preview file
    private volatile String showingFileUrl;
    private volatile String editorFileUrl;

    // to be returned
    private String selectedText;

    // account holder in project level
    private GithubAccountHolder accountHolder;

    public InsertGistDialog(Project project, boolean insertable) {
        super(project);
        this.project = project;
        this.insertable = insertable;
        this.service = GistSnippetService.getInstance();
        accountHolder = GithubAccountHolder.getInstance(project);
        init(project);
    }

    // create custom components before ui-designer init
    private void createUIComponents() {
        // Replace JTree with SimpleTree here due to ui designer fails to preview SimpleTree.
        snippetTree = new SimpleTree();
        snippetTree.addTreeSelectionListener(e -> onSelect(e));
        snippetTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // double click to load forcibly
                if (e.getButton() == e.BUTTON1 && e.getClickCount() == 2) {
                    DefaultMutableTreeNode selected = (DefaultMutableTreeNode) ((JTree) e.getComponent()).getLastSelectedPathComponent();
                    if (selected != null && selected.isLeaf()) {
                        loadFileContent(project, selected, true);
                    }
                }
            }
        });

        // use tree structure for rendering
        snippetRoot = new SnippetRootNode();
        // fix API removal issue
        try {
            // after 192
            Constructor<StructureTreeModel> constructor = StructureTreeModel.class.getConstructor(AbstractTreeStructure.class, Disposable.class);
            snippetStructure = constructor.newInstance(new CustomTreeStructure(snippetRoot), myDisposable);
        } catch (NoSuchMethodException e) {
            try {
                // before 192
                Constructor<StructureTreeModel> constructor = StructureTreeModel.class.getConstructor(AbstractTreeStructure.class);
                snippetStructure = constructor.newInstance(new CustomTreeStructure(snippetRoot));
            } catch (ReflectiveOperationException ex) {
                // NOOP
            }
        } catch (ReflectiveOperationException e) {
            // NOOP
        }
        if (snippetStructure != null) {
            AsyncTreeModel treeModel = new AsyncTreeModel(snippetStructure, myDisposable);

            // make it focusable after rendering
            treeModel.addTreeModelListener(new TreeModelAdapter() {
                protected void process(@NotNull TreeModelEvent event, @NotNull EventType type) {
                    if (snippetRoot.children().size() > 0 && snippetTree.getSelectionCount() == 0) {
                        UIUtil.invokeLaterIfNeeded(() -> {
                            if (snippetRoot.children().size() > 0 && snippetTree.getSelectionCount() == 0) {
                                snippetTree.setSelectionRow(0);
                                snippetTree.requestFocusInWindow();
                            }
                        });
                    }
                }
            });

            snippetTree.setModel(treeModel);
        }

        // init empty editor
        EditorFactory editorFactory = EditorFactory.getInstance();
        editor = editorFactory.createViewer(editorFactory.createDocument(""), project);
    }

    private void filterByPublic(Boolean isPublic) {
        for (SnippetNodeDTO child : snippetRoot.children()) {
            if (isPublic == null) {
                child.setVisible(true);
            } else {
                child.setVisible(isPublic.equals(child.isPublic()));
            }
        }

        snippetStructure.invalidate();
    }

    // init after ui-designer setup
    private void init(Project project) {
        setTitle("Insert Gist");
        setOKButtonText("Insert");
        if (!insertable) {
            setOKButtonTooltip("Document is read only, snippet could not be inserted.");
        }
        // to be enabled after file loaded
        setOKActionEnabled(false);
        // focus cancel button by default
        getCancelAction().putValue(FOCUSED_ACTION, true);

        // after project field assigned
        ((SimpleTree) snippetTree).setPopupGroup(getPopupActions(), ActionPlaces.UNKNOWN);

        GithubAuthenticationManager authenticationManager = GithubAuthenticationManager.getInstance();
        Set<GithubAccount> accounts = authenticationManager.getAccounts();
        if (accounts != null && accounts.size() > 0) {
            initYoursPane(new ArrayList<>(accounts));
        } else {
            // hide yours tab content without github account
            yoursSplitPane.setVisible(false);
            yoursTabTitle = LinkLabel.create("Add GitHub Account", () -> {
                boolean existing = authenticationManager.ensureHasAccounts(project);
                if (existing) {
                    List<GithubAccount> accountList = new ArrayList<>(authenticationManager.getAccounts());
                    initYoursPane(accountList);
                }
            });
            tabbedPane.setTabComponentAt(0, yoursTabTitle);
        }

        // TODO discover feature is not done
        tabbedPane.remove(1);

        super.init();
    }


    // tree popup actions
    private ActionGroup getPopupActions() {
        DefaultActionGroup group = new DefaultActionGroup();

        // delete gist
        group.add(new DeleteAction(snippetTree, snippetStructure, snippetRoot, project));

        // open in browser
        group.add(new OpenInBrowserAction(snippetTree));

        return group;
    }

    // tree toolbar actions
    private ActionGroup getToolbarActions() {
        DefaultActionGroup group = new DefaultActionGroup();

        // scope combo-box
        group.add(scopeAction = CustomComboBoxAction.create(
                new AnAction("Own", "Load own gists", ownIcon) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        boolean forced = "Own".equals(scopeAction.getText());
                        loadOwnGist(forced);
                    }
                },

                new AnAction("Starred", "Load starred gists", starredIcon) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        boolean forced = "Starred".equals(scopeAction.getText());
                        loadStarredGist(forced);
                    }
                })
        );

        // type combo-box
        group.add(
                typeAction = CustomComboBoxAction.create(
                        DumbAwareAction.create("-Type-", e -> filterByPublic(null))
                        , DumbAwareAction.create("Public", e -> filterByPublic(true))
                        , DumbAwareAction.create("Secret", e -> filterByPublic(false))
                )
        );

        group.addSeparator();

        group.add(new ExpandAllAction(snippetTree));
        group.add(new CollapseAllAction(snippetTree));

        return group;
    }

    private void initYoursPane(List<GithubAccount> accountList) {
        UIUtil.removeScrollBorder(yoursSplitPane);

        yoursSplitPane.setVisible(true);

        // init toolbar
        ActionToolbar actionToolbar = ActionManager.getInstance()
                .createActionToolbar(ActionPlaces.UNKNOWN, getToolbarActions(), true);
        actionToolbar.setTargetComponent(mainPanel);
        ((JPanel) yoursSplitPane.getLeftComponent()).add(actionToolbar.getComponent(), BorderLayout.NORTH);

        // load remembered width
        int width = PropertiesComponent.getInstance().getInt(SPLIT_LEFT_WIDTH, 220);
        yoursSplitPane.getLeftComponent().setPreferredSize(new Dimension(width, -1));

        // bind editor
        yoursSplitPane.setRightComponent(editor.getComponent());

        // display account name on 1st tab
        if (accountHolder.getAccount() == null) {
            accountHolder.setAccount(accountList.get(0));
        }

        yoursTabTitle = new DropDownLink<>(accountHolder.getAccount(), accountList, chosenItem -> {
            if (!chosenItem.equals(accountHolder.getAccount())) {
                accountHolder.setAccount(chosenItem);

                // load All Own Gist by default
                scopeAction.reset();
                loadOwnGist(false);
            }
        }, true);

        tabbedPane.setTabComponentAt(0, yoursTabTitle);

        loadOwnGist(false);
    }

    private void onSelect(TreeSelectionEvent e) {
        DefaultMutableTreeNode selected = null;
        TreePath treePath = e.getNewLeadSelectionPath();
        if (treePath != null) {
            selected = (DefaultMutableTreeNode) treePath.getLastPathComponent();
        }
        if (selected != null && selected.isLeaf() && selected.getUserObject() instanceof GistFileDTO) {
            // show file content
            GistFileDTO gistFileDTO = getUserObject(selected);
            showingFileUrl = gistFileDTO.getRawUrl();

            // just show cache in view
            if (gistFileDTO.getContent() != null) {
                showInEditor(gistFileDTO, false);
            } else {
                // load forcibly
                loadFileContent(project, selected, true);
            }
        }
    }

    // run in dispatch thread
    private void showInEditor(GistFileDTO fileDTO, boolean forced) {
        if (!Objects.equals(editorFileUrl, fileDTO.getRawUrl()) || forced) {
            // setText require write access
            ApplicationManager.getApplication().runWriteAction(() -> {
                EditorHighlighterFactory highlighterFactory = EditorHighlighterFactory.getInstance();
                FileType fileType = GistFileDTO.getFileType(fileDTO);

                editor.getDocument().setText(fileDTO.getContent());
                ((EditorEx) editor).setHighlighter(highlighterFactory.createEditorHighlighter(project, fileType));
                editorFileUrl = fileDTO.getRawUrl();

                // make it focusable
                editor.getCaretModel().moveToLogicalPosition(new LogicalPosition(0, 0));
                editor.getContentComponent().requestFocusInWindow();
                snippetTree.requestFocusInWindow();

                setOKActionEnabled(insertable);
            });
        }
    }

    // load file content
    private void loadFileContent(Project project, DefaultMutableTreeNode selected, boolean forced) {
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selected.getParent();
        SnippetNodeDTO snippet = getUserObject(parent);
        new Task.Backgroundable(project, "Loading gist files...") {
            boolean showInDispatch = false;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                GistDTO gist = service.getGistDetail(accountHolder.getAccount(), snippet.getId(), forced);
                boolean shouldUpdate = snippet.update(gist);
                if (shouldUpdate) {
                    // invalidating is in a new background task, should invoker later in dispatch thread
                    snippetStructure.invalidate(new TreePath(parent), true).onSuccess((treePath) -> {
                        ApplicationManager.getApplication().invokeLater(() -> previewFile());
                    });
                } else {
                    showInDispatch = true;
                }
            }

            @Override
            public void onThrowable(Throwable e) {
                notifyWarn("Failed to get Gist files, " + e.getMessage());
            }

            @Override
            // this will run in dispatch thread
            public void onSuccess() {
                if (showInDispatch) {
                    previewFile();
                }
            }

            private void previewFile() {
                // selected may be changed by user, check before replacing editor content
                GistFileDTO gistFileDTO = getUserObject(selected);
                if (Objects.equals(showingFileUrl, gistFileDTO.getRawUrl())) {
                    if (gistFileDTO.getContent() != null) {
                        showInEditor(gistFileDTO, forced);
                    }
                }
            }
        }.queue();
    }

    private <T> T getUserObject(DefaultMutableTreeNode node) {
        return (T) (node.getUserObject());
    }

    private void loadOwnGist(boolean forced) {
        // reset type filter
        typeAction.reset();

        // com.intellij.util.io.HttpRequests#process does not allow Network accessed in dispatch thread or read action
        // start a background task to bypass api limitation
        new Task.Backgroundable(project, "Loading own gists...") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    List<GistDTO> ownGist = service.queryOwnGist(accountHolder.getAccount(), forced);
                    // non-modal task should not invoke onSuccess() in modal dialog initialization.
                    // it will be blocked in dispatch thread by modal dialog, here just run in background
                    renderTree(ownGist, ScopeEnum.OWN);
                } catch (GistException e) {
                    notifyWarn("Failed to load own gists, error: " + e.getMessage());
                }
            }
        }.queue();
    }

    private void loadStarredGist(boolean forced) {
        // reset type filter
        typeAction.reset();

        new Task.Backgroundable(project, "Loading starred gists...") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    List<GistDTO> starredGist = service.queryStarredGist(accountHolder.getAccount(), forced);
                    renderTree(starredGist, ScopeEnum.STARRED);
                } catch (GistException e) {
                    notifyWarn("Failed to load starred gists, error: " + e.getMessage());
                }
            }
        }.queue();
    }

    private void renderTree(List<GistDTO> gistList, ScopeEnum scope) {
        snippetRoot.resetChildren(gistList, scope);
        snippetStructure.invalidate();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return mainPanel;
    }

    @Override // remember window position and size
    protected String getDimensionServiceKey() {
        return "GistSnippet.InsertGistDialog";
    }

    @Override
    protected String getHelpId() {
        return "https://gist.chuntung.com";
    }

    @Override
    protected void doHelpAction() {
        if (myHelpAction.isEnabled()) {
            BrowserUtil.browse(getHelpId());
        }
    }

    @Override
    protected void doOKAction() {
        if (getOKAction().isEnabled()) {
            // set selected text for external action usage
            selectedText = editor.getSelectionModel().getSelectedText();
            if (selectedText == null) {
                selectedText = editor.getDocument().getText();
            }
        }

        super.doOKAction();
    }

    @Override
    protected void dispose() {
        if (editor != null) {
            EditorFactory.getInstance().releaseEditor(editor);
        }

        // remember left width
        PropertiesComponent.getInstance().setValue(SPLIT_LEFT_WIDTH, yoursSplitPane.getLeftComponent().getWidth(), 220);
        super.dispose();
    }

    public void notifyWarn(String warn) {
        NotificationGroup notificationGroup = new NotificationGroup("GistSnippet.NotificationGroup", NotificationDisplayType.BALLOON, true);
        Notification notification = notificationGroup.createNotification(warn, NotificationType.WARNING);
        Notifications.Bus.notify(notification, project);
    }

    /**
     * @return selected text or whole file content.
     */
    public String getSelectedText() {
        return selectedText;
    }
}