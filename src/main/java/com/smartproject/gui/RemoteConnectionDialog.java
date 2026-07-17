package com.smartproject.gui;

import com.smartproject.config.ConfigManager;
import com.smartproject.db.ProjectRepository;
import com.smartproject.model.Project;
import com.smartproject.scanner.DockerScanner;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * Uzak sunucu baglanti diyalogu.
 * Uc sekme:
 *   1) SSH Sunucu     : VDS'ye SSH ile baglanip dizin gez, proje tara
 *   2) Docker (SSH)   : SSH ile VDS'ye baglan, container listele, ic tara
 *   3) Terminal (SSH) : Komut gir/calistir, hazir Docker butonlari
 */
public class RemoteConnectionDialog extends JDialog {

    // Renk paleti
    private static final Color CLR_BG        = new Color(30, 32, 40);
    private static final Color CLR_PANEL      = new Color(38, 41, 52);
    private static final Color CLR_CARD       = new Color(45, 49, 63);
    private static final Color CLR_BORDER     = new Color(60, 65, 82);
    private static final Color CLR_ACCENT     = new Color(99, 132, 255);
    private static final Color CLR_SUCCESS    = new Color(72, 199, 142);
    private static final Color CLR_WARNING    = new Color(255, 183, 77);
    private static final Color CLR_TEXT       = new Color(220, 225, 240);
    private static final Color CLR_TEXT_MUTED = new Color(130, 140, 165);
    private static final Color CLR_DIR        = new Color(150, 190, 255);
    private static final Color CLR_FILE       = new Color(200, 205, 220);
    private static final Color CLR_HINT_MAVEN = new Color(255, 120, 80);
    private static final Color CLR_HINT_NODE  = new Color(100, 210, 140);
    private static final Color CLR_HINT_PY    = new Color(80, 170, 255);
    private static final Color CLR_HINT_GIT   = new Color(240, 100, 100);
    private static final Color CLR_HINT_DOCK  = new Color(100, 180, 255);

    private final DockerScanner dockerScanner;
    private final DefaultListModel<Project> parentListModel;
    private final ProjectRepository repository;
    private final ConfigManager config;

    // --- Ortak SSH Kimlik Bilgileri ---
    private JTextField txtHost;
    private JTextField txtPort;
    private JTextField txtUser;
    private JPasswordField txtPass;
    private JTextField txtPemPath;
    private JRadioButton rbPassword;
    private JRadioButton rbPemKey;
    private JCheckBox chkSudo;
    private JButton btnConnect;
    private JLabel lblConnStatus;
    private static boolean isConnected = false;
    /** Aktif bir baglanma islemi varken yeni denemeyi engeller (rate limit korumasi) */
    private volatile boolean isConnecting = false;
    private static String staticHomeDir = "";
    private static String staticHost = "";
    private static String staticPort = "";
    private static String staticUser = "";
    private static String staticPass = "";
    private static String staticPemPath = "";
    private static boolean staticRbPassword = true;
    private static boolean staticRbPemKey = false;
    private static boolean staticUseSudo = false;

    // --- SSH Sekmesi ---
    private JTextField txtSshCurrentPath;
    private DefaultListModel<String> sshDirListModel;
    private JList<String> sshDirList;
    private List<Map<String, String>> sshCurrentEntries = new ArrayList<>();
    private static Deque<String> sshPathHistory = new ArrayDeque<>();
    private JButton btnSshBack;
    private JButton btnSshScanFolder;
    private JCheckBox chkSshDeep;
    private JToggleButton chkSshBrowseSudo;   // sudo ile dizin listele
    private JLabel lblSshStatus;
    private JLabel lblSshHint;

    // --- Docker Sekmesi ---
    private DefaultListModel<String> dockerContainerListModel;
    private JList<String> dockerContainerList;
    private List<Map<String, String>> dockerContainerData = new ArrayList<>();
    private JTextField txtDockerContainerPath;
    private DefaultListModel<String> dockerDirListModel;
    private JList<String> dockerDirList;
    private List<Map<String, String>> dockerDirEntries = new ArrayList<>();
    private static Deque<String> dockerPathHistory = new ArrayDeque<>();
    private JButton btnDockerLoadContainers;
    private JButton btnDockerBack;
    private JButton btnDockerBrowse;
    private JButton btnDockerScan;
    private JToggleButton chkDockerBrowseSudo; // sudo ile container dizin listele
    private JLabel lblDockerStatus;
    private JLabel lblDockerHint;
    private static String selectedContainerId   = null;
    private static String selectedContainerName = null;

    private static boolean staticSshBrowseSudo = false;
    private static boolean staticDockerBrowseSudo = false;
    private static String staticDockerContainerPath = "/app";

    // --- Terminal Sekmesi ---
    private JTextArea terminalOutput;
    private JTextField txtTerminalCommand;
    private JLabel lblTerminalPrompt;
    private JButton btnTerminalRun;
    private JToggleButton btnSudoMode;
    private JLabel lblCmdPromptIcon;
    private JTabbedPane mainTabs;

    public RemoteConnectionDialog(JFrame parent, DockerScanner dockerScanner,
                                   DefaultListModel<Project> parentListModel,
                                   ProjectRepository repository, ConfigManager config) {
        super(parent, "Uzak Baglanti", true);
        this.dockerScanner   = dockerScanner;
        this.parentListModel = parentListModel;
        this.repository      = repository;
        this.config          = config;

        setSize(860, 660);
        setMinimumSize(new Dimension(700, 540));
        setLocationRelativeTo(parent);
        buildUI();
        loadSavedConfig();
        restoreConnectionState();
    }

    // ====================================================
    // ANA UI KURULUMU
    // ====================================================

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(CLR_BG);

        // Ust: Kimlik Bilgileri Paneli
        root.add(buildCredentialsPanel(), BorderLayout.NORTH);

        // Orta: Sekmeler
        mainTabs = new JTabbedPane();
        mainTabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        mainTabs.setBackground(CLR_PANEL);
        mainTabs.setForeground(CLR_TEXT);
        mainTabs.addTab("  SSH Sunucu  ", buildSshPanel());
        mainTabs.addTab("  Docker (SSH)  ", buildDockerPanel());
        mainTabs.addTab("  Terminal  ", buildTerminalPanel());
        root.add(mainTabs, BorderLayout.CENTER);

