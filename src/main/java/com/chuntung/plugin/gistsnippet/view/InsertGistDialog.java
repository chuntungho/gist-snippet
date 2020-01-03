package com.chuntung.plugin.gistsnippet.view;

import com.chuntung.plugin.gistsnippet.dto.ScopeEnum;
import com.chuntung.plugin.gistsnippet.dto.SnippetNodeDTO;
import com.chuntung.plugin.gistsnippet.dto.SnippetRootNode;
import com.chuntung.plugin.gistsnippet.dto.api.GistDTO;
import com.chuntung.plugin.gistsnippet.dto.api.GistFileDTO;
import com.chuntung.plugin.gistsnippet.service.GistSnippetService;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.JBComboBoxLabel;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.labels.DropDownLink;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.ui.treeStructure.SimpleTreeBuilder;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.github.authentication.GithubAuthenticationManager;
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;

public class InsertGistDialog extends DialogWrapper {
    private JPanel mainPanel;
    private JBTabbedPane tabbedPane;
    private LinkLabel<?> yoursTabTitle;
    private JSplitPane yoursSplitPane;
    private JTree snippetTree;
    private Editor editor;
    private JBComboBoxLabel scopeDropDownLink;
    private DropDownLink typeDropDownLink;

    private JButton searchButton;
    private JTextField textField1;
    private JComboBox languageComboBox;
    private JComboBox sortByComboBox;

    private Project project;
    private boolean insertable;
    private GistSnippetService service;

    private SimpleTreeBuilder treeBuilder;
    private SnippetRootNode snippetRoot;

    // remember used account
    private GithubAccount currentAccount;
    // remember last preview file
    private String currentFileUrl;

    // to be returned
    private String selectedText;

    public InsertGistDialog(Project project, boolean insertable) {
        super(project);
        this.project = project;
        this.insertable = insertable;
        this.service = GistSnippetService.getInstance();
        init(project);
    }

    // create custom components before ui-designer init
    private void createUIComponents() {
        // Replace JTree with SimpleTree here due to ui designer fails to preview SimpleTree.
        snippetTree = new SimpleTree();
        snippetTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
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
        treeBuilder = new SimpleTreeBuilder(snippetTree, (DefaultTreeModel) snippetTree.getModel(),
                new CustomTreeStructure(snippetRoot), null);

        // UI designer cannot find custom class when preview, here create manually.
        Icon ownIcon = IconLoader.getIcon("/images/own.png");
        Icon starredIcon = IconLoader.getIcon("/images/starred.png");
        scopeDropDownLink = new CustomDropDownLink("Own",
                new String[]{"Own", "Starred"}, new Icon[]{ownIcon, starredIcon},
                item -> {
                    // select again will load forcibly
                    String previousItem = ((CustomDropDownLink) scopeDropDownLink).getSelectedItem();
                    if ("Own".equals(item)) {
                        loadOwnGist(item.equals(previousItem));
                    } else if ("Starred".equals(item)) {
                        loadStarredGist(item.equals(previousItem));
                    }
                }
        );

        // init type drop down link
        typeDropDownLink = new DropDownLink("All", Arrays.asList("All", "Public", "Secret"), new Consumer() {
            @Override
            public void consume(Object item) {
                Boolean isPublic = "All".equals(item) ? null : "Public".equals(item);
                // filter by public
                for (SnippetNodeDTO child : snippetRoot.children()) {
                    if (isPublic == null) {
                        child.setVisible(true);
                    } else {
                        child.setVisible(isPublic.equals(child.isPublic()));
                    }
                }

                // rebuild tree
                treeBuilder.queueUpdate();
            }
        }, true);

        // init empty editor
        EditorFactory editorFactory = EditorFactory.getInstance();
        editor = editorFactory.createViewer(editorFactory.createDocument(""), project);
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
        // focus cancel button
        getCancelAction().putValue(FOCUSED_ACTION, true);

        GithubAuthenticationManager authenticationManager = GithubAuthenticationManager.getInstance();
        Set<GithubAccount> accounts = authenticationManager.getAccounts();
        if (accounts != null && accounts.size() > 0) {
            initYoursPane(new ArrayList<>(accounts), authenticationManager.getSingleOrDefaultAccount(project));
        } else {
            // hide yours tab content without github account
            yoursSplitPane.setVisible(false);
            yoursTabTitle = LinkLabel.create("Add GitHub Account", () -> {
                boolean existing = authenticationManager.ensureHasAccounts(project);
                if (existing) {
                    List<GithubAccount> accountList = new ArrayList<>(authenticationManager.getAccounts());
                    initYoursPane(accountList, authenticationManager.getSingleOrDefaultAccount(project));
                }
            });
            tabbedPane.setTabComponentAt(0, yoursTabTitle);
        }

        // TODO discover feature is not done
        tabbedPane.remove(1);

        super.init();
    }

