package com.smartproject.gui;

import com.smartproject.ai.AIAnalyzer;
import com.smartproject.config.ConfigManager;
import com.smartproject.db.ProjectRepository;
import com.smartproject.file.FileManager;
import com.smartproject.git.GitManager;
import com.smartproject.model.Project;
import com.smartproject.scanner.DockerScanner;
import com.smartproject.scanner.ProjectScanner;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import java.util.List;

public class MainGUI extends JFrame {

    // --- Ayarlar Sekmesi ---
    private JTextField txtApiKey;
    private JTextField txtGeminiApiKey;
    private JTextField txtGeminiModel;
    private JTextField txtGptApiKey;
    private JTextField txtGptApiUrl;
    private JTextField txtGptModel;
    private JRadioButton rbUseGroq;
    private JRadioButton rbUseGemini;
    private JRadioButton rbUseGpt;
    private JRadioButton rbUseCustom;
    private JTextField txtGithubUser;
    private JTextField txtGithubToken;
    private JLabel     lblDbInfo;   // DB kayit sayisi - canli guncellenir
    private AssistantPanel assistantPanel;

    // --- Veritabanı Sekmesi ---
    private JPanel databasePanel;
    private JList<Project> dbProjectJList;
    private DefaultListModel<Project> dbListModel;
    private JLabel lblDbDetailName;
    private JLabel lblDbDetailPath;
    private JLabel lblDbDetailSource;
    private JLabel lblDbDetailLangs;
    private JLabel lblDbDetailFileCount;
    private JLabel lblDbDetailDate;
    private JLabel lblDbDetailTags;
    private JTextArea txtDbReadme;
    private JButton btnDbDelete;
    private JButton btnDbRefresh;
    private JButton btnDbEdit;
    private JButton btnDbGitPush;
    private JComboBox<String> cbProfiles;

    // --- Veritabanı Entegrasyon Ayarları GUI Elemanları ---
    private JComboBox<String> cbDbType;
    private JLabel lblDbUrl;
    private JTextField txtDbUrl;
    private JLabel lblDbName;
    private JTextField txtDbName;
    private JLabel lblDbTable;
    private JTextField txtDbTable;
    private JLabel lblDbUser;
    private JTextField txtDbUser;
    private JLabel lblDbPass;
    private JPasswordField txtDbPass;
    private JLabel lblDbDriver;
    private JTextField txtDbDriver;

    // --- Projeler Sekmesi ---
    private JList<Project> projectJList;
    private DefaultListModel<Project> listModel;
    private JTextArea consoleArea;
    private JButton btnSelectFolder;
    private JButton btnAnalyze;        // Tek proje analizi
    private JButton btnAnalyzeBatch;   // Secili projeleri sirayla analiz
    private JButton btnGitCommit;
    private JButton btnDockerScan;

    // --- Detay & Etiket Paneli ---
    private JLabel lblDetailName;
    private JLabel lblDetailPath;
    private JLabel lblDetailLangs;
    private JLabel lblDetailFileCount;
    private JLabel lblDetailDate;
    private JLabel lblDetailSource;
    private DefaultListModel<String> filesListModel;
    private JList<String> filesJList;
    private JTextField txtTagInput;
    private DefaultListModel<String> tagsListModel;
    private JList<String> tagsJList;

    // --- Is Siniflari ---
    private ProjectScanner    scanner;
    private DockerScanner     dockerScanner;
    private AIAnalyzer        aiAnalyzer;
    private FileManager       fileManager;
    private GitManager        gitManager;
    private ProjectRepository repository;
    private ConfigManager     config;

    public MainGUI() {
        super("Smart Project Manager");
        scanner       = new ProjectScanner();
        dockerScanner = new DockerScanner();
        aiAnalyzer    = new AIAnalyzer();
        fileManager   = new FileManager();
        gitManager    = new GitManager();
        config        = new ConfigManager();
        repository    = new ProjectRepository(config);

        initUI();
        loadConfig();          // Kayitli ayarlari yukle
        loadFromDatabase();    // Eski projeleri yukle
    }

