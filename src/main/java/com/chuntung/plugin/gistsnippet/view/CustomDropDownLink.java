package com.chuntung.plugin.gistsnippet.view;

import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBComboBoxLabel;
import com.intellij.util.SmartList;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

class CustomDropDownLink extends JBComboBoxLabel {
    private final JLabel leftIcon = new JLabel("", null, SwingConstants.CENTER);
    private final List<String> items = new SmartList<>();
    private final List<Icon> icons = new SmartList<>();
    private String selectedItem;
    private Consumer consumer;

    CustomDropDownLink(String selectedItem, String[] items, Icon[] icons, Consumer consumer) {
        this.selectedItem = selectedItem;
        this.consumer = consumer;
        this.items.addAll(Arrays.asList(items));
        if (icons != null) {
            this.icons.addAll(Arrays.asList(icons));
        }

        setForeground(JBUI.CurrentTheme.Link.linkColor());
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                showPopup();
            }
        });

        init();
    }

    private void init() {
        leftIcon.setBorder(new EmptyBorder(0, 4, 0, 4));
        add(leftIcon, BorderLayout.WEST);

        setText(selectedItem);

        if (icons.size() > 0) {
            leftIcon.setIcon(icons.get(items.indexOf(selectedItem)));
        }
    }

    public String getSelectedItem() {
        return selectedItem;
    }

    /**
     * Only render selected item.
     *
     * @param selectedItem
     */
    public void setSelectedItem(String selectedItem) {
        if (!items.contains(selectedItem)) {
            return;
        }

        setText(selectedItem);
        if (icons.size() > 0) {
            leftIcon.setIcon(icons.get(items.indexOf(selectedItem)));
        }

        this.selectedItem = selectedItem;
    }

    void showPopup() {
        if (!isEnabled()) return;
        final BaseListPopupStep<String> list = new BaseListPopupStep<String>(null, items, icons) {
            @Override
            public PopupStep onChosen(String selectedValue, boolean finalChoice) {
                if (consumer != null) {
                    consumer.accept(selectedValue);
                }
                setSelectedItem(selectedValue);
                return super.onChosen(selectedValue, finalChoice);
            }

            @Override
            public int getDefaultOptionIndex() {
                return items.indexOf(selectedItem);
            }
        };

        final ListPopup popup = JBPopupFactory.getInstance().createListPopup(list);
        Point showPoint = new Point(0, getHeight() + JBUI.scale(4));
        popup.show(new RelativePoint(this, showPoint));
    }
}