        add(root);
    }

    // ====================================================
    // KIMLIK BILGILERI PANELI
    // ====================================================

    private JPanel buildCredentialsPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(CLR_PANEL);
        outer.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, CLR_BORDER));

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(CLR_PANEL);
        panel.setBorder(new EmptyBorder(12, 16, 12, 16));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        // Satir 0: Baslik
        g.gridx = 0; g.gridy = 0; g.gridwidth = 6;
        JLabel title = new JLabel("  Sunucu Baglanti Bilgileri");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(CLR_ACCENT);
        title.setIcon(makeTextIcon("🔌"));
        panel.add(title, g);

        g.gridwidth = 1;

        // Satir 1: Host | Port | User
        g.gridx = 0; g.gridy = 1; g.weightx = 0;
        panel.add(makeLabel("Sunucu (IP/Host):"), g);
        g.gridx = 1; g.weightx = 0.35;
        txtHost = makeTextField(!staticHost.isEmpty() ? staticHost : config.getSshHost(), "192.168.1.100");
        panel.add(txtHost, g);
 
        g.gridx = 2; g.weightx = 0;
        panel.add(makeLabel("Port:"), g);
        g.gridx = 3; g.weightx = 0.08;
        txtPort = makeTextField(!staticPort.isEmpty() ? staticPort : (config.getSshPort().isEmpty() ? "22" : config.getSshPort()), "22");
        txtPort.setPreferredSize(new Dimension(55, 28));
        panel.add(txtPort, g);
 
        g.gridx = 4; g.weightx = 0;
        panel.add(makeLabel("Kullanici:"), g);
        g.gridx = 5; g.weightx = 0.25;
        txtUser = makeTextField(!staticUser.isEmpty() ? staticUser : config.getSshUser(), "root");
        panel.add(txtUser, g);
 
        // Satir 2: Kimlik Tipi + Deger
        g.gridx = 0; g.gridy = 2; g.weightx = 0;
        panel.add(makeLabel("Kimlik:"), g);
 
        JPanel authPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        authPanel.setBackground(CLR_PANEL);
        rbPassword = new JRadioButton("Sifre");
        rbPemKey   = new JRadioButton("Private Key (.pem)");
        styleRadio(rbPassword);
        styleRadio(rbPemKey);
        ButtonGroup authGroup = new ButtonGroup();
        authGroup.add(rbPassword);
        authGroup.add(rbPemKey);
 
        boolean usePem = !staticHost.isEmpty() ? staticRbPemKey : !config.getSshPemPath().isEmpty();
        rbPassword.setSelected(!usePem);
        rbPemKey.setSelected(usePem);
 
        authPanel.add(rbPassword);
        authPanel.add(rbPemKey);
        g.gridx = 1; g.gridwidth = 2; g.weightx = 0.3;
        panel.add(authPanel, g);
        g.gridwidth = 1;
 
        // Sifre alani
        g.gridx = 3; g.gridy = 2; g.weightx = 0;
        panel.add(makeLabel("Sifre / Key:"), g);
        g.gridx = 4; g.weightx = 0.3; g.gridwidth = 1;
        txtPass = new JPasswordField(staticPass != null ? staticPass : "");
        styleTextField(txtPass);
        panel.add(txtPass, g);
 
        // PEM yolu alani (sifre yerine)
        g.gridx = 5; g.weightx = 0.25; g.gridwidth = 1;
        txtPemPath = makeTextField(!staticHost.isEmpty() ? staticPemPath : config.getSshPemPath(), "/path/to/key.pem");
        JButton btnBrowsePem = makeSmallButton("...");
        btnBrowsePem.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle(".pem / private key dosyasi sec");
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                txtPemPath.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        JPanel pemPanel = new JPanel(new BorderLayout(3, 0));
        pemPanel.setBackground(CLR_PANEL);
        pemPanel.add(txtPemPath, BorderLayout.CENTER);
        pemPanel.add(btnBrowsePem, BorderLayout.EAST);
        panel.add(pemPanel, g);
 
        // Auth tipi degisince PEM/Sifre alanini goster/gizle
        ActionListener authToggle = e -> {
            boolean isPem = rbPemKey.isSelected();
            txtPass.setEnabled(!isPem);
            txtPemPath.setEnabled(isPem);
            btnBrowsePem.setEnabled(isPem);
        };
        rbPassword.addActionListener(authToggle);
        rbPemKey.addActionListener(authToggle);
        authToggle.actionPerformed(null); // baslangic durumu
 
        // Satir 3: Sudo + Baglan + Durum
        g.gridx = 0; g.gridy = 3; g.weightx = 0; g.gridwidth = 1;
        chkSudo = new JCheckBox("Sudo Kullan");
        chkSudo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        chkSudo.setForeground(CLR_TEXT_MUTED);
        chkSudo.setBackground(CLR_PANEL);
        chkSudo.setSelected(!staticHost.isEmpty() ? staticUseSudo : config.getSshUseSudo());
        panel.add(chkSudo, g);

        g.gridx = 1; g.weightx = 0.2; g.gridwidth = 2;
        btnConnect = new JButton("  Baglan  ");
        btnConnect.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnConnect.setBackground(CLR_ACCENT);
        btnConnect.setForeground(Color.WHITE);
        btnConnect.setFocusPainted(false);
        btnConnect.addActionListener(e -> connectToServer());
        panel.add(btnConnect, g);

        g.gridx = 3; g.weightx = 0.5; g.gridwidth = 3;
        lblConnStatus = new JLabel("Baglanmadi.");
        lblConnStatus.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblConnStatus.setForeground(CLR_TEXT_MUTED);
        panel.add(lblConnStatus, g);

        outer.add(panel, BorderLayout.CENTER);
        return outer;
    }

    // ====================================================
    // SSH SEKMESI
    // ====================================================

    private JPanel buildSshPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(CLR_BG);
        panel.setBorder(new EmptyBorder(10, 12, 10, 12));

        // Ust: Adres cubugu
        JPanel addrPanel = new JPanel(new BorderLayout(6, 0));
        addrPanel.setBackground(CLR_BG);

        btnSshBack = makeSmallButton("< Geri");
        btnSshBack.setEnabled(false);
        btnSshBack.addActionListener(e -> navigateSshBack());

        txtSshCurrentPath = makeTextField("", "/home/user");
        txtSshCurrentPath.setEditable(false);
        txtSshCurrentPath.setBackground(CLR_CARD);
        txtSshCurrentPath.setForeground(CLR_DIR);
        txtSshCurrentPath.setFont(new Font("Consolas", Font.PLAIN, 13));

        JButton btnGo = makeSmallButton("Git >");
        btnGo.addActionListener(e -> {
            String path = txtSshCurrentPath.getText().trim();
            if (!path.isEmpty()) loadSshDirectory(path);
        });

        // Sudo toggle: root dizinlerine erisim icin
        chkSshBrowseSudo = makeSudoBrowseToggle();
        chkSshBrowseSudo.setSelected(staticSshBrowseSudo);
        chkSshBrowseSudo.addActionListener(e -> {
            boolean on = chkSshBrowseSudo.isSelected();
            staticSshBrowseSudo = on;
            // Mevcut dizini yeniden yukle
            String cur = txtSshCurrentPath.getText().trim();
            if (!cur.isEmpty() && isConnected) loadSshDirectory(cur);
        });

        // Sol taraf: Geri + Sudo toggle
        JPanel leftAddr = new JPanel(new BorderLayout(4, 0));
        leftAddr.setBackground(CLR_BG);
        leftAddr.add(btnSshBack, BorderLayout.WEST);
        leftAddr.add(chkSshBrowseSudo, BorderLayout.EAST);

        addrPanel.add(leftAddr, BorderLayout.WEST);
        addrPanel.add(txtSshCurrentPath, BorderLayout.CENTER);
        addrPanel.add(btnGo, BorderLayout.EAST);
        panel.add(addrPanel, BorderLayout.NORTH);

        // Orta: Dizin Listesi
        sshDirListModel = new DefaultListModel<>();
        sshDirList = new JList<>(sshDirListModel);
        sshDirList.setFont(new Font("Consolas", Font.PLAIN, 13));
        sshDirList.setBackground(CLR_CARD);
        sshDirList.setForeground(CLR_TEXT);
        sshDirList.setSelectionBackground(CLR_ACCENT.darker());
        sshDirList.setSelectionForeground(Color.WHITE);
        sshDirList.setFixedCellHeight(26);
        sshDirList.setCellRenderer(new RemoteEntryRenderer());

        sshDirList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) navigateSshInto();
            }
        });

        JScrollPane scroll = new JScrollPane(sshDirList);
        scroll.setBorder(BorderFactory.createLineBorder(CLR_BORDER));
        scroll.getViewport().setBackground(CLR_CARD);
        panel.add(scroll, BorderLayout.CENTER);

        // Alt: Ipucu + Tarama secenekleri + Buton
        JPanel bottomPanel = new JPanel(new BorderLayout(8, 6));
        bottomPanel.setBackground(CLR_BG);

        lblSshHint = new JLabel(" ");
        lblSshHint.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblSshHint.setForeground(CLR_SUCCESS);

        lblSshStatus = new JLabel("Baglanin, ardindan dizin gezebilirsiniz.");
        lblSshStatus.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblSshStatus.setForeground(CLR_TEXT_MUTED);

        JPanel hints = new JPanel(new BorderLayout());
        hints.setBackground(CLR_BG);
        hints.add(lblSshHint, BorderLayout.NORTH);
        hints.add(lblSshStatus, BorderLayout.SOUTH);
        bottomPanel.add(hints, BorderLayout.NORTH);

        JPanel optPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        optPanel.setBackground(CLR_BG);
        chkSshDeep = new JCheckBox("Alt klasorlerdeki projeleri de bul (derin tarama)");
        chkSshDeep.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        chkSshDeep.setForeground(CLR_TEXT_MUTED);
        chkSshDeep.setBackground(CLR_BG);
        chkSshDeep.setSelected(true);
        optPanel.add(chkSshDeep);
        bottomPanel.add(optPanel, BorderLayout.CENTER);

        btnSshScanFolder = new JButton("  Bu Klasoru Tara  ");
        btnSshScanFolder.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSshScanFolder.setBackground(CLR_SUCCESS);
        btnSshScanFolder.setForeground(Color.WHITE);
        btnSshScanFolder.setFocusPainted(false);
        btnSshScanFolder.setEnabled(false);
        btnSshScanFolder.addActionListener(e -> scanSshFolder());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(CLR_BG);
        JButton btnClose = makeSmallButton("Kapat");
        btnClose.addActionListener(e -> dispose());
        btnPanel.add(btnClose);
        btnPanel.add(btnSshScanFolder);
        bottomPanel.add(btnPanel, BorderLayout.SOUTH);

        panel.add(bottomPanel, BorderLayout.SOUTH);
        return panel;
    }

    // ====================================================
    // DOCKER SEKMESI
    // ====================================================

    private JPanel buildDockerPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(CLR_BG);
        panel.setBorder(new EmptyBorder(10, 12, 10, 12));

        // Sol: Container Listesi
        JPanel leftPanel = new JPanel(new BorderLayout(4, 6));
        leftPanel.setBackground(CLR_BG);
        leftPanel.setPreferredSize(new Dimension(240, 0));

        JLabel lblContainers = new JLabel("  Container'lar");
        lblContainers.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblContainers.setForeground(CLR_ACCENT);

        dockerContainerListModel = new DefaultListModel<>();
        dockerContainerList = new JList<>(dockerContainerListModel);
        dockerContainerList.setFont(new Font("Consolas", Font.PLAIN, 12));
        dockerContainerList.setBackground(CLR_CARD);
        dockerContainerList.setForeground(CLR_TEXT);
        dockerContainerList.setSelectionBackground(CLR_ACCENT.darker());
        dockerContainerList.setSelectionForeground(Color.WHITE);
        dockerContainerList.setFixedCellHeight(28);
        dockerContainerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        dockerContainerList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onContainerSelected();
        });

        btnDockerLoadContainers = new JButton("Container Listesini Getir");
        btnDockerLoadContainers.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnDockerLoadContainers.setEnabled(false);
        btnDockerLoadContainers.addActionListener(e -> loadDockerContainers());

        leftPanel.add(lblContainers, BorderLayout.NORTH);
        JScrollPane cScroll = new JScrollPane(dockerContainerList);
        cScroll.setBorder(BorderFactory.createLineBorder(CLR_BORDER));
        leftPanel.add(cScroll, BorderLayout.CENTER);
        leftPanel.add(btnDockerLoadContainers, BorderLayout.SOUTH);

        // Sag: Dizin Gezgini
        JPanel rightPanel = new JPanel(new BorderLayout(6, 6));
        rightPanel.setBackground(CLR_BG);

        // Adres cubugu
        JPanel addrPanel = new JPanel(new BorderLayout(4, 0));
        addrPanel.setBackground(CLR_BG);

        btnDockerBack = makeSmallButton("< Geri");
        btnDockerBack.setEnabled(false);
        btnDockerBack.addActionListener(e -> navigateDockerBack());

        txtDockerContainerPath = makeTextField(!staticDockerContainerPath.isEmpty() ? staticDockerContainerPath : "/app", "/app");
        txtDockerContainerPath.setFont(new Font("Consolas", Font.PLAIN, 13));
        txtDockerContainerPath.setBackground(CLR_CARD);
        txtDockerContainerPath.setForeground(CLR_DIR);

        btnDockerBrowse = makeSmallButton("Gozat >");
        btnDockerBrowse.setEnabled(false);
        btnDockerBrowse.addActionListener(e -> {
            String path = txtDockerContainerPath.getText().trim();
            if (!path.isEmpty() && selectedContainerId != null) loadContainerDirectory(path);
        });

        chkDockerBrowseSudo = makeSudoBrowseToggle();
        chkDockerBrowseSudo.setSelected(staticDockerBrowseSudo);
        chkDockerBrowseSudo.addActionListener(e -> {
            boolean on = chkDockerBrowseSudo.isSelected();
            staticDockerBrowseSudo = on;
            String path = txtDockerContainerPath.getText().trim();
            if (!path.isEmpty() && selectedContainerId != null && isConnected)
                loadContainerDirectory(path);
        });

        JPanel leftDockerAddr = new JPanel(new BorderLayout(4, 0));
        leftDockerAddr.setBackground(CLR_BG);
        leftDockerAddr.add(btnDockerBack, BorderLayout.WEST);
        leftDockerAddr.add(chkDockerBrowseSudo, BorderLayout.EAST);

        addrPanel.add(leftDockerAddr, BorderLayout.WEST);
        addrPanel.add(txtDockerContainerPath, BorderLayout.CENTER);
        addrPanel.add(btnDockerBrowse, BorderLayout.EAST);

        // Dizin listesi
        dockerDirListModel = new DefaultListModel<>();
        dockerDirList = new JList<>(dockerDirListModel);
        dockerDirList.setFont(new Font("Consolas", Font.PLAIN, 13));
        dockerDirList.setBackground(CLR_CARD);
        dockerDirList.setForeground(CLR_TEXT);
        dockerDirList.setSelectionBackground(CLR_ACCENT.darker());
        dockerDirList.setSelectionForeground(Color.WHITE);
        dockerDirList.setFixedCellHeight(26);
        dockerDirList.setCellRenderer(new RemoteEntryRenderer());
        dockerDirList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) navigateDockerInto();
            }
        });

        JScrollPane dScroll = new JScrollPane(dockerDirList);
        dScroll.setBorder(BorderFactory.createLineBorder(CLR_BORDER));
        dScroll.getViewport().setBackground(CLR_CARD);

        rightPanel.add(addrPanel, BorderLayout.NORTH);
        rightPanel.add(dScroll, BorderLayout.CENTER);

        // Alt: Ipucu + Buton
        JPanel btmPanel = new JPanel(new BorderLayout(6, 4));
        btmPanel.setBackground(CLR_BG);

        lblDockerHint = new JLabel(" ");
        lblDockerHint.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblDockerHint.setForeground(CLR_SUCCESS);

        lblDockerStatus = new JLabel("Container secin, ardından icini gezin ve tarayin.");
        lblDockerStatus.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblDockerStatus.setForeground(CLR_TEXT_MUTED);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(CLR_BG);
        statusPanel.add(lblDockerHint,  BorderLayout.NORTH);
        statusPanel.add(lblDockerStatus, BorderLayout.SOUTH);
        btmPanel.add(statusPanel, BorderLayout.CENTER);

        btnDockerScan = new JButton("  Bu Dizini Tara  ");
        btnDockerScan.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnDockerScan.setBackground(CLR_SUCCESS);
        btnDockerScan.setForeground(Color.WHITE);
        btnDockerScan.setFocusPainted(false);
        btnDockerScan.setEnabled(false);
        btnDockerScan.addActionListener(e -> scanDockerFolder());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(CLR_BG);
        JButton btnClose = makeSmallButton("Kapat");
        btnClose.addActionListener(ev -> dispose());
        btnPanel.add(btnClose);
        btnPanel.add(btnDockerScan);
        btmPanel.add(btnPanel, BorderLayout.SOUTH);

        rightPanel.add(btmPanel, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        split.setDividerLocation(240);
        split.setDividerSize(5);
        split.setBackground(CLR_BG);
        panel.add(split, BorderLayout.CENTER);

        return panel;
    }

    // ====================================================
    // BAGLANTI ISLEMLERI
    // ====================================================

    private void connectToServer() {
        // Debounce: Aktif bir baglanma islemi varken yeni deneme baslatma
        if (isConnecting) {
            lblConnStatus.setText("Zaten baglaniliyor, lutfen bekleyin...");
            lblConnStatus.setForeground(CLR_WARNING);
            return;
        }

        String host = txtHost.getText().trim();
        String portStr = txtPort.getText().trim();
        String user = txtUser.getText().trim();

        if (host.isEmpty() || user.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Sunucu adresi ve kullanici adi zorunludur.", "Eksik Bilgi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int port = 22;
        try { port = Integer.parseInt(portStr); } catch (Exception ex) {}

        String pass   = rbPassword.isSelected() ? new String(txtPass.getPassword()) : null;
        String pemPath = getPemPath();
        boolean useSudo = chkSudo.isSelected();

        isConnecting = true;
        btnConnect.setEnabled(false);
        lblConnStatus.setText("Baglaniliyor...");
        lblConnStatus.setForeground(CLR_WARNING);

        final int finalPort = port;
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                return dockerScanner.testSshConnection(host, finalPort, user, pass, pemPath);
            }
            @Override protected void done() {
                try {
                    String homeDir = get();
                    isConnected = true;
                    staticHomeDir = homeDir;
                    staticHost = host;
                    staticPort = String.valueOf(finalPort);
                    staticUser = user;
                    staticPass = pass != null ? pass : "";
                    staticPemPath = pemPath != null ? pemPath : "";
                    staticRbPassword = rbPassword.isSelected();
                    staticRbPemKey = rbPemKey.isSelected();
                    staticUseSudo = useSudo;
                    lblConnStatus.setText("Baglandi!  Ev dizini: " + homeDir);
                    lblConnStatus.setForeground(CLR_SUCCESS);

                    // SSH sekmesi: ev dizininden baslat
                    String startPath = config.getSshLastPath().isEmpty() ? homeDir : config.getSshLastPath();
                    txtSshCurrentPath.setEditable(true);
                    loadSshDirectory(startPath);

                    // Docker sekmesi: container listesi butonu aktif ve otomatik yukle
                    btnDockerLoadContainers.setEnabled(true);
                    loadDockerContainers();

                    // Terminal sekmesi: prompt guncelle, aktif et
                    String prompt = user + "@" + host + ":~$ ";
                    lblTerminalPrompt.setText(prompt);
                    txtTerminalCommand.setEnabled(true);
                    btnTerminalRun.setEnabled(true);
                    terminalOutput.setText("");
                    terminalOutput.append("Baglanti basarili: " + user + "@" + host + "\n");
                    terminalOutput.append("Ev dizini: " + homeDir + "\n");
                    terminalOutput.append("─".repeat(60) + "\n");
                    // Terminal sekmesini gostermek icin tab basligini vurgula
                    mainTabs.setForegroundAt(2, CLR_SUCCESS);

                    // Config kaydet
                    config.setSshHost(host);
                    config.setSshPort(String.valueOf(finalPort));
                    config.setSshUser(user);
                    config.setSshUseSudo(useSudo);
                    config.setSshPemPath(pemPath != null ? pemPath : "");
                    config.save();

                } catch (Exception ex) {
                    isConnected = false;
                    lblConnStatus.setText("Baglanti hatasi: " + getCleanMessage(ex));
                    lblConnStatus.setForeground(CLR_HINT_GIT);
                } finally {
                    isConnecting = false;
                    btnConnect.setEnabled(true);
                }
            }
        }.execute();
    }

    // ====================================================
    // SSH DIZIN GEZGINI
    // ====================================================

    private void loadSshDirectory(String path) {
        if (!isConnected) return;

        String host   = txtHost.getText().trim();
        int port      = parsePort();
        String user   = txtUser.getText().trim();
        String pass   = rbPassword.isSelected() ? new String(txtPass.getPassword()) : null;
        String pemPath = getPemPath();
        boolean sudo  = chkSshBrowseSudo != null && chkSshBrowseSudo.isSelected();

        lblSshStatus.setText("Dizin yukleniyor: " + path);
        lblSshStatus.setForeground(CLR_WARNING);
        sshDirListModel.clear();
        btnSshScanFolder.setEnabled(false);

        new SwingWorker<List<Map<String, String>>, Void>() {
            @Override protected List<Map<String, String>> doInBackground() throws Exception {
                return dockerScanner.listSshDirectory(host, port, user, pass, pemPath, path, sudo);
            }
            @Override protected void done() {
                try {
                    sshCurrentEntries = get();
                    String hint = "";
                    for (Map<String, String> e : sshCurrentEntries) {
                        if (e.containsKey("currentDirHint") && !e.get("currentDirHint").isEmpty()) {
                            hint = e.get("currentDirHint");
                            break;
                        }
                    }
                    updateSshHintLabel(hint);

                    sshDirListModel.clear();
                    for (Map<String, String> e : sshCurrentEntries) {
                        boolean isDir = "true".equals(e.get("isDir"));
                        String icon = isDir ? "📁 " : "📄 ";
                        sshDirListModel.addElement(icon + e.get("name"));
                    }

                    txtSshCurrentPath.setText(path);
                    btnSshBack.setEnabled(!sshPathHistory.isEmpty());
                    btnSshScanFolder.setEnabled(true);

                    lblSshStatus.setText(sshCurrentEntries.size() + " oge listelendi."
                            + (chkSshBrowseSudo.isSelected() ? "  [sudo modu]" : ""));
                    lblSshStatus.setForeground(CLR_TEXT_MUTED);

                    config.setSshLastPath(path);
                    config.save();

                } catch (Exception ex) {
                    lblSshStatus.setText("Hata: " + getCleanMessage(ex));
                    lblSshStatus.setForeground(CLR_HINT_GIT);
                }
            }
        }.execute();
    }

    private void navigateSshInto() {
        int idx = sshDirList.getSelectedIndex();
        if (idx < 0 || idx >= sshCurrentEntries.size()) return;
        Map<String, String> entry = sshCurrentEntries.get(idx);
        if (!"true".equals(entry.get("isDir"))) return;

        String currentPath = txtSshCurrentPath.getText().trim();
        sshPathHistory.push(currentPath);
        loadSshDirectory(entry.get("path"));
    }

    private void navigateSshBack() {
        if (sshPathHistory.isEmpty()) return;
        String prev = sshPathHistory.pop();
        loadSshDirectory(prev);
    }

    private void scanSshFolder() {
        String path = txtSshCurrentPath.getText().trim();
        if (path.isEmpty() || !isConnected) return;

        String host   = txtHost.getText().trim();
        int port      = parsePort();
        String user   = txtUser.getText().trim();
        String pass   = rbPassword.isSelected() ? new String(txtPass.getPassword()) : null;
        String pemPath = getPemPath();
        boolean sudo  = chkSudo.isSelected();

        lblSshStatus.setText("Tarama baslatildi: " + path);
        lblSshStatus.setForeground(CLR_WARNING);
        btnSshScanFolder.setEnabled(false);

        new SwingWorker<List<Project>, Void>() {
            @Override protected List<Project> doInBackground() throws Exception {
                return dockerScanner.scanRemoteFolderSsh(host, port, user, pass, pemPath, path, sudo);
            }
            @Override protected void done() {
                try {
                    List<Project> projects = get();
                    for (Project p : projects) {
                        parentListModel.addElement(p);
                        repository.saveProject(p);
                    }
                    lblSshStatus.setText("Tamamlandi! " + projects.size() + " proje eklendi.");
                    lblSshStatus.setForeground(CLR_SUCCESS);
                    if (projects.isEmpty()) {
                        JOptionPane.showMessageDialog(RemoteConnectionDialog.this,
                                "Bu dizinde proje bulunamadi.\nFarkli bir klasor deneyin.", "Sonuc", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(RemoteConnectionDialog.this,
                                projects.size() + " proje basariyla eklendi.", "Basarili", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    lblSshStatus.setText("Hata: " + getCleanMessage(ex));
                    lblSshStatus.setForeground(CLR_HINT_GIT);
                    JOptionPane.showMessageDialog(RemoteConnectionDialog.this,
                            "Hata: " + getCleanMessage(ex), "Hata", JOptionPane.ERROR_MESSAGE);
                } finally {
                    btnSshScanFolder.setEnabled(true);
                }
            }
        }.execute();
    }

    // ====================================================
    // DOCKER CONTAINER ISLEMLERI
    // ====================================================

    private void loadDockerContainers() {
        if (!isConnected) return;

        String host   = txtHost.getText().trim();
        int port      = parsePort();
        String user   = txtUser.getText().trim();
        String pass   = rbPassword.isSelected() ? new String(txtPass.getPassword()) : null;
        String pemPath = getPemPath();
        boolean sudo  = chkSudo.isSelected();

        lblDockerStatus.setText("Container listesi aliniyor...");
        lblDockerStatus.setForeground(CLR_WARNING);
        dockerContainerListModel.clear();
        dockerContainerData.clear();
        btnDockerLoadContainers.setEnabled(false);

        new SwingWorker<List<Map<String, String>>, Void>() {
            @Override protected List<Map<String, String>> doInBackground() throws Exception {
                return dockerScanner.listContainersSsh(host, port, user, pass, pemPath, sudo);
            }
            @Override protected void done() {
                try {
                    dockerContainerData = get();
                    if (dockerContainerData.isEmpty()) {
                        lblDockerStatus.setText("Calisma container'i bulunamadi.");
                        lblDockerStatus.setForeground(CLR_HINT_GIT);
                    } else {
                        for (Map<String, String> c : dockerContainerData) {
                            dockerContainerListModel.addElement(
                                "  " + c.get("name") + "  (" + c.get("image") + ")");
                        }
                        lblDockerStatus.setText(dockerContainerData.size() + " container bulundu.");
                        lblDockerStatus.setForeground(CLR_TEXT_MUTED);

                        // Reselect previously selected container
                        if (selectedContainerId != null) {
                            int toSelect = -1;
                            for (int i = 0; i < dockerContainerData.size(); i++) {
                                if (selectedContainerId.equals(dockerContainerData.get(i).get("id"))) {
                                    toSelect = i;
                                    break;
                                }
                            }
                            if (toSelect >= 0) {
                                dockerContainerList.setSelectedIndex(toSelect);
                            } else {
                                selectedContainerId = null;
                                selectedContainerName = null;
                            }
                        }
                    }
                } catch (Exception ex) {
                    lblDockerStatus.setText("Hata: " + getCleanMessage(ex));
                    lblDockerStatus.setForeground(CLR_HINT_GIT);
                } finally {
                    btnDockerLoadContainers.setEnabled(true);
                }
            }
        }.execute();
    }

    private void onContainerSelected() {
        int idx = dockerContainerList.getSelectedIndex();
        if (idx < 0 || idx >= dockerContainerData.size()) return;
        Map<String, String> container = dockerContainerData.get(idx);
        selectedContainerId   = container.get("id");
        selectedContainerName = container.get("name");
        btnDockerBrowse.setEnabled(true);
        btnDockerScan.setEnabled(false);
        dockerDirListModel.clear();
        dockerPathHistory.clear();
        
        String path = txtDockerContainerPath.getText().trim();
        if (path.isEmpty()) {
            path = "/";
            txtDockerContainerPath.setText("/");
        }
        loadContainerDirectory(path);
    }

    private void loadContainerDirectory(String containerPath) {
        if (!isConnected || selectedContainerId == null) return;
        staticDockerContainerPath = containerPath;

        String host   = txtHost.getText().trim();
        int port      = parsePort();
        String user   = txtUser.getText().trim();
        String pass   = rbPassword.isSelected() ? new String(txtPass.getPassword()) : null;
        String pemPath = getPemPath();
        boolean sudo  = chkDockerBrowseSudo != null && chkDockerBrowseSudo.isSelected();

        lblDockerStatus.setText("Dizin yukleniyor: " + containerPath);
        lblDockerStatus.setForeground(CLR_WARNING);
        dockerDirListModel.clear();
        btnDockerScan.setEnabled(false);

        new SwingWorker<List<Map<String, String>>, Void>() {
            @Override protected List<Map<String, String>> doInBackground() throws Exception {
                return dockerScanner.listContainerDirectory(
                        host, port, user, pass, pemPath, sudo, selectedContainerId, containerPath);
            }
            @Override protected void done() {
                try {
                    dockerDirEntries = get();
                    String hint = "";
                    for (Map<String, String> e : dockerDirEntries) {
                        if (e.containsKey("currentDirHint") && !e.get("currentDirHint").isEmpty()) {
                            hint = e.get("currentDirHint");
                            break;
                        }
                    }
                    updateDockerHintLabel(hint);

                    dockerDirListModel.clear();
                    for (Map<String, String> e : dockerDirEntries) {
                        boolean isDir = "true".equals(e.get("isDir"));
                        dockerDirListModel.addElement((isDir ? "📁 " : "📄 ") + e.get("name"));
                    }

                    txtDockerContainerPath.setText(containerPath);
                    btnDockerBack.setEnabled(!dockerPathHistory.isEmpty());
                    btnDockerScan.setEnabled(true);

                    lblDockerStatus.setText(dockerDirEntries.size() + " oge listelendi. Cift tikla icine gir.");
                    lblDockerStatus.setForeground(CLR_TEXT_MUTED);
                } catch (Exception ex) {
                    lblDockerStatus.setText("Hata: " + getCleanMessage(ex));
                    lblDockerStatus.setForeground(CLR_HINT_GIT);
                }
            }
        }.execute();
    }

    private void navigateDockerInto() {
        int idx = dockerDirList.getSelectedIndex();
        if (idx < 0 || idx >= dockerDirEntries.size()) return;
        Map<String, String> entry = dockerDirEntries.get(idx);
        if (!"true".equals(entry.get("isDir"))) return;
        String currentPath = txtDockerContainerPath.getText().trim();
        dockerPathHistory.push(currentPath);
        loadContainerDirectory(entry.get("path"));
    }

    private void navigateDockerBack() {
        if (dockerPathHistory.isEmpty()) return;
        String prev = dockerPathHistory.pop();
        loadContainerDirectory(prev);
    }

    private void scanDockerFolder() {
        String containerPath = txtDockerContainerPath.getText().trim();
        if (containerPath.isEmpty() || selectedContainerId == null) return;

        String host   = txtHost.getText().trim();
        int port      = parsePort();
        String user   = txtUser.getText().trim();
        String pass   = rbPassword.isSelected() ? new String(txtPass.getPassword()) : null;
        String pemPath = getPemPath();
        boolean sudo  = chkSudo.isSelected();

        lblDockerStatus.setText("Tarama baslatildi: " + selectedContainerName + ":" + containerPath);
        lblDockerStatus.setForeground(CLR_WARNING);
        btnDockerScan.setEnabled(false);

        new SwingWorker<List<Project>, Void>() {
            @Override protected List<Project> doInBackground() throws Exception {
                return dockerScanner.scanContainerSsh(
                        host, port, user, pass, pemPath,
                        selectedContainerId, selectedContainerName,
                        containerPath, sudo);
            }
            @Override protected void done() {
                try {
                    List<Project> projects = get();
                    for (Project p : projects) {
                        parentListModel.addElement(p);
                        repository.saveProject(p);
                    }
                    lblDockerStatus.setText("Tamamlandi! " + projects.size() + " proje eklendi.");
                    lblDockerStatus.setForeground(CLR_SUCCESS);
                    if (projects.isEmpty()) {
                        JOptionPane.showMessageDialog(RemoteConnectionDialog.this,
                                "Bu dizinde proje bulunamadi.\nFarkli bir dizin deneyin.", "Sonuc", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(RemoteConnectionDialog.this,
                                projects.size() + " proje basariyla eklendi!", "Basarili", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    lblDockerStatus.setText("Hata: " + getCleanMessage(ex));
                    lblDockerStatus.setForeground(CLR_HINT_GIT);
                    JOptionPane.showMessageDialog(RemoteConnectionDialog.this,
                            "Hata: " + getCleanMessage(ex), "Hata", JOptionPane.ERROR_MESSAGE);
                } finally {
                    btnDockerScan.setEnabled(true);
                }
            }
        }.execute();
    }

    // ====================================================
    // TERMINAL PANEL
    // ====================================================

    private JPanel buildTerminalPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(new Color(18, 20, 26));
        panel.setBorder(new EmptyBorder(0, 0, 0, 0));

        // Ust: Baslık cubugu (user@host)
        JPanel topBar = new JPanel(new BorderLayout(8, 0));
        topBar.setBackground(new Color(28, 32, 42));
        topBar.setBorder(new EmptyBorder(6, 12, 6, 12));

        lblTerminalPrompt = new JLabel("[Baglanmadi]  --  Once baglan butonu ile sunucuya baglanin");
        lblTerminalPrompt.setFont(new Font("Consolas", Font.BOLD, 13));
        lblTerminalPrompt.setForeground(new Color(130, 220, 130));
        topBar.add(lblTerminalPrompt, BorderLayout.WEST);

        JButton btnClearTerm = new JButton("Temizle");
        btnClearTerm.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnClearTerm.setBackground(new Color(50, 55, 70));
        btnClearTerm.setForeground(CLR_TEXT_MUTED);
        btnClearTerm.setFocusPainted(false);
        btnClearTerm.setBorderPainted(false);
        btnClearTerm.addActionListener(e -> terminalOutput.setText(""));
        topBar.add(btnClearTerm, BorderLayout.EAST);
        panel.add(topBar, BorderLayout.NORTH);

        // Orta: Cikti alani
        terminalOutput = new JTextArea();
        terminalOutput.setFont(new Font("Consolas", Font.PLAIN, 13));
        terminalOutput.setBackground(new Color(18, 20, 26));
        terminalOutput.setForeground(new Color(200, 210, 230));
        terminalOutput.setCaretColor(new Color(200, 210, 230));
        terminalOutput.setEditable(false);
        terminalOutput.setLineWrap(true);
        terminalOutput.setWrapStyleWord(false);
        terminalOutput.setMargin(new Insets(8, 10, 8, 10));
        JScrollPane outScroll = new JScrollPane(terminalOutput);
        outScroll.setBorder(BorderFactory.createEmptyBorder());
        outScroll.getViewport().setBackground(new Color(18, 20, 26));
        panel.add(outScroll, BorderLayout.CENTER);

        // Alt: Hazir butonlar + Komut satiri
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 4));
        bottomPanel.setBackground(new Color(22, 25, 34));
        bottomPanel.setBorder(new EmptyBorder(6, 10, 8, 10));

        // Hazir islem butonlari
        JPanel quickPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        quickPanel.setBackground(new Color(22, 25, 34));

        JLabel quickLbl = new JLabel("Hizli:");
        quickLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        quickLbl.setForeground(CLR_TEXT_MUTED);
        quickPanel.add(quickLbl);

        // Docker komutlari (sudo ile - seyfo docker grubunda olmayabilir)
        addQuickBtn(quickPanel, "docker ps",        "sudo docker ps");
        addQuickBtn(quickPanel, "docker images",    "sudo docker images");
        addQuickBtn(quickPanel, "docker stats",     "sudo docker stats --no-stream");
        addQuickBtn(quickPanel, "docker volume ls", "sudo docker volume ls");
        addQuickBtn(quickPanel, "docker logs",      "sudo docker ps -q | head -1 | xargs sudo docker logs --tail 30 2>&1");

        // Ayirici
        JLabel sep1 = new JLabel(" | ");
        sep1.setForeground(CLR_BORDER);
        quickPanel.add(sep1);

        // Sistem komutlari
        addQuickBtn(quickPanel, "df -h",    "df -h");
        addQuickBtn(quickPanel, "free -m",  "free -m");
        addQuickBtn(quickPanel, "uptime",   "uptime");
        addQuickBtn(quickPanel, "whoami",   "whoami");
        addQuickBtn(quickPanel, "ls -la",   "ls -la ~");
        addQuickBtn(quickPanel, "ps aux",   "ps aux --sort=-%cpu | head -15");
        addQuickBtn(quickPanel, "netstat",  "netstat -tulpn 2>/dev/null || ss -tulpn");

        // Ayirici
        JLabel sep2 = new JLabel(" | ");
        sep2.setForeground(CLR_BORDER);
        quickPanel.add(sep2);

        // Root / yetki komutlari
        addQuickBtn(quickPanel, "sudo whoami",     "sudo whoami");          // root doner → root erisimi var demek
        addQuickBtn(quickPanel, "id",              "id");                   // uid/gid bilgisi
        addQuickBtn(quickPanel, "sudo id",         "sudo id");              // root olarak uid=0 goster

        bottomPanel.add(quickPanel, BorderLayout.NORTH);

        // Komut girisi
        JPanel cmdPanel = new JPanel(new BorderLayout(6, 0));
        cmdPanel.setBackground(new Color(22, 25, 34));
        cmdPanel.setBorder(new EmptyBorder(4, 0, 0, 0));

        // Komut satiri sol kisim: Sudo toggle + prompt ikonu
        JPanel leftCmdPanel = new JPanel(new BorderLayout(4, 0));
        leftCmdPanel.setBackground(new Color(22, 25, 34));

        // Sudo modu toggle butonu
        btnSudoMode = new JToggleButton("sudo");
        btnSudoMode.setFont(new Font("Consolas", Font.BOLD, 12));
        btnSudoMode.setBackground(new Color(40, 45, 60));
        btnSudoMode.setForeground(new Color(255, 160, 80));
        btnSudoMode.setFocusPainted(false);
        btnSudoMode.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 90, 60)),
                new EmptyBorder(4, 8, 4, 8)));
        btnSudoMode.setToolTipText("<html><b>Sudo Modu Nasil Calisir?</b><br><br>" +
                "SSH exec her komut icin AYRI bir oturum acar.<br>" +
                "Bu yuzden 'sudo -i' ile root shell acilamiyor.<br><br>" +
                "<b>Sudo modu ACIK iken:</b><br>" +
                "Her komut 'sudo -S &lt;komut&gt;' olarak calistirilir.<br>" +
                "Yani komut ZATEN root olarak calisir!<br><br>" +
                "Ornek: 'docker ps' → aslinda 'sudo -S docker ps'<br>" +
                "Test: 'sudo whoami' → 'root' donmeli</html>");
        btnSudoMode.addActionListener(e -> {
            boolean isSudo = btnSudoMode.isSelected();
            btnSudoMode.setBackground(isSudo ? new Color(180, 60, 30) : new Color(40, 45, 60));
            btnSudoMode.setForeground(isSudo ? Color.WHITE : new Color(255, 160, 80));
            btnSudoMode.setText(isSudo ? "# ROOT" : "sudo");
            lblCmdPromptIcon.setText(isSudo ? "#" : "$");
            lblCmdPromptIcon.setForeground(isSudo ? new Color(255, 80, 80) : new Color(100, 210, 130));
            if (isSudo) {
                terminalOutput.append("\n[>>> SUDO MODU ACIK <<<]\n");
                terminalOutput.append("Her komut 'sudo -S <komut>' olarak calistirilir.\n");
                terminalOutput.append("Yani komutlar root olarak execute edilir.\n");
                terminalOutput.append("Test: 'sudo whoami' butonuna basin → 'root' gormeli.\n");
                terminalOutput.append("Not: 'sudo -i' veya 'su -' CALISMIYOR (interaktif shell gerektirir).\n");
                terminalOutput.setCaretPosition(terminalOutput.getDocument().getLength());
            } else {
                terminalOutput.append("\n[sudo modu KAPALI]\n");
                terminalOutput.setCaretPosition(terminalOutput.getDocument().getLength());
            }
        });
        leftCmdPanel.add(btnSudoMode, BorderLayout.WEST);

        lblCmdPromptIcon = new JLabel("$");
        lblCmdPromptIcon.setFont(new Font("Consolas", Font.BOLD, 15));
        lblCmdPromptIcon.setForeground(new Color(100, 210, 130));
        lblCmdPromptIcon.setBorder(new EmptyBorder(0, 6, 0, 6));
        leftCmdPanel.add(lblCmdPromptIcon, BorderLayout.CENTER);
        cmdPanel.add(leftCmdPanel, BorderLayout.WEST);

        txtTerminalCommand = new JTextField();
        txtTerminalCommand.setFont(new Font("Consolas", Font.PLAIN, 13));
        txtTerminalCommand.setBackground(new Color(28, 32, 42));
        txtTerminalCommand.setForeground(new Color(220, 230, 255));
        txtTerminalCommand.setCaretColor(new Color(220, 230, 255));
        txtTerminalCommand.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 65, 85)),
                new EmptyBorder(5, 8, 5, 8)));
        txtTerminalCommand.setEnabled(false);
        txtTerminalCommand.addActionListener(e -> runTerminalCommand());
        cmdPanel.add(txtTerminalCommand, BorderLayout.CENTER);

        btnTerminalRun = new JButton("Calistir ▶");
        btnTerminalRun.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnTerminalRun.setBackground(CLR_ACCENT);
        btnTerminalRun.setForeground(Color.WHITE);
        btnTerminalRun.setFocusPainted(false);
        btnTerminalRun.setBorderPainted(false);
        btnTerminalRun.setEnabled(false);
        btnTerminalRun.addActionListener(e -> runTerminalCommand());
        cmdPanel.add(btnTerminalRun, BorderLayout.EAST);

        bottomPanel.add(cmdPanel, BorderLayout.SOUTH);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    /** Hazir islem butonu olustur ve panele ekle */
    private void addQuickBtn(JPanel panel, String label, String command) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btn.setBackground(new Color(40, 45, 60));
        btn.setForeground(new Color(180, 200, 255));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 70, 95)),
                new EmptyBorder(3, 7, 3, 7)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(55, 70, 100)); }
            @Override public void mouseExited(MouseEvent e)  { btn.setBackground(new Color(40, 45, 60)); }
        });
        btn.addActionListener(e -> {
            if (!isConnected) {
                terminalOutput.append("[!] Once sunucuya baglanin.\n");
                return;
            }
            txtTerminalCommand.setText(command);
            runTerminalCommand();
        });
        panel.add(btn);
    }

    /** Komut satirindaki komutu SSH ile calistir, ciktisini terminale yaz */
    private void runTerminalCommand() {
        if (!isConnected) {
            terminalOutput.append("[!] Baglanmadi. Once 'Baglan' butonuna basin.\n");
            return;
        }
        String command = txtTerminalCommand.getText().trim();
        if (command.isEmpty()) return;

        String host    = txtHost.getText().trim();
        int port       = parsePort();
        String user    = txtUser.getText().trim();
        String pass    = rbPassword.isSelected() ? new String(txtPass.getPassword()) : null;
        String pemPath = getPemPath();
        // Sudo modu: terminal'deki toggle veya ust paneldeki checkbox
        boolean useSudo = (btnSudoMode != null && btnSudoMode.isSelected());

        // Prompt: sudo modundaysa kirmizi # ile goster
        String promptStr = useSudo
                ? "[root@" + host + "]# "
                : user + "@" + host + ":~$ ";

        terminalOutput.append("\n" + promptStr + command + "\n");
        txtTerminalCommand.setText("");
        btnTerminalRun.setEnabled(false);

        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                return dockerScanner.executeSshCommand(host, port, user, pass, pemPath, command, useSudo);
            }
            @Override protected void done() {
                try {
                    String output = get();
                    if (output == null || output.trim().isEmpty()) {
                        terminalOutput.append("(cikti yok)\n");
                    } else {
                        terminalOutput.append(output);
                        if (!output.endsWith("\n")) terminalOutput.append("\n");
                    }
                } catch (Exception ex) {
                    terminalOutput.append("[HATA] " + getCleanMessage(ex) + "\n");
                } finally {
                    btnTerminalRun.setEnabled(true);
                    terminalOutput.setCaretPosition(terminalOutput.getDocument().getLength());
                }
            }
        }.execute();
    }

    // ====================================================
    // YARDIMCI METODLAR
    // ====================================================

    private void loadSavedConfig() {
        if (isConnected) {
            rbPassword.setSelected(staticRbPassword);
            rbPemKey.setSelected(staticRbPemKey);
            txtPemPath.setEnabled(staticRbPemKey);
            txtPass.setEnabled(staticRbPassword);
            return;
        }
        // Config'den yuklenecek degerler zaten constructor'da set edildi.
        // Auth tipi varsayilani: pem varsa pem, yoksa sifre
        boolean hasPem = !config.getSshPemPath().isEmpty();
        rbPemKey.setSelected(hasPem);
        rbPassword.setSelected(!hasPem);
        txtPemPath.setEnabled(hasPem);
        txtPass.setEnabled(!hasPem);
    }

    private String getPemPath() {
        if (!rbPemKey.isSelected()) return null;
        String path = txtPemPath.getText().trim();
        return "/path/to/key.pem".equals(path) ? "" : path;
    }

    private int parsePort() {
        try { return Integer.parseInt(txtPort.getText().trim()); } catch (Exception e) { return 22; }
    }

    private void updateSshHintLabel(String hint) {
        if (hint.isEmpty()) { lblSshHint.setText(" "); return; }
        lblSshHint.setIcon(makeTextIcon(getHintIcon(hint)));
        lblSshHint.setText("  Bu dizin: " + getHintLabel(hint));
        lblSshHint.setForeground(getHintColor(hint));
    }

    private void updateDockerHintLabel(String hint) {
        if (hint.isEmpty()) { lblDockerHint.setText(" "); return; }
        lblDockerHint.setIcon(makeTextIcon(getHintIcon(hint)));
        lblDockerHint.setText("  Bu dizin: " + getHintLabel(hint));
        lblDockerHint.setForeground(getHintColor(hint));
    }

    private String getHintIcon(String hint) {
        switch (hint) {
            case "maven":  return "☕";
            case "node":   return "🟢";
            case "python": return "🐍";
            case "git":    return "🔴";
            case "docker": return "🐳";
            case "gradle": return "🟠";
            case "go":     return "🔵";
            case "rust":   return "🦀";
            default:       return "📦";
        }
    }

    private String getHintLabel(String hint) {
        switch (hint) {
            case "maven":  return "Java/Maven Projesi";
            case "node":   return "Node.js Projesi";
            case "python": return "Python Projesi";
            case "git":    return "Git Deposu";
            case "docker": return "Docker Projesi";
            case "gradle": return "Gradle Projesi";
            case "go":     return "Go Projesi";
            case "rust":   return "Rust Projesi";
            default:       return "Proje";
        }
    }

    private Color getHintColor(String hint) {
        switch (hint) {
            case "maven":  return CLR_HINT_MAVEN;
            case "node":   return CLR_HINT_NODE;
            case "python": return CLR_HINT_PY;
            case "git":    return CLR_HINT_GIT;
            case "docker": return CLR_HINT_DOCK;
            default:       return CLR_SUCCESS;
        }
    }

    // ====================================================
    // UI YARDIMCI METODLAR
    // ====================================================

    private JTextField makeTextField(String value, String placeholder) {
        String val = (value == null) ? "" : value;
        JTextField tf = new JTextField(val);
        styleTextField(tf);
        if (val.isEmpty()) tf.setText(placeholder);
        tf.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (tf.getText().equals(placeholder)) tf.setText("");
            }
            @Override public void focusLost(FocusEvent e) {
                if (tf.getText().isEmpty()) tf.setText(placeholder);
            }
        });
        tf.setForeground(val.isEmpty() ? CLR_TEXT_MUTED : CLR_TEXT);
        return tf;
    }

    private void styleTextField(JTextField tf) {
        tf.setBackground(CLR_CARD);
        tf.setForeground(CLR_TEXT);
        tf.setCaretColor(CLR_TEXT);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CLR_BORDER),
                new EmptyBorder(2, 6, 2, 6)));
    }

    private JLabel makeLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(CLR_TEXT_MUTED);
        return lbl;
    }

    /**
     * SSH ve Docker dizin gezginleri icin 'sudo ile gez' toggle butonu.
     * Acik iken 'sudo ls /root' gibi komutlar calistirilir, root'a ait
     * klasorler (ornek: /root, /var/lib/docker) gorunur hale gelir.
     */
    private JToggleButton makeSudoBrowseToggle() {
        JToggleButton btn = new JToggleButton("🔒 sudo");
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setBackground(new Color(45, 50, 65));
        btn.setForeground(new Color(255, 160, 80));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 90, 60)),
                new EmptyBorder(3, 8, 3, 8)));
        btn.setToolTipText("<html><b>sudo ile Goz At</b><br>" +
                "Acik iken dizinler root yetkisiyle listelenir.<br>" +
                "<b>/root</b>, <b>/var/lib/docker</b>, sistem klasorleri gorunur.<br>" +
                "Sifre 'Baglan' panelindeki ile ayni.");
        btn.addChangeListener(e -> {
            boolean on = btn.isSelected();
            btn.setBackground(on ? new Color(160, 50, 20) : new Color(45, 50, 65));
            btn.setForeground(on ? Color.WHITE : new Color(255, 160, 80));
            btn.setText(on ? "🔓 root" : "🔒 sudo");
        });
        return btn;
    }

    private JButton makeSmallButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setBackground(CLR_CARD);
        btn.setForeground(CLR_TEXT);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CLR_BORDER),
                new EmptyBorder(3, 8, 3, 8)));
        return btn;
    }

    private void restoreConnectionState() {
        if (!isConnected) return;
        
        lblConnStatus.setText("Baglandi!  Ev dizini: " + staticHomeDir);
        lblConnStatus.setForeground(CLR_SUCCESS);
        
        String startPath = config.getSshLastPath().isEmpty() ? staticHomeDir : config.getSshLastPath();
        txtSshCurrentPath.setEditable(true);
        loadSshDirectory(startPath);
        
        btnDockerLoadContainers.setEnabled(true);
        loadDockerContainers();
        
        String prompt = txtUser.getText().trim() + "@" + txtHost.getText().trim() + ":~$ ";
        lblTerminalPrompt.setText(prompt);
        txtTerminalCommand.setEnabled(true);
        btnTerminalRun.setEnabled(true);
        terminalOutput.setText("Baglanti devam ediyor...\n");
        terminalOutput.append("Ev dizini: " + staticHomeDir + "\n");
        terminalOutput.append("─".repeat(60) + "\n");
    }

    private String getCleanMessage(Throwable t) {
        if (t == null) return "Bilinmeyen hata";
        if (t instanceof java.util.concurrent.ExecutionException && t.getCause() != null) {
            return getCleanMessage(t.getCause());
        }
        String msg = t.getMessage();
        if (msg == null || msg.trim().isEmpty()) {
            return t.getClass().getSimpleName();
        }
        return msg;
    }

    private void styleRadio(JRadioButton rb) {
        rb.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        rb.setForeground(CLR_TEXT);
        rb.setBackground(CLR_PANEL);
    }

    private Icon makeTextIcon(String text) {
        return new Icon() {
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                g.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
                g.setColor(CLR_TEXT);
                g.drawString(text, x, y + 13);
            }
            @Override public int getIconWidth()  { return 18; }
            @Override public int getIconHeight() { return 16; }
        };
    }

    // ====================================================
    // OZEL LISTE RENDERER
    // ====================================================

    private class RemoteEntryRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                       boolean isSelected, boolean cellHasFocus) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            lbl.setBackground(isSelected ? CLR_ACCENT.darker() : (index % 2 == 0 ? CLR_CARD : CLR_PANEL));
            lbl.setForeground(isSelected ? Color.WHITE : CLR_TEXT);
            lbl.setBorder(new EmptyBorder(2, 8, 2, 8));
            String txt = value != null ? value.toString() : "";
            if (txt.startsWith("📁")) lbl.setForeground(isSelected ? Color.WHITE : CLR_DIR);
            else                       lbl.setForeground(isSelected ? Color.WHITE : CLR_FILE);
            return lbl;
        }
    }
}
