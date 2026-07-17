package com.smartproject.gui;

import com.smartproject.db.ProjectRepository;
import com.smartproject.model.Project;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Analiz edilmis bir projenin README.md ve etiketlerini duzenlemek icin dialog.
 */
public class ProjectEditDialog extends JDialog {

    private Project           project;
    private ProjectRepository repository;

    private JTextArea         txtReadme;
    private DefaultListModel<String> tagsModel;
    private JList<String>     tagsList;
    private JTextField        txtNewTag;
    private JLabel            lblSaveStatus;

    public ProjectEditDialog(JFrame parent, Project project, ProjectRepository repository) {
        super(parent, "Proje Duzenle: " + project.getDisplayName(), true);
        this.project    = project;
        this.repository = repository;

        setSize(720, 580);
        setLocationRelativeTo(parent);
        buildUI();
        loadData();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(new EmptyBorder(12, 12, 12, 12));

        // --- README editoru ---
        txtReadme = new JTextArea();
        txtReadme.setFont(new Font("Consolas", Font.PLAIN, 13));
        txtReadme.setLineWrap(true);
        txtReadme.setWrapStyleWord(true);

        JScrollPane readmeScroll = new JScrollPane(txtReadme);
        readmeScroll.setBorder(BorderFactory.createTitledBorder("README.md (Duzenlenebilir)"));
        readmeScroll.setPreferredSize(new Dimension(0, 350));

        // --- Etiket paneli ---
        JPanel tagPanel = new JPanel(new BorderLayout(6, 6));
        tagPanel.setBorder(BorderFactory.createTitledBorder("Etiketler (Tags)"));

        tagsModel = new DefaultListModel<>();
        tagsList  = new JList<>(tagsModel);
        tagsList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tagsList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        tagsList.setVisibleRowCount(-1);
        tagPanel.add(new JScrollPane(tagsList), BorderLayout.CENTER);

        JPanel tagInput = new JPanel(new BorderLayout(5, 0));
        tagInput.setBorder(new EmptyBorder(4, 0, 0, 0));
        txtNewTag = new JTextField();
        txtNewTag.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtNewTag.setToolTipText("Yeni etiket gir ve Enter'a bas");
        txtNewTag.addActionListener(e -> addTag());

        JButton btnAdd = new JButton("Ekle");
        JButton btnDel = new JButton("Sil");
        btnAdd.addActionListener(e -> addTag());
        btnDel.addActionListener(e -> deleteSelectedTag());

        JPanel tagBtns = new JPanel(new GridLayout(1, 2, 4, 0));
        tagBtns.add(btnAdd);
        tagBtns.add(btnDel);
        tagInput.add(txtNewTag, BorderLayout.CENTER);
        tagInput.add(tagBtns,   BorderLayout.EAST);
        tagPanel.add(tagInput, BorderLayout.SOUTH);
        tagPanel.setPreferredSize(new Dimension(0, 150));

        // --- Icerik ---
        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.add(readmeScroll, BorderLayout.CENTER);
        centerPanel.add(tagPanel,     BorderLayout.SOUTH);
        main.add(centerPanel, BorderLayout.CENTER);

        // --- Alt butonlar ---
        JPanel bottom = new JPanel(new BorderLayout());
        lblSaveStatus = new JLabel(" ");
        lblSaveStatus.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        bottom.add(lblSaveStatus, BorderLayout.WEST);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnCancel = new JButton("Iptal");
        btnCancel.addActionListener(e -> dispose());

        JButton btnSave = new JButton("Kaydet");
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSave.addActionListener(e -> saveAll());

        btns.add(btnCancel);
        btns.add(btnSave);
        bottom.add(btns, BorderLayout.EAST);
        main.add(bottom, BorderLayout.SOUTH);

        add(main);
    }

    private void loadData() {
        // README'yi .spm/README.md dosyasindan yukle
        try {
            File readmeFile = new File(project.getSpmFolder(), "README.md");
            if (readmeFile.exists()) {
                txtReadme.setText(new String(Files.readAllBytes(readmeFile.toPath()), "utf-8"));
            } else {
                txtReadme.setText("# " + project.getDisplayName() + "\n\nHenuz analiz edilmedi.");
            }
            txtReadme.setCaretPosition(0);
        } catch (Exception e) {
            txtReadme.setText("README yuklenirken hata: " + e.getMessage());
        }

        // Etiketleri yukle
        tagsModel.clear();
        for (String t : project.getTags()) tagsModel.addElement(t);
    }

    private void addTag() {
        String tag = txtNewTag.getText().trim().toLowerCase().replaceAll("\\s+", "-");
        if (tag.isEmpty()) return;
        if (!tagsModel.contains(tag)) {
            tagsModel.addElement(tag);
        }
        txtNewTag.setText("");
    }

    private void deleteSelectedTag() {
        String sel = tagsList.getSelectedValue();
        if (sel != null) tagsModel.removeElement(sel);
    }

    private void saveAll() {
        lblSaveStatus.setText("Kaydediliyor...");

        // 1. README'yi diske kaydet
        try {
            File spmFolder = project.getSpmFolder();
            if (!spmFolder.exists()) spmFolder.mkdirs();
            File readmeFile = new File(spmFolder, "README.md");
            Files.write(readmeFile.toPath(), txtReadme.getText().getBytes("utf-8"));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "README kaydedilemedi: " + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
            lblSaveStatus.setText("Hata olustu.");
            return;
        }

        // 2. Etiketleri guncelle
        List<String> newTags = new ArrayList<>();
        for (int i = 0; i < tagsModel.size(); i++) newTags.add(tagsModel.get(i));
        project.getTags().clear();
        project.getTags().addAll(newTags);
        repository.updateTags(project.getId(), newTags);

        // 3. Aciklamayi guncelle (README'nin ilk 300 karakteri)
        String md = txtReadme.getText();
        String desc = md.substring(0, Math.min(md.length(), 300));
        project.setDescription(desc);
        repository.updateDescription(project.getId(), desc);

        lblSaveStatus.setText("Kaydedildi!");
        JOptionPane.showMessageDialog(this,
                "README.md ve etiketler basariyla kaydedildi.",
                "Tamam", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }
}
