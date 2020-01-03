package com.chuntung.plugin.gistsnippet.view;

import com.chuntung.plugin.gistsnippet.dto.ScopeEnum;
import com.chuntung.plugin.gistsnippet.dto.SnippetNodeDTO;
import com.chuntung.plugin.gistsnippet.dto.api.GistFileDTO;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.speedSearch.ElementFilter;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTreeStructure;
import com.intellij.ui.treeStructure.filtered.FilteringTreeStructure;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Deprecated // render in SimpleNode instead
class CustomTreeCellRenderer extends ColoredTreeCellRenderer {
    // cache avatar for starred/discover gist
    private static Map<String, Icon> avatarCache = new ConcurrentHashMap<>();
    private static Icon publicIcon = IconLoader.getIcon("/images/public.png");
    private static Icon secretIcon = IconLoader.getIcon("/images/secret.png");
    private FilteringTreeStructure dummyStructure = new FilteringTreeStructure(new ElementFilter.Active.Impl<SimpleNode>() {
        @Override
        public boolean shouldBeShowing(SimpleNode value) {
            return false;
        }
    }, new SimpleTreeStructure.Impl(null));


    private Icon findAvatar(String url) {
        return avatarCache.computeIfAbsent(url, (k) -> {
            try {
                return new ImageIcon(new URL(k));
            } catch (MalformedURLException e) {
                return null;
            }
        });
    }

    @Override
    public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded,
                                      boolean leaf, int row, boolean hasFocus) {
        if (value instanceof DefaultMutableTreeNode) {
            Object val = ((DefaultMutableTreeNode) value).getUserObject();

            if (val instanceof SnippetNodeDTO) {
                SnippetNodeDTO dto = (SnippetNodeDTO) val;

                if (!dto.isVisible()) {
                    // has no effect, will display place
                    this.setVisible(false);
                    return;
                }

                if (ScopeEnum.OWN.equals(dto.getScope())) {
                    setIcon(dto.isPublic() ? publicIcon : secretIcon);
                } else {
                    // get 16 * 16 icon from official site
                    setIcon(findAvatar(dto.getOwner().getAvatarUrl() + "&s=16"));
                }

                if (dto.getTags() != null) {
                    for (String tag : dto.getTags()) {
                        append(tag, SimpleTextAttributes.LINK_BOLD_ATTRIBUTES);
                        append(" ");
                    }
                }

                // Text format: tags TITLE Description n files
                if (dto.getTitle() != null) {
                    append(dto.getTitle(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                    append(" ");
                }

                append(dto.getDescription(), SimpleTextAttributes.REGULAR_ATTRIBUTES, true);
                String filesCount = " " + dto.getFilesCount() + " file" + (dto.getFilesCount() > 1 ? "s" : "");
                append(filesCount, SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES);

                setToolTipText(String.format("Last active at %s by %s", dto.getUpdatedAt(), dto.getOwner().getLogin()));
            } else if (val instanceof GistFileDTO) {
                GistFileDTO dto = (GistFileDTO) val;

                // file type icon
                FileType fileType = getFileType(dto);
                setIcon(fileType.getIcon());

                String size = " " + (dto.getSize() < 1024 ? (dto.getSize() + " B") : (dto.getSize() / 1024 + " KB"));
                append(dto.getFilename()).append(size, SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES);
            }
        }
    }

    @NotNull
    public static FileType getFileType(GistFileDTO dto) {
        FileTypeManager fileTypeManager = FileTypeManager.getInstance();
        FileType fileType = fileTypeManager.getFileTypeByFileName(dto.getFilename());
        if (UnknownFileType.INSTANCE == fileType) {
            fileType = dto.getLanguage() == null ?
                    PlainTextFileType.INSTANCE : fileTypeManager.getStdFileType(dto.getLanguage());
        }
        return fileType;
    }

}