    private void initYoursPane(List<GithubAccount> accountList, GithubAccount singleOrDefaultAccount) {
        // choose first account by default

        currentAccount = accountList.get(0);

        // visible if has account
        yoursSplitPane.setVisible(true);

        // bind editor
        yoursSplitPane.setRightComponent(editor.getComponent());

        // display account name on 1st tab
        yoursTabTitle = new DropDownLink<>(currentAccount, accountList, chosenItem -> {
            if (!chosenItem.equals(currentAccount)) {
                currentAccount = chosenItem;
                // load All Own Gist by default
                ((CustomDropDownLink) scopeDropDownLink).setSelectedItem("Own");
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
        if (selected != null && selected.isLeaf()) {
            // show file content
            loadFileContent(project, selected, false);
        }
    }

    private void showInEditor(GistFileDTO fileDTO, boolean forced) {
        if (!Objects.equals(currentFileUrl, fileDTO.getRawUrl()) || forced) {
            // setText require write access
            ApplicationManager.getApplication().runWriteAction(() -> {
                EditorHighlighterFactory highlighterFactory = EditorHighlighterFactory.getInstance();
                FileType fileType = GistFileDTO.getFileType(fileDTO);

                editor.getDocument().setText(fileDTO.getContent());
                ((EditorEx) editor).setHighlighter(highlighterFactory.createEditorHighlighter(project, fileType));
                currentFileUrl = fileDTO.getRawUrl();
                setOKActionEnabled(insertable);
            });
        }
    }

    private void loadFileContent(Project project, DefaultMutableTreeNode selected, boolean forced) {
        GistFileDTO gistFileDTO = getUserObject(selected);
        if (gistFileDTO.getContent() != null && !forced) {
            showInEditor(gistFileDTO, false);
            return;
        }

        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selected.getParent();
        SnippetNodeDTO snippet = getUserObject(parent);
        new Task.Backgroundable(project, "Loading gist files...") {
            boolean needRebuild = false;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                GistDTO gist = service.getGistDetail(currentAccount, snippet.getId(), forced);
                if (gist != null) {
                    // merge files into tree structure
                    Set<String> children = new HashSet<>();
                    // traverse tree structure to remove non-existing items
                    Iterator<GistFileDTO> iterator = snippet.getFiles().iterator();
                    while (iterator.hasNext()) {
                        GistFileDTO fileDTO = iterator.next();
                        if (gist.getFiles().containsKey(fileDTO.getFilename())) {
                            fileDTO.setContent(gist.getFiles().get(fileDTO.getFilename()).getContent());
                            children.add(fileDTO.getFilename());
                        } else {
                            needRebuild = true;
                            iterator.remove();
                        }
                    }
                    // traverse latest files to add missing items if gist changed
                    for (GistFileDTO fileDTO : gist.getFiles().values()) {
                        if (!children.contains(fileDTO.getFilename())) {
                            needRebuild = true;
                            snippet.getFiles().add(fileDTO);
                        }
                    }
                }
            }

            @Override
            // this will run in dispatch thread
            public void onSuccess() {
                // rebuild if needed
                if (needRebuild) {
                    treeBuilder.queueUpdateFrom(snippet, false);
                }

                // selected may be changed by user, check before replacing editor content
                Object currentSelected = snippetTree.getLastSelectedPathComponent();
                if (currentSelected == selected) {
                    // load the replaced item
                    GistFileDTO gistFileDTO = getUserObject(selected);
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
        if (currentAccount == null) {
            return;
        }

        // com.intellij.util.io.HttpRequests.process does not allow Network accessed in dispatch thread or read action
        // start a background task to access network due to api limitation
        new Task.Backgroundable(project, "Loading own gists...") {

            private List<GistDTO> ownGist;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                ownGist = service.queryOwnGist(currentAccount, forced);
            }

            @Override
            public void onSuccess() {
                if (ownGist != null) {
                    renderTree(ownGist, ScopeEnum.OWN);
                }
            }
        }.queue();
    }

    private void loadStarredGist(boolean forced) {
        if (currentAccount == null) {
            return;
        }

        new Task.Backgroundable(project, "Loading starred gists...") {
            private List<GistDTO> starredGist;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                starredGist = service.queryStarredGist(currentAccount, forced);
            }

            @Override
            public void onSuccess() {
                if (starredGist != null) {
                    renderTree(starredGist, ScopeEnum.STARRED);
                }
            }
        }.queue();
    }

    private void renderTree(List<GistDTO> gistList, ScopeEnum scope) {
        snippetRoot.setSetChildren(gistList, scope);
        treeBuilder.queueUpdate();

        // reset type filter
        typeDropDownLink.setText("All");
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
        if (treeBuilder != null) {
            Disposer.dispose(treeBuilder);
        }
        super.dispose();
    }

    /**
     * @return selected text or whole file content.
     */
    public String getSelectedText() {
        return selectedText;
    }
}
