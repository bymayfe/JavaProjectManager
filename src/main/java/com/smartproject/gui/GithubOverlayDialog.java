package com.smartproject.gui;

import com.smartproject.git.GitManager;
import com.smartproject.git.GithubAPIManager;
import com.smartproject.model.Project;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Files;

public class GithubOverlayDialog extends JDialog {

    private Project project;
    private String gitUser;
    private String gitToken;
    private GitManager gitManager;
    private GithubAPIManager apiManager;
    
    private JTextField txtRepoName;
    private JComboBox<String> cbVisibility;
    private JTextArea txtReadme;
    private JButton btnPush;
    private JButton btnCancel;
    private JLabel lblStatus;

    public GithubOverlayDialog(JFrame parent, Project project, String gitUser, String gitToken, GitManager gitManager) {
        super(parent, "GitHub'a Gönder - " + project.getDisplayName(), true);
        this.project = project;
        this.gitUser = gitUser;
        this.gitToken = gitToken;
        this.gitManager = gitManager;
        this.apiManager = new GithubAPIManager();

        initUI();
        loadReadmeContent();
    }

    private void initUI() {
        setSize(700, 600);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // --- Üst Kısım: Ayarlar ---
        JPanel settingsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        settingsPanel.add(new JLabel("Aktif GitHub Hesabı:"), gbc);

        JLabel lblUser = new JLabel(gitUser != null && !gitUser.isEmpty() ? gitUser : "(Kullanıcı adı girilmemiş)");
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblUser.setForeground(new Color(66, 133, 244));
        gbc.gridx = 1; gbc.weightx = 1.0;
        settingsPanel.add(lblUser, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        settingsPanel.add(new JLabel("Depo (Repo) Adı:"), gbc);
        
        txtRepoName = new JTextField(project.getProjectFolder().getName().replaceAll("\\s+", "-"));
        gbc.gridx = 1; gbc.weightx = 1.0;
        settingsPanel.add(txtRepoName, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        settingsPanel.add(new JLabel("Gizlilik:"), gbc);

        cbVisibility = new JComboBox<>(new String[]{"Public (Herkese Açık)", "Private (Gizli)"});
        gbc.gridx = 1;
        settingsPanel.add(cbVisibility, gbc);

        mainPanel.add(settingsPanel, BorderLayout.NORTH);

        // --- Orta Kısım: README Önizlemesi ---
        JPanel readmePanel = new JPanel(new BorderLayout());
        readmePanel.setBorder(BorderFactory.createTitledBorder("README.md Önizlemesi (Düzenlenebilir)"));
        
        txtReadme = new JTextArea();
        txtReadme.setFont(new Font("Consolas", Font.PLAIN, 13));
        txtReadme.setLineWrap(true);
        txtReadme.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(txtReadme);
        readmePanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(readmePanel, BorderLayout.CENTER);

        // --- Alt Kısım: Butonlar ---
        JPanel bottomPanel = new JPanel(new BorderLayout());
        
        lblStatus = new JLabel("Hazır.");
        lblStatus.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        bottomPanel.add(lblStatus, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnCancel = new JButton("İptal");
        btnCancel.addActionListener(e -> dispose());
        
        btnPush = new JButton("Oluştur ve Yükle");
        btnPush.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnPush.addActionListener(new PushAction());

        btnPanel.add(btnCancel);
        btnPanel.add(btnPush);
        bottomPanel.add(btnPanel, BorderLayout.EAST);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void loadReadmeContent() {
        try {
            File readmeFile = new File(project.getSpmFolder(), "README.md");
            if (readmeFile.exists()) {
                String content = new String(Files.readAllBytes(readmeFile.toPath()));
                txtReadme.setText(content);
            } else {
                txtReadme.setText("# " + project.getDisplayName() + "\n\nBu proje henüz AI ile analiz edilmedi.");
            }
        } catch (Exception e) {
            txtReadme.setText("README yüklenirken hata oluştu.");
        }
    }

    private class PushAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (gitUser == null || gitUser.isEmpty() || gitToken == null || gitToken.isEmpty()) {
                JOptionPane.showMessageDialog(GithubOverlayDialog.this, 
                    "GitHub'a yükleme yapabilmek için lütfen 'Ayarlar' sekmesine dönüp GitHub Kullanıcı Adı ve Token giriniz!", 
                    "Eksik Bilgi", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String repoName = txtRepoName.getText().trim();
            boolean isPrivate = cbVisibility.getSelectedIndex() == 1;

            if (repoName.isEmpty()) {
                JOptionPane.showMessageDialog(GithubOverlayDialog.this, "Lütfen bir repo adı girin!", "Uyarı", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Arayüzü kilitle
            btnPush.setEnabled(false);
            btnCancel.setEnabled(false);
            lblStatus.setText("Lütfen bekleyin, GitHub'da işlem yapılıyor...");

            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() throws Exception {
                    // 1. Önce README dosyasındaki değişiklikleri kaydet
                    File spmFolder = project.getSpmFolder();
                    if (!spmFolder.exists()) spmFolder.mkdirs();
                    File readmeFile = new File(spmFolder, "README.md");
                    Files.write(readmeFile.toPath(), txtReadme.getText().getBytes("utf-8"));

                    // 2. Mevcut remote durumunu ve sahibini kontrol et
                    String currentRemote = gitManager.getRemoteUrl(project);
                    String currentOwner = gitManager.getOwnerFromUrl(currentRemote);
                    String targetUrl = "https://github.com/" + gitUser + "/" + repoName + ".git";

                    if (currentRemote == null || currentOwner == null || !currentOwner.equalsIgnoreCase(gitUser) || currentRemote.contains("@")) {
                        lblStatus.setText("Yeni profil/hesap için GitHub'da repo oluşturuluyor...");
                        try {
                            targetUrl = apiManager.createRepository(repoName, isPrivate, gitToken);
                        } catch (Exception ex) {
                            // Repo zaten yeni kullanıcının hesabında mevcutsa API hata verir, devam edelim
                            targetUrl = "https://github.com/" + gitUser + "/" + repoName + ".git";
                        }
                    }

                    lblStatus.setText("Kodlar pushlanıyor...");
                    return gitManager.commitAndPush(project, gitUser, gitToken, targetUrl);
                }

                @Override
                protected void done() {
                    try {
                        String result = get();
                        JOptionPane.showMessageDialog(GithubOverlayDialog.this, result, "Sonuç", JOptionPane.INFORMATION_MESSAGE);
                        dispose(); // İşlem bitince pencereyi kapat
                    } catch (Exception ex) {
                        lblStatus.setText("Hata oluştu.");
                        JOptionPane.showMessageDialog(GithubOverlayDialog.this, "Hata: " + ex.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
                        btnPush.setEnabled(true);
                        btnCancel.setEnabled(true);
                    }
                }
            }.execute();
        }
    }
}
