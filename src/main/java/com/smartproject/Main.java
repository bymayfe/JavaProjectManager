package com.smartproject;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;
import com.smartproject.gui.MainGUI;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;

public class Main {
    public static void main(String[] args) {
        // FlatLaf Karanlık Temayı Kur
        try {
            // Modern rounded corners
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.trackArc", 999);
            
            // Sleek thin scrollbars
            UIManager.put("ScrollBar.width", 10);
            UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
            
            // Premium Indigo accent colors
            UIManager.put("AccentColor", "#6366f1");
            UIManager.put("TabbedPane.selectedType", "underline");
            UIManager.put("TabbedPane.underlineColor", new Color(99, 102, 241));
            UIManager.put("TabbedPane.showTabSeparators", true);
            UIManager.put("TabbedPane.tabHeight", 36);
            UIManager.put("TabbedPane.font", new Font("Segoe UI", Font.BOLD, 13));

            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // GUI'yi Event Dispatch Thread üzerinde başlat
        SwingUtilities.invokeLater(() -> {
            MainGUI gui = new MainGUI();
            gui.setVisible(true);
        });
    }
}