    // ====================================================
    // Ana UI
    // ====================================================
    private void initUI() {
        setSize(1100, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        assistantPanel = createAssistantPanel();
        databasePanel = createDatabasePanel();

        tabs.addTab("Projeler", createProjectsPanel());
        tabs.addTab("Veritabanı", databasePanel);
        tabs.addTab("Asistan",  assistantPanel);
        
        JScrollPane settingsScroll = new JScrollPane(createSettingsPanel());
        settingsScroll.setBorder(BorderFactory.createEmptyBorder());
        settingsScroll.getVerticalScrollBar().setUnitIncrement(16);
        tabs.addTab("Ayarlar",  settingsScroll);

        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() == assistantPanel) {
                assistantPanel.refreshActiveModel();
            } else if (tabs.getSelectedComponent() == databasePanel) {
                loadDatabaseTab();
            }
        });

        add(tabs);
    }

    // ====================================================
    // Projeler Sekmesi
    // ====================================================
    private JPanel createProjectsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- Ust: Butonlar ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));

        btnSelectFolder = new JButton("Klasor Sec ve Tara");
        btnSelectFolder.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSelectFolder.addActionListener(e -> selectAndScan());

        btnDockerScan = new JButton("Docker (Yerel)");
        btnDockerScan.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnDockerScan.addActionListener(e -> openDockerDialog());

        JButton btnRemote = new JButton("Uzak Baglanti");
        btnRemote.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnRemote.setBackground(new Color(60, 100, 180));
        btnRemote.setForeground(Color.WHITE);
        btnRemote.setFocusPainted(false);
        btnRemote.addActionListener(e -> openRemoteDialog());

        JButton btnClearList = new JButton("Listeyi Temizle");
        btnClearList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnClearList.addActionListener(e -> {
            listModel.clear();
            log("Liste temizlendi.");
        });

        topPanel.add(btnSelectFolder);
        topPanel.add(btnDockerScan);
        topPanel.add(btnRemote);
        topPanel.add(btnClearList);

        JLabel lblHint = new JLabel("Secilen klasordeki projeler listelenir ve veritabanina kaydedilir.");
        lblHint.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        topPanel.add(lblHint);

        panel.add(topPanel, BorderLayout.NORTH);

        // --- Orta: SplitPane ---
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(280);
        split.setContinuousLayout(true);

        // Sol: Proje listesi - COKLU SECIM
        listModel = new DefaultListModel<>();
        projectJList = new JList<>(listModel);
        projectJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        projectJList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        projectJList.addListSelectionListener(new ProjectSelectionListener());
        projectJList.setToolTipText("Ctrl veya Shift ile birden fazla proje secebilirsiniz");

        JScrollPane listScroll = new JScrollPane(projectJList);
        listScroll.setBorder(BorderFactory.createTitledBorder("Projeler"));
        split.setLeftComponent(listScroll);

        // Sag: Detay + Etiket paneli (dikey split)
        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        rightSplit.setDividerLocation(350);
        rightSplit.setTopComponent(createDetailsPanel());
        rightSplit.setBottomComponent(createTagPanel());
        split.setRightComponent(rightSplit);

        panel.add(split, BorderLayout.CENTER);

        // --- Alt: Konsol + Aksiyon butonlari ---
        JPanel bottomContainer = new JPanel(new BorderLayout(5, 5));

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        btnAnalyze = new JButton("Secili Projeyi Analiz Et");
        btnAnalyze.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnAnalyze.setEnabled(false);
        btnAnalyze.setToolTipText("Listeden secili olan ilk projeyi analiz eder");
        btnAnalyze.addActionListener(e -> analyzeSelected());

        btnAnalyzeBatch = new JButton("Secilenleri Sirayla Analiz Et");
        btnAnalyzeBatch.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnAnalyzeBatch.setEnabled(false);
        btnAnalyzeBatch.setToolTipText("Ctrl/Shift ile sectiklerinizi sirayla analiz eder");
        btnAnalyzeBatch.addActionListener(e -> analyzeBatch());

        JButton btnEdit = new JButton("Projeyi Duzenle");
        btnEdit.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnEdit.setToolTipText("Analiz edilmis projenin README ve etiketlerini duzenle");
        btnEdit.addActionListener(e -> openEditDialog());

        btnGitCommit = new JButton("GitHub'a Gonder");
        btnGitCommit.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnGitCommit.setEnabled(false);
        btnGitCommit.addActionListener(e -> openGitDialog());

        actionPanel.add(btnAnalyze);
        actionPanel.add(btnAnalyzeBatch);
        actionPanel.add(btnEdit);
        actionPanel.add(btnGitCommit);

        consoleArea = new JTextArea(5, 60);
        consoleArea.setEditable(false);
        consoleArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        JScrollPane consoleScroll = new JScrollPane(consoleArea);
        consoleScroll.setBorder(BorderFactory.createTitledBorder("Islem Loglari"));

        bottomContainer.add(consoleScroll, BorderLayout.CENTER);
        bottomContainer.add(actionPanel, BorderLayout.SOUTH);

        panel.add(bottomContainer, BorderLayout.SOUTH);
        return panel;
    }

    // ====================================================
    // Proje Detay Paneli
    // ====================================================
    private JPanel createDetailsPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createTitledBorder("Proje Detaylari"));

        JPanel info = new JPanel(new GridLayout(6, 1, 3, 3));
        info.setBorder(new EmptyBorder(8, 8, 8, 8));

        lblDetailName   = new JLabel("Proje: (Secilmedi)");
        lblDetailName.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblDetailName.setForeground(new Color(100, 150, 255));

        lblDetailPath   = new JLabel("Yol: -");
        lblDetailPath.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        lblDetailSource = new JLabel("Kaynak: -");
        lblDetailSource.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        lblDetailLangs  = new JLabel("Diller: -");
        lblDetailLangs.setFont(new Font("Segoe UI", Font.BOLD, 13));

        lblDetailFileCount = new JLabel("Dosya Sayisi: -");
        lblDetailFileCount.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        lblDetailDate   = new JLabel("Taranma Tarihi: -");
        lblDetailDate.setFont(new Font("Segoe UI", Font.ITALIC, 12));

        info.add(lblDetailName);
        info.add(lblDetailPath);
        info.add(lblDetailSource);
        info.add(lblDetailLangs);
        info.add(lblDetailFileCount);
        info.add(lblDetailDate);

        p.add(info, BorderLayout.NORTH);

        filesListModel = new DefaultListModel<>();
        filesJList = new JList<>(filesListModel);
        filesJList.setFont(new Font("Consolas", Font.PLAIN, 12));
        JScrollPane fs = new JScrollPane(filesJList);
        fs.setBorder(BorderFactory.createTitledBorder("Proje Dosyalari"));
        p.add(fs, BorderLayout.CENTER);

        return p;
    }

    // ====================================================
    // Etiket Paneli
    // ====================================================
    private JPanel createTagPanel() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(BorderFactory.createTitledBorder("Etiketler (Tags)"));
        p.setPreferredSize(new Dimension(0, 150));

        tagsListModel = new DefaultListModel<>();
        tagsJList = new JList<>(tagsListModel);
        tagsJList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tagsJList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        tagsJList.setVisibleRowCount(-1);
        p.add(new JScrollPane(tagsJList), BorderLayout.CENTER);

        JPanel inputRow = new JPanel(new BorderLayout(5, 0));
        inputRow.setBorder(new EmptyBorder(4, 4, 4, 4));

        txtTagInput = new JTextField();
        txtTagInput.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtTagInput.setToolTipText("Etiket ekle ve Enter'a bas");

        JButton btnAddTag = new JButton("Ekle");
        JButton btnRemTag = new JButton("Sil");

        btnAddTag.addActionListener(e -> addTagToSelected());
        btnRemTag.addActionListener(e -> removeTagFromSelected());
        txtTagInput.addActionListener(e -> addTagToSelected());

        JPanel btnRow = new JPanel(new GridLayout(1, 2, 4, 0));
        btnRow.add(btnAddTag);
        btnRow.add(btnRemTag);

        inputRow.add(txtTagInput, BorderLayout.CENTER);
        inputRow.add(btnRow, BorderLayout.EAST);

        p.add(inputRow, BorderLayout.SOUTH);
        return p;
    }

    // ====================================================
    // Asistan Sekmesi
    // ====================================================
    private AssistantPanel createAssistantPanel() {
        return new AssistantPanel(repository,
                () -> getActiveAiService(),
                () -> getActiveApiKey(),
                () -> getActiveGeminiModel(),
                () -> getGptApiUrlValue(),
                () -> getGptModelValue());
    }

    // ====================================================
    // Ayarlar Sekmesi
    // ====================================================
    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 10, 6, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // AI Ayarlari
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel t1 = new JLabel("Yapay Zeka (AI) Ayarlari");
        t1.setFont(new Font("Segoe UI", Font.BOLD, 17));
        panel.add(t1, gbc);

        gbc.gridy = 1;
        panel.add(new JLabel("<html>Kullanmak istediginiz servisi secip ilgili API anahtarini girin.</html>"), gbc);

        // Groq
        gbc.gridy = 2; gbc.gridwidth = 2;
        rbUseGroq = new JRadioButton("Groq (llama-3.3-70b-versatile)");
        rbUseGroq.setFont(new Font("Segoe UI", Font.BOLD, 12));
        panel.add(rbUseGroq, gbc);

        gbc.gridy = 3; gbc.gridwidth = 1;
        panel.add(new JLabel("  Groq API Anahtari:"), gbc);
        txtApiKey = new JPasswordField(32);
        gbc.gridx = 1; panel.add(txtApiKey, gbc);

        // Gemini
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        rbUseGemini = new JRadioButton("Gemini Free");
        rbUseGemini.setFont(new Font("Segoe UI", Font.BOLD, 12));
        panel.add(rbUseGemini, gbc);

        gbc.gridy = 5; gbc.gridwidth = 1;
        panel.add(new JLabel("  Gemini API Anahtari:"), gbc);
        txtGeminiApiKey = new JPasswordField(32);
        gbc.gridx = 1; panel.add(txtGeminiApiKey, gbc);

        gbc.gridx = 0; gbc.gridy = 6;
        panel.add(new JLabel("  Gemini Model:"), gbc);
        txtGeminiModel = new JTextField(32);
        gbc.gridx = 1; panel.add(txtGeminiModel, gbc);

        // GPT (OpenAI)
        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
        rbUseGpt = new JRadioButton("OpenAI (GPT) - [Ücretli / Bakiye Gerekir]");
        rbUseGpt.setFont(new Font("Segoe UI", Font.BOLD, 12));
        panel.add(rbUseGpt, gbc);

        gbc.gridy = 8; gbc.gridwidth = 1;
        panel.add(new JLabel("  OpenAI API Anahtari:"), gbc);
        txtGptApiKey = new JPasswordField(32);
        gbc.gridx = 1; panel.add(txtGptApiKey, gbc);

        // Custom API
        gbc.gridx = 0; gbc.gridy = 9; gbc.gridwidth = 2;
        rbUseCustom = new JRadioButton("Custom OpenAI Compatible API (Ollama, local, proxy)");
        rbUseCustom.setFont(new Font("Segoe UI", Font.BOLD, 12));
        panel.add(rbUseCustom, gbc);

        gbc.gridy = 10; gbc.gridwidth = 1;
        panel.add(new JLabel("  Custom API URL:"), gbc);
        txtGptApiUrl = new JTextField(32);
        gbc.gridx = 1; panel.add(txtGptApiUrl, gbc);

        gbc.gridx = 0; gbc.gridy = 11;
        panel.add(new JLabel("  Custom Model:"), gbc);
        txtGptModel = new JTextField(32);
        gbc.gridx = 1; panel.add(txtGptModel, gbc);

        ButtonGroup aiGroup = new ButtonGroup();
        aiGroup.add(rbUseGroq);
        aiGroup.add(rbUseGemini);
        aiGroup.add(rbUseGpt);
        aiGroup.add(rbUseCustom);

        ActionListener aiToggle = e -> {
            txtApiKey.setEnabled(rbUseGroq.isSelected());
            txtGeminiApiKey.setEnabled(rbUseGemini.isSelected());
            txtGeminiModel.setEnabled(rbUseGemini.isSelected());
            txtGptApiKey.setEnabled(rbUseGpt.isSelected() || rbUseCustom.isSelected());
            txtGptApiUrl.setEnabled(rbUseCustom.isSelected());
            txtGptModel.setEnabled(rbUseCustom.isSelected());
        };
        rbUseGroq.addActionListener(aiToggle);
        rbUseGemini.addActionListener(aiToggle);
        rbUseGpt.addActionListener(aiToggle);
        rbUseCustom.addActionListener(aiToggle);

        // GitHub
        gbc.gridx = 0; gbc.gridy = 13; gbc.gridwidth = 2;
        panel.add(new JSeparator(), gbc);

        gbc.gridy = 14;
        JLabel t2 = new JLabel("GitHub Ayarlari");
        t2.setFont(new Font("Segoe UI", Font.BOLD, 17));
        panel.add(t2, gbc);

        gbc.gridy = 15;
        panel.add(new JLabel("<html>GitHub'a push icin kullanici adi ve Personal Access Token (Classic) gereklidir.</html>"), gbc);

        gbc.gridy = 16; gbc.gridwidth = 1;
        panel.add(new JLabel("GitHub Kullanici Adi:"), gbc);
        txtGithubUser = new JTextField(32);
        gbc.gridx = 1; panel.add(txtGithubUser, gbc);

        gbc.gridx = 0; gbc.gridy = 17;
        panel.add(new JLabel("GitHub Token:"), gbc);
        txtGithubToken = new JPasswordField(32);
        gbc.gridx = 1; panel.add(txtGithubToken, gbc);

        // --- Database Entegrasyon Ayarlari ---
        gbc.gridx = 0; gbc.gridy = 18; gbc.gridwidth = 2;
        panel.add(new JSeparator(), gbc);

        gbc.gridy = 19;
        JLabel tDbSettings = new JLabel("Veritabanı Entegrasyon Ayarları");
        tDbSettings.setFont(new Font("Segoe UI", Font.BOLD, 17));
        panel.add(tDbSettings, gbc);

        gbc.gridy = 20;
        panel.add(new JLabel("<html>Projelerin kaydedileceği veritabanı türünü seçin ve bağlantı detaylarını girin.</html>"), gbc);

        gbc.gridy = 21; gbc.gridwidth = 1;
        panel.add(new JLabel("Veritabanı Türü:"), gbc);
        cbDbType = new JComboBox<>(new String[]{"JSON Dosyası (Yerel)", "MongoDB (Cloud / Atlas / Yerel)", "SQL Veritabanı (SQLite, MySQL, vb.)"});
        gbc.gridx = 1; panel.add(cbDbType, gbc);

        gbc.gridx = 0; gbc.gridy = 22;
        lblDbUrl = new JLabel("Bağlantı Adresi (URL/URI):");
        panel.add(lblDbUrl, gbc);
        txtDbUrl = new JTextField(32);
        gbc.gridx = 1; panel.add(txtDbUrl, gbc);

        gbc.gridx = 0; gbc.gridy = 23;
        lblDbName = new JLabel("Veritabanı Adı:");
        panel.add(lblDbName, gbc);
        txtDbName = new JTextField(32);
        gbc.gridx = 1; panel.add(txtDbName, gbc);

        gbc.gridx = 0; gbc.gridy = 24;
        lblDbTable = new JLabel("Tablo / Koleksiyon Adı:");
        panel.add(lblDbTable, gbc);
        txtDbTable = new JTextField(32);
        gbc.gridx = 1; panel.add(txtDbTable, gbc);

        gbc.gridx = 0; gbc.gridy = 25;
        lblDbUser = new JLabel("Kullanıcı Adı (SQL):");
        panel.add(lblDbUser, gbc);
        txtDbUser = new JTextField(32);
        gbc.gridx = 1; panel.add(txtDbUser, gbc);

        gbc.gridx = 0; gbc.gridy = 26;
        lblDbPass = new JLabel("Şifre (SQL):");
        panel.add(lblDbPass, gbc);
        txtDbPass = new JPasswordField(32);
        gbc.gridx = 1; panel.add(txtDbPass, gbc);

        gbc.gridx = 0; gbc.gridy = 27;
        lblDbDriver = new JLabel("JDBC Sürücü Sınıfı (SQL - Opsiyonel):");
        panel.add(lblDbDriver, gbc);
        txtDbDriver = new JTextField(32);
        gbc.gridx = 1; panel.add(txtDbDriver, gbc);

        ActionListener dbTypeToggle = e -> {
            String selected = (String) cbDbType.getSelectedItem();
            boolean isJson = "JSON Dosyası (Yerel)".equals(selected);
            boolean isMongo = "MongoDB (Cloud / Atlas / Yerel)".equals(selected);
            boolean isSql = "SQL Veritabanı (SQLite, MySQL, vb.)".equals(selected);

            lblDbUrl.setVisible(!isJson);
            txtDbUrl.setVisible(!isJson);

            lblDbName.setVisible(isMongo);
            txtDbName.setVisible(isMongo);

            lblDbTable.setVisible(!isJson);
            txtDbTable.setVisible(!isJson);

            lblDbUser.setVisible(isSql);
            txtDbUser.setVisible(isSql);

            lblDbPass.setVisible(isSql);
            txtDbPass.setVisible(isSql);

            lblDbDriver.setVisible(isSql);
            txtDbDriver.setVisible(isSql);

            panel.revalidate();
            panel.repaint();
        };
        cbDbType.addActionListener(dbTypeToggle);

        // Kaydet / Yukle butonlari
        gbc.gridx = 0; gbc.gridy = 28; gbc.gridwidth = 2;
        JPanel settingsBtnPanel = new JPanel(new GridLayout(1, 2, 8, 0));

        JButton btnSaveSettings = new JButton("Ayarlari Kaydet");
        btnSaveSettings.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSaveSettings.addActionListener(e -> saveConfig());

        JButton btnLoadSettings = new JButton("Ayarlari Yukle");
        btnLoadSettings.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnLoadSettings.addActionListener(e -> {
            loadConfig();
            JOptionPane.showMessageDialog(this,
                    "Kayitli ayarlar basariyla yuklendi.",
                    "Yuklendi", JOptionPane.INFORMATION_MESSAGE);
        });

        settingsBtnPanel.add(btnSaveSettings);
        settingsBtnPanel.add(btnLoadSettings);
        panel.add(settingsBtnPanel, gbc);

        // Profil Yonetimi
        gbc.gridy = 29;
        panel.add(new JSeparator(), gbc);

        gbc.gridy = 30;
        JLabel lblProfileTitle = new JLabel("Profil Yonetimi");
        lblProfileTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        panel.add(lblProfileTitle, gbc);

        gbc.gridy = 31;
        JPanel profilePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));

        cbProfiles = new JComboBox<>();
        cbProfiles.setPreferredSize(new Dimension(180, 26));
        refreshProfilesCombo();

        JButton btnLoadProfile = new JButton("Yükle");
        btnLoadProfile.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnLoadProfile.addActionListener(e -> {
            String selected = (String) cbProfiles.getSelectedItem();
            if (selected != null && !selected.isEmpty()) {
                loadProfile(selected);
            }
        });

        JButton btnSaveAsProfile = new JButton("Farklı Kaydet...");
        btnSaveAsProfile.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnSaveAsProfile.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this, "Profil İsmi Girin:", "Farklı Kaydet", JOptionPane.PLAIN_MESSAGE);
            if (name != null) {
                name = name.trim().replaceAll("[^a-zA-Z0-9_\\-]", "");
                if (!name.isEmpty()) {
                    saveConfigSilently();
                    config.saveProfile(name);
                    refreshProfilesCombo();
                    cbProfiles.setSelectedItem(name);
                    log("Profil kaydedildi: " + name);
                    JOptionPane.showMessageDialog(this, "Profil '" + name + "' basariyla kaydedildi.", "Kaydedildi", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        JButton btnDeleteProfile = new JButton("Sil");
        btnDeleteProfile.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnDeleteProfile.setForeground(new Color(220, 60, 60));
        btnDeleteProfile.addActionListener(e -> {
            String selected = (String) cbProfiles.getSelectedItem();
            if (selected != null && !selected.isEmpty()) {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "'" + selected + "' profilini silmek istediğinize emin misiniz?",
                        "Profil Sil", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    config.deleteProfile(selected);
                    refreshProfilesCombo();
                    log("Profil silindi: " + selected);
                }
            }
        });

        profilePanel.add(new JLabel("Aktif Profil: "));
        profilePanel.add(cbProfiles);
        profilePanel.add(btnLoadProfile);
        profilePanel.add(btnSaveAsProfile);
        profilePanel.add(btnDeleteProfile);

        panel.add(profilePanel, gbc);

        // DB bilgisi
        gbc.gridy = 32;
        panel.add(new JSeparator(), gbc);

        gbc.gridy = 33;
        JLabel t3 = new JLabel("Veritabani Bilgisi");
        t3.setFont(new Font("Segoe UI", Font.BOLD, 17));
        panel.add(t3, gbc);

        gbc.gridy = 34;
        lblDbInfo = new JLabel(getDbInfoText());
        lblDbInfo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        panel.add(lblDbInfo, gbc);

        gbc.gridy = 35;
        JButton btnClearDb = new JButton("Veritabanini Temizle");
        btnClearDb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnClearDb.setForeground(new Color(220, 60, 60));
        btnClearDb.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "<html>Tum analiz kayitlari silinecek.<br>README dosyalari korunur.<br><br>Devam?</html>",
                    "DB Temizle", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                repository.clear();
                refreshDbInfo();
                log("Veritabani temizlendi.");
                JOptionPane.showMessageDialog(this, "Veritabani temizlendi.", "Tamam", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        panel.add(btnClearDb, gbc);

        gbc.gridy = 36; gbc.weighty = 1.0;
        panel.add(new JLabel(""), gbc);
        return panel;
    }

    // DB etiket metnini olustur
    private String getDbInfoText() {
        return "<html>Analiz edilen projeler kaydedilir:<br>"
                + "<code>" + repository.getDbPath() + "</code><br>"
                + "Kayitli proje sayisi: <b>" + repository.count() + "</b></html>";
    }

    // DB etiketini guncelle (her kayit/silme sonrasi cagir)
    private void refreshDbInfo() {
        if (lblDbInfo != null) lblDbInfo.setText(getDbInfoText());
    }

    private String getActiveAiService() {
        if (rbUseGemini != null && rbUseGemini.isSelected()) return "gemini";
        if (rbUseGpt != null && rbUseGpt.isSelected()) return "gpt";
        if (rbUseCustom != null && rbUseCustom.isSelected()) return "custom";
        return "groq";
    }

    private String getActiveApiKey() {
        if (rbUseGemini != null && rbUseGemini.isSelected()) {
            return txtGeminiApiKey != null ? txtGeminiApiKey.getText().trim() : "";
        }
        if ((rbUseGpt != null && rbUseGpt.isSelected() || rbUseCustom != null && rbUseCustom.isSelected())) {
            return txtGptApiKey != null ? txtGptApiKey.getText().trim() : "";
        }
        return txtApiKey != null ? txtApiKey.getText().trim() : "";
    }

    private String getActiveGeminiModel() {
        return txtGeminiModel != null ? txtGeminiModel.getText().trim() : "";
    }

    private String getGptApiUrlValue() {
        return txtGptApiUrl != null ? txtGptApiUrl.getText().trim() : "";
    }

    private String getGptModelValue() {
        return txtGptModel != null ? txtGptModel.getText().trim() : "";
    }

    // Ayarlari yukle
    private void loadConfig() {
        String service = config.getAiService();
        if ("gemini".equalsIgnoreCase(service)) {
            rbUseGemini.setSelected(true);
        } else if ("gpt".equalsIgnoreCase(service)) {
            rbUseGpt.setSelected(true);
        } else if ("custom".equalsIgnoreCase(service)) {
            rbUseCustom.setSelected(true);
        } else {
            rbUseGroq.setSelected(true);
        }

        txtApiKey.setText(config.getApiKey());
        txtGeminiApiKey.setText(config.getGeminiApiKey());
        
        String geminiM = config.getGeminiModel();
        if (geminiM == null || geminiM.trim().isEmpty()) geminiM = "gemini-2.5-flash";
        txtGeminiModel.setText(geminiM);

        txtGptApiKey.setText(config.getGptApiKey());

        String gptUrl = config.getGptApiUrl();
        if (gptUrl == null || gptUrl.trim().isEmpty()) gptUrl = "https://api.openai.com/v1/chat/completions";
        txtGptApiUrl.setText(gptUrl);

        String gptM = config.getGptModel();
        if (gptM == null || gptM.trim().isEmpty()) gptM = "gpt-4o-mini";
        txtGptModel.setText(gptM);

        txtGithubUser.setText(config.getGithubUser());
        txtGithubToken.setText(config.getGithubToken());

        // Database Ayarlari Yukleme
        if (cbDbType != null) {
            String dbType = config.getDbType();
            if ("mongodb".equalsIgnoreCase(dbType)) {
                cbDbType.setSelectedItem("MongoDB (Cloud / Atlas / Yerel)");
            } else if ("sql".equalsIgnoreCase(dbType)) {
                cbDbType.setSelectedItem("SQL Veritabanı (SQLite, MySQL, vb.)");
            } else {
                cbDbType.setSelectedItem("JSON Dosyası (Yerel)");
            }
            txtDbUrl.setText(config.getDbUrl());
            txtDbName.setText(config.getDbName());
            txtDbTable.setText(config.getDbTable());
            txtDbUser.setText(config.getDbUser());
            txtDbPass.setText(config.getDbPass());
            txtDbDriver.setText(config.getDbDriver());

            boolean isJson = "json".equalsIgnoreCase(dbType);
            boolean isMongo = "mongodb".equalsIgnoreCase(dbType);
            boolean isSql = "sql".equalsIgnoreCase(dbType);

            lblDbUrl.setVisible(!isJson);
            txtDbUrl.setVisible(!isJson);

            lblDbName.setVisible(isMongo);
            txtDbName.setVisible(isMongo);

            lblDbTable.setVisible(!isJson);
            txtDbTable.setVisible(!isJson);

            lblDbUser.setVisible(isSql);
            txtDbUser.setVisible(isSql);

            lblDbPass.setVisible(isSql);
            txtDbPass.setVisible(isSql);

            lblDbDriver.setVisible(isSql);
            txtDbDriver.setVisible(isSql);

            if (cbDbType.getParent() != null) {
                cbDbType.getParent().revalidate();
                cbDbType.getParent().repaint();
            }
        }

        // Enable/disable key fields appropriately
        txtApiKey.setEnabled(rbUseGroq.isSelected());
        txtGeminiApiKey.setEnabled(rbUseGemini.isSelected());
        txtGeminiModel.setEnabled(rbUseGemini.isSelected());
        txtGptApiKey.setEnabled(rbUseGpt.isSelected() || rbUseCustom.isSelected());
        txtGptApiUrl.setEnabled(rbUseCustom.isSelected());
        txtGptModel.setEnabled(rbUseCustom.isSelected());

        log("Ayarlar yuklendi. AI Servisi: " + service);

        if (repository != null) {
            repository.refreshProvider();
            loadFromDatabase();
            loadDatabaseTab();
        }
    }

    // Ayarlari kaydet sessizce
    private void saveConfigSilently() {
        String service = "groq";
        if (rbUseGemini.isSelected()) service = "gemini";
        else if (rbUseGpt.isSelected()) service = "gpt";
        else if (rbUseCustom.isSelected()) service = "custom";

        config.setAiService(service);
        config.setApiKey(txtApiKey.getText().trim());
        config.setGeminiApiKey(txtGeminiApiKey.getText().trim());
        config.setGeminiModel(txtGeminiModel.getText().trim());
        config.setGptApiKey(txtGptApiKey.getText().trim());
        config.setGptApiUrl(txtGptApiUrl.getText().trim());
        config.setGptModel(txtGptModel.getText().trim());
        config.setGithubUser(txtGithubUser.getText().trim());
        config.setGithubToken(txtGithubToken.getText().trim());

        // Database Ayarlarini Kaydet
        if (cbDbType != null) {
            String dbType = "json";
            String selectedDb = (String) cbDbType.getSelectedItem();
            if ("MongoDB (Cloud / Atlas / Yerel)".equals(selectedDb)) {
                dbType = "mongodb";
            } else if ("SQL Veritabanı (SQLite, MySQL, vb.)".equals(selectedDb)) {
                dbType = "sql";
            }
            config.setDbType(dbType);
            config.setDbUrl(txtDbUrl.getText().trim());
            config.setDbName(txtDbName.getText().trim());
            config.setDbTable(txtDbTable.getText().trim());
            config.setDbUser(txtDbUser.getText().trim());
            config.setDbPass(new String(txtDbPass.getPassword()).trim());
            config.setDbDriver(txtDbDriver.getText().trim());
        }

        config.save();
        repository.refreshProvider(); // Refresh active database connection pool
        refreshDbInfo();             // Refresh connection path/status label
        loadFromDatabase();          // Reload Projects tab list
        loadDatabaseTab();           // Reload Veritabanı tab list

        if (assistantPanel != null) {
            assistantPanel.refreshActiveModel();
        }
    }

    // Ayarlari kaydet
    private void saveConfig() {
        saveConfigSilently();
        JOptionPane.showMessageDialog(this, "Ayarlar kaydedildi.", "Tamam", JOptionPane.INFORMATION_MESSAGE);
    }

    // ====================================================
    // Veritabanindan Yukle
    // ====================================================
    private void loadFromDatabase() {
        List<ProjectRepository.ProjectEntry> saved = repository.getAllEntries();
        if (saved.isEmpty()) return;
        listModel.clear();
        for (ProjectRepository.ProjectEntry entry : saved) {
            Project p = new Project(entry);
            listModel.addElement(p);
        }
        log("Veritabanindan " + saved.size() + " proje yuklendi ve listeye eklendi.");
        refreshDbInfo();
    }

    // ====================================================
    // Aksiyon Metodlari
    // ====================================================
    private void selectAndScan() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Taranacak Ana Klasoru Secin");
        // Son taranan klasoru hatirlat
        String lastFolder = config.getLastFolder();
        if (!lastFolder.isEmpty()) {
            File last = new File(lastFolder);
            if (last.exists()) chooser.setCurrentDirectory(last);
        }
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File dir = chooser.getSelectedFile();
        config.setLastFolder(dir.getAbsolutePath());
        config.save();
        log("Tarama baslatildi: " + dir.getAbsolutePath());
        btnSelectFolder.setEnabled(false);

        new SwingWorker<List<Project>, Void>() {
            @Override protected List<Project> doInBackground() {
                return scanner.scanDirectory(dir);
            }
            @Override protected void done() {
                try {
                    List<Project> projects = get();
                    listModel.clear();
                    for (Project p : projects) {
                        listModel.addElement(p);
                        // DB'ye KAYDETME - sadece analiz sonrasi kaydedilecek!
                    }
                    log("Tarama tamamlandi! " + projects.size() + " proje bulundu.");
                    log("Not: Projeler analiz edildikten sonra veritabanina kaydedilir.");
                } catch (Exception ex) {
                    log("Hata: " + ex.getMessage());
                } finally {
                    btnSelectFolder.setEnabled(true);
                }
            }
        }.execute();
    }

    private void openDockerDialog() {
        DockerScanDialog dlg = new DockerScanDialog(this, dockerScanner, listModel, repository);
        dlg.setVisible(true);
    }

    private void openRemoteDialog() {
        RemoteConnectionDialog dlg = new RemoteConnectionDialog(this, dockerScanner, listModel, repository, config);
        dlg.setVisible(true);
        refreshDbInfo();
    }

    private void analyzeProject(Project p, String service, String apiKey, String geminiModel, String gptApiUrl, String gptModel, String githubUser, int index, int toplam) {
        new SwingWorker<Void, String>() {
            String readme;
            List<String> autoTags;

            @Override protected Void doInBackground() throws Exception {
                // 1. README olustur
                readme = aiAnalyzer.generateReadme(p, service, apiKey, geminiModel, gptApiUrl, gptModel, githubUser);
                // 2. Otomatik etiket uret
                autoTags = aiAnalyzer.generateTags(p, service, apiKey, geminiModel, gptApiUrl, gptModel);
                return null;
            }

            @Override protected void done() {
                try {
                    get(); // exception varsa fiyatlatir

                    // README'yi kaydet
                    fileManager.saveAnalysisResult(p, readme);
                    p.setDescription(readme.substring(0, Math.min(readme.length(), 300)));

                    // Otomatik taglari ekle (varsa)
                    if (autoTags != null) {
                        for (String t : autoTags) p.addTag(t);
                    }

                    // DB'ye SIMDI kaydet (sadece analiz sonrasi!)
                    repository.saveProject(p);
                    refreshDbInfo();

                    String tagStr = p.getTags().isEmpty() ? "" : "  Etiketler: " + String.join(", ", p.getTags());
                    if (toplam > 1) {
                        log("[" + index + "/" + toplam + "] Tamamlandi: " + p.getDisplayName() + tagStr);
                    } else {
                        log("Analiz tamamlandi: " + p.getDisplayName() + tagStr);
                    }

                    // Secili proje guncellendiyse detay panelini yenile
                    if (projectJList.getSelectedValue() == p) {
                        tagsListModel.clear();
                        for (String t : p.getTags()) tagsListModel.addElement(t);
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    String detail = ex.getCause() != null ? " (" + ex.getCause().toString() + ")" : "";
                    log("Hata [" + p.getDisplayName() + "]: " + ex.getMessage() + detail);
                }
            }
        }.execute();
    }

    private void openEditDialog() {
        Project p = projectJList.getSelectedValue();
        if (p == null) {
            JOptionPane.showMessageDialog(this, "Lutfen listeden bir proje secin.", "Uyari", JOptionPane.WARNING_MESSAGE);
            return;
        }
        File spmFolder = p.getSpmFolder();
        if (!spmFolder.exists()) {
            JOptionPane.showMessageDialog(this,
                    "Bu proje henuz analiz edilmedi.\nOnce 'Analiz Et' butonuna basin.",
                    "Analiz Gerekli", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ProjectEditDialog dlg = new ProjectEditDialog(this, p, repository);
        dlg.setVisible(true);
        // Kapandiktan sonra etiket panelini yenile
        tagsListModel.clear();
        for (String t : p.getTags()) tagsListModel.addElement(t);
    }

    private void analyzeSelected() {
        List<Project> selected = projectJList.getSelectedValuesList();
        if (selected.isEmpty()) return;
        Project p = selected.get(0);
        String service = getActiveAiService();
        String apiKey = getActiveApiKey();
        String geminiModel = getActiveGeminiModel();
        String gptApiUrl = getGptApiUrlValue();
        String gptModel = getGptModelValue();
        String githubUser = txtGithubUser.getText().trim();
        if (apiKey.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Lutfen Ayarlar sekmesinden " + service.toUpperCase() + " API anahtarini girin!", "API Key Eksik", JOptionPane.WARNING_MESSAGE);
            return;
        }
        log("AI analizi ve otomatik etiketleme baslatildi: " + p.getDisplayName() + " (Servis: " + service.toUpperCase() + ")");
        btnAnalyze.setEnabled(false);
        analyzeProject(p, service, apiKey, geminiModel, gptApiUrl, gptModel, githubUser, 1, 1);
        btnAnalyze.setEnabled(true);
    }

    /**
     * Secili tum projeleri sira sira (bir bitince digeri) analiz eder.
     */
    private void analyzeBatch() {
        List<Project> selected = new ArrayList<>(projectJList.getSelectedValuesList());
        if (selected.isEmpty()) return;

        String service = getActiveAiService();
        String apiKey = getActiveApiKey();
        String geminiModel = getActiveGeminiModel();
        String gptApiUrl = getGptApiUrlValue();
        String gptModel = getGptModelValue();
        String githubUser = txtGithubUser.getText().trim();
        if (apiKey.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Lutfen Ayarlar sekmesinden " + service.toUpperCase() + " API anahtarini girin!", "API Key Eksik", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int toplam = selected.size();
        log("Toplu analiz baslatildi: " + toplam + " proje sirayla analiz edilecek. (Servis: " + service.toUpperCase() + ")");
        btnAnalyzeBatch.setEnabled(false);
        btnAnalyze.setEnabled(false);

        // Her projeyi sirayla analiz eden recursive SwingWorker
        analyzeNext(selected, 0, toplam, service, apiKey, geminiModel, gptApiUrl, gptModel, githubUser);
    }

    private void analyzeNext(List<Project> list, int index, int toplam, String service, String apiKey, String geminiModel, String gptApiUrl, String gptModel, String githubUser) {
        if (index >= list.size()) {
            log("Toplu analiz tamamlandi! " + toplam + " proje analiz edildi.");
            SwingUtilities.invokeLater(() -> {
                btnAnalyzeBatch.setEnabled(true);
                btnAnalyze.setEnabled(true);
            });
            return;
        }
        Project p = list.get(index);
        log("[" + (index + 1) + "/" + toplam + "] Analiz ediliyor: " + p.getDisplayName());

        new SwingWorker<Void, Void>() {
            String readme; List<String> autoTags;
            @Override protected Void doInBackground() throws Exception {
                readme   = aiAnalyzer.generateReadme(p, service, apiKey, geminiModel, gptApiUrl, gptModel, githubUser);
                autoTags = aiAnalyzer.generateTags(p, service, apiKey, geminiModel, gptApiUrl, gptModel);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    fileManager.saveAnalysisResult(p, readme);
                    p.setDescription(readme.substring(0, Math.min(readme.length(), 300)));
                    if (autoTags != null) for (String t : autoTags) p.addTag(t);
                    repository.saveProject(p);
                    refreshDbInfo();
                    
                    String tagStr = p.getTags().isEmpty() ? "" : "  Etiketler: " + String.join(", ", p.getTags());
                    log("[" + (index + 1) + "/" + toplam + "] Tamamlandi: " + p.getDisplayName() + tagStr);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    String detail = ex.getCause() != null ? " (" + ex.getCause().toString() + ")" : "";
                    log("[" + (index + 1) + "/" + toplam + "] Hata (" + p.getDisplayName() + "): " + ex.getMessage() + detail);
                }
                analyzeNext(list, index + 1, toplam, service, apiKey, geminiModel, gptApiUrl, gptModel, githubUser);
            }
        }.execute();
    }

    private void openGitDialog() {
        Project p = projectJList.getSelectedValue();
        if (p == null) return;
        String gitUser  = txtGithubUser.getText().trim();
        String gitToken = txtGithubToken.getText().trim();
        GithubOverlayDialog dlg = new GithubOverlayDialog(this, p, gitUser, gitToken, gitManager);
        dlg.setVisible(true);
    }

    private void openDbEditDialog() {
        Project p = dbProjectJList.getSelectedValue();
        if (p == null) return;
        File spmFolder = p.getSpmFolder();
        if (!spmFolder.exists()) {
            spmFolder.mkdirs();
        }
        ProjectEditDialog dlg = new ProjectEditDialog(this, p, repository);
        dlg.setVisible(true);
        loadDatabaseTab();
    }

    private void openDbGitDialog() {
        Project p = dbProjectJList.getSelectedValue();
        if (p == null) return;
        String gitUser  = txtGithubUser.getText().trim();
        String gitToken = txtGithubToken.getText().trim();
        GithubOverlayDialog dlg = new GithubOverlayDialog(this, p, gitUser, gitToken, gitManager);
        dlg.setVisible(true);
    }

    private void addTagToSelected() {
        Project p = projectJList.getSelectedValue();
        if (p == null) return;
        String tag = txtTagInput.getText().trim();
        if (tag.isEmpty()) return;
        p.addTag(tag);
        tagsListModel.addElement(tag);
        repository.updateTags(p.getId(), p.getTags());
        txtTagInput.setText("");
    }

    private void removeTagFromSelected() {
        Project p = projectJList.getSelectedValue();
        if (p == null) return;
        String selected = tagsJList.getSelectedValue();
        if (selected == null) return;
        p.removeTag(selected);
        tagsListModel.removeElement(selected);
        repository.updateTags(p.getId(), p.getTags());
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            consoleArea.append(">> " + msg + "\n");
            consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
        });
    }

    // ====================================================
    // Listener: Projeden birini secince detaylari goster
    // ====================================================
    private class ProjectSelectionListener implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting()) return;

            List<Project> selected = projectJList.getSelectedValuesList();
            boolean hasOne  = selected.size() == 1;
            boolean hasAny  = !selected.isEmpty();

            btnAnalyze.setEnabled(hasAny);
            btnAnalyzeBatch.setEnabled(selected.size() > 1);
            btnGitCommit.setEnabled(hasOne);

            // Detay paneli: sadece tek secimde goster
            if (!hasOne) return;
            Project sel = selected.get(0);

            lblDetailName.setText("Proje: " + sel.getDisplayName());
            lblDetailPath.setText("Yol: " + sel.getAbsolutePath());
            lblDetailSource.setText("Kaynak: " + sel.getSource().name());
            lblDetailLangs.setText("Diller: " + String.join(", ", sel.getLanguagesUsed()));
            lblDetailFileCount.setText("Dosya Sayisi: " + sel.getSourceFiles().size());
            lblDetailDate.setText("Taranma: " + sel.getScanDate());

            filesListModel.clear();
            for (File f : sel.getSourceFiles()) {
                filesListModel.addElement(f.getName() + "  (" + (f.length() / 1024) + " KB)");
            }

            tagsListModel.clear();
            for (String t : sel.getTags()) tagsListModel.addElement(t);
        }
    }

    // ====================================================
    // Veritabanı Sekmesi Panel ve Metodları
    // ====================================================
    private JPanel createDatabasePanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Sol panel: Proje listesi
        dbListModel = new DefaultListModel<>();
        dbProjectJList = new JList<>(dbListModel);
        dbProjectJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        dbProjectJList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dbProjectJList.addListSelectionListener(new DbProjectSelectionListener());

        JScrollPane listScroll = new JScrollPane(dbProjectJList);
        listScroll.setBorder(BorderFactory.createTitledBorder("Kayıtlı Projeler"));
        listScroll.setPreferredSize(new Dimension(280, 0));

        // Sağ panel: Detay + README
        JPanel rightPanel = new JPanel(new BorderLayout(8, 8));
        
        // Üst detay kartı
        JPanel infoPanel = new JPanel(new GridLayout(7, 1, 3, 3));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Proje Bilgileri"));
        
        lblDbDetailName = new JLabel("Proje: (Secilmedi)");
        lblDbDetailName.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblDbDetailName.setForeground(new Color(100, 150, 255));

        lblDbDetailPath = new JLabel("Yol: -");
        lblDbDetailPath.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        lblDbDetailSource = new JLabel("Kaynak: -");
        lblDbDetailSource.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        lblDbDetailLangs = new JLabel("Diller: -");
        lblDbDetailLangs.setFont(new Font("Segoe UI", Font.BOLD, 13));

        lblDbDetailFileCount = new JLabel("Dosya Sayisi: -");
        lblDbDetailFileCount.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        lblDbDetailDate = new JLabel("Taranma Tarihi: -");
        lblDbDetailDate.setFont(new Font("Segoe UI", Font.ITALIC, 12));

        lblDbDetailTags = new JLabel("Etiketler: -");
        lblDbDetailTags.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblDbDetailTags.setForeground(new Color(150, 200, 150));

        infoPanel.add(lblDbDetailName);
        infoPanel.add(lblDbDetailPath);
        infoPanel.add(lblDbDetailSource);
        infoPanel.add(lblDbDetailLangs);
        infoPanel.add(lblDbDetailFileCount);
        infoPanel.add(lblDbDetailDate);
        infoPanel.add(lblDbDetailTags);

        // Orta: README.md
        txtDbReadme = new JTextArea();
        txtDbReadme.setEditable(false);
        txtDbReadme.setFont(new Font("Consolas", Font.PLAIN, 13));
        txtDbReadme.setLineWrap(true);
        txtDbReadme.setWrapStyleWord(true);
        JScrollPane readmeScroll = new JScrollPane(txtDbReadme);
        readmeScroll.setBorder(BorderFactory.createTitledBorder("Analiz Sonucu (README.md)"));

        rightPanel.add(infoPanel, BorderLayout.NORTH);
        rightPanel.add(readmeScroll, BorderLayout.CENTER);

        // Split
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, rightPanel);
        split.setDividerLocation(280);
        split.setContinuousLayout(true);
        panel.add(split, BorderLayout.CENTER);

        // Alt: Butonlar
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));

        btnDbRefresh = new JButton("Yenile");
        btnDbRefresh.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnDbRefresh.addActionListener(e -> loadDatabaseTab());

        btnDbEdit = new JButton("Düzenle");
        btnDbEdit.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnDbEdit.setEnabled(false);
        btnDbEdit.addActionListener(e -> openDbEditDialog());

        btnDbGitPush = new JButton("GitHub'a Yükle");
        btnDbGitPush.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnDbGitPush.setEnabled(false);
        btnDbGitPush.addActionListener(e -> openDbGitDialog());

        btnDbDelete = new JButton("Veritabanından Sil");
        btnDbDelete.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnDbDelete.setForeground(new Color(220, 60, 60));
        btnDbDelete.setEnabled(false);
        btnDbDelete.addActionListener(e -> deleteSelectedDbProjects());

        bottomPanel.add(btnDbRefresh);
        bottomPanel.add(btnDbEdit);
        bottomPanel.add(btnDbGitPush);
        bottomPanel.add(btnDbDelete);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadDatabaseTab() {
        if (dbListModel == null) return;
        List<com.smartproject.db.ProjectRepository.ProjectEntry> saved = repository.getAllEntries();
        dbListModel.clear();
        for (com.smartproject.db.ProjectRepository.ProjectEntry entry : saved) {
            Project p = new Project(entry);
            dbListModel.addElement(p);
        }
        clearDbDetailsPanel();
    }

    private void clearDbDetailsPanel() {
        if (lblDbDetailName != null) {
            lblDbDetailName.setText("Proje: (Secilmedi)");
            lblDbDetailPath.setText("Yol: -");
            lblDbDetailSource.setText("Kaynak: -");
            lblDbDetailLangs.setText("Diller: -");
            lblDbDetailFileCount.setText("Dosya Sayisi: -");
            lblDbDetailDate.setText("Taranma Tarihi: -");
            lblDbDetailTags.setText("Etiketler: -");
            txtDbReadme.setText("");
            btnDbDelete.setEnabled(false);
            if (btnDbEdit != null) btnDbEdit.setEnabled(false);
            if (btnDbGitPush != null) btnDbGitPush.setEnabled(false);
        }
    }

    private void deleteSelectedDbProjects() {
        List<Project> selected = dbProjectJList.getSelectedValuesList();
        if (selected.isEmpty()) return;

        int confirm = JOptionPane.showConfirmDialog(this,
                selected.size() + " adet projeyi veritabanından silmek istediğinize emin misiniz?",
                "Veritabanından Sil", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        for (Project p : selected) {
            repository.deleteById(p.getId());
        }
        refreshDbInfo();
        loadDatabaseTab();
        log(selected.size() + " proje veritabanindan silindi.");
    }

    private class DbProjectSelectionListener implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting()) return;

            List<Project> selected = dbProjectJList.getSelectedValuesList();
            boolean hasOne = selected.size() == 1;
            boolean hasAny = !selected.isEmpty();

            btnDbDelete.setEnabled(hasAny);
            if (btnDbEdit != null) btnDbEdit.setEnabled(hasOne);
            if (btnDbGitPush != null) btnDbGitPush.setEnabled(hasOne);

            if (!hasOne) {
                if (!hasAny) {
                    clearDbDetailsPanel();
                } else {
                    lblDbDetailName.setText("Secili Proje Sayisi: " + selected.size());
                    lblDbDetailPath.setText("-");
                    lblDbDetailSource.setText("-");
                    lblDbDetailLangs.setText("-");
                    lblDbDetailFileCount.setText("-");
                    lblDbDetailDate.setText("-");
                    lblDbDetailTags.setText("-");
                    txtDbReadme.setText("");
                }
                return;
            }

            Project sel = selected.get(0);
            lblDbDetailName.setText("Proje: " + sel.getDisplayName());
            lblDbDetailPath.setText("Yol: " + sel.getAbsolutePath());
            lblDbDetailSource.setText("Kaynak: " + sel.getSource().name());
            lblDbDetailLangs.setText("Diller: " + String.join(", ", sel.getLanguagesUsed()));
            lblDbDetailFileCount.setText("Dosya Sayisi: " + sel.getSourceFiles().size());
            lblDbDetailDate.setText("Taranma: " + sel.getScanDate());
            lblDbDetailTags.setText("Etiketler: " + String.join(", ", sel.getTags()));

            try {
                File readmeFile = new File(sel.getSpmFolder(), "README.md");
                if (readmeFile.exists()) {
                    txtDbReadme.setText(new String(java.nio.file.Files.readAllBytes(readmeFile.toPath()), "utf-8"));
                } else {
                    txtDbReadme.setText("# " + sel.getDisplayName() + "\n\nHenuz analiz edilmedi.");
                }
                txtDbReadme.setCaretPosition(0);
            } catch (Exception ex) {
                txtDbReadme.setText("README yuklenirken hata: " + ex.getMessage());
            }
        }
    }

    private void refreshProfilesCombo() {
        if (cbProfiles == null) return;
        cbProfiles.removeAllItems();
        java.util.List<String> list = config.getProfileNames();
        for (String p : list) {
            cbProfiles.addItem(p);
        }
    }

    private void loadProfile(String profileName) {
        if (config.loadProfile(profileName)) {
            loadConfig();
            if (assistantPanel != null) {
                assistantPanel.refreshActiveModel();
            }
            log("Profil yuklendi: " + profileName);
            JOptionPane.showMessageDialog(this, "Profil '" + profileName + "' basariyla yuklendi.", "Basarili", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Profil yuklenemedi!", "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }
}
