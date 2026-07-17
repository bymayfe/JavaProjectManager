package com.smartproject.gui;

import com.smartproject.db.ProjectRepository;
import com.smartproject.model.Project;
import com.smartproject.scanner.DockerScanner;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Yerel Docker container listesini gosterip secilen container'i tarayan dialog.
 * Sadece CLI (docker komutu) ve REST API (localhost:2375) modlarini destekler.
 * SSH modlari RemoteConnectionDialog'a tasindi.
 */
public class DockerScanDialog extends JDialog {

    private final DockerScanner dockerScanner;
    private final DefaultListModel<Project> parentListModel;
    private final ProjectRepository repository;

    private JComboBox<String> cbMode;
    private DefaultListModel<String> containerListModel;
    private JList<String> containerJList;
    private JTextField txtRemotePath;
    private JButton btnLoadContainers;
    private JButton btnScan;
    private JLabel lblStatus;

    private List<Map<String, String>> containerData = new ArrayList<>();

    public DockerScanDialog(JFrame parent, DockerScanner dockerScanner,
                            DefaultListModel<Project> parentListModel,
                            ProjectRepository repository) {
        super(parent, "Docker Tarayici (Yerel)", true);
        this.dockerScanner   = dockerScanner;
        this.parentListModel = parentListModel;
        this.repository      = repository;

        setSize(500, 400);
        setLocationRelativeTo(parent);
        buildUI();
    }

    private void buildUI() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));

        // Ust: mod secimi + dizin + buton
        JPanel topPanel = new JPanel(new GridLayout(3, 2, 8, 6));

        topPanel.add(new JLabel("Baglanti Modu:"));
        cbMode = new JComboBox<>(new String[]{"CLI (docker komutu)", "REST API (localhost:2375)"});
        topPanel.add(cbMode);

        topPanel.add(new JLabel("Container Dizin Yolu:"));
        txtRemotePath = new JTextField("/app");
        topPanel.add(txtRemotePath);

        topPanel.add(new JLabel(""));
        btnLoadContainers = new JButton("Container Listesini Getir");
        btnLoadContainers.addActionListener(e -> loadContainers());
        topPanel.add(btnLoadContainers);

        panel.add(topPanel, BorderLayout.NORTH);

        // Orta: Container listesi
        containerListModel = new DefaultListModel<>();
        containerJList = new JList<>(containerListModel);
        containerJList.setFont(new Font("Consolas", Font.PLAIN, 13));
        containerJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane scroll = new JScrollPane(containerJList);
        scroll.setBorder(BorderFactory.createTitledBorder("Calisma Container'lari"));
        panel.add(scroll, BorderLayout.CENTER);

        // Alt: Durum + Butonlar
        JPanel bottomPanel = new JPanel(new BorderLayout(8, 6));

        lblStatus = new JLabel("Container listesini getirmek icin butona basin.");
        lblStatus.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        bottomPanel.add(lblStatus, BorderLayout.NORTH);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnCancel = new JButton("Iptal");
        btnCancel.addActionListener(e -> dispose());

        btnScan = new JButton("Secili Container'lari Tara");
        btnScan.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnScan.setEnabled(false);
        btnScan.addActionListener(e -> scanSelected());

        btnRow.add(btnCancel);
        btnRow.add(btnScan);
        bottomPanel.add(btnRow, BorderLayout.SOUTH);

        panel.add(bottomPanel, BorderLayout.SOUTH);
        add(panel);
    }

    private void loadContainers() {
        int mode = cbMode.getSelectedIndex();
        btnLoadContainers.setEnabled(false);
        lblStatus.setText("Container listesi aliniyor...");
        containerListModel.clear();
        containerData.clear();
        btnScan.setEnabled(false);

        new SwingWorker<List<Map<String, String>>, Void>() {
            @Override protected List<Map<String, String>> doInBackground() throws Exception {
                return mode == 0 ? dockerScanner.listContainersCli() : dockerScanner.listContainersApi();
            }
            @Override protected void done() {
                try {
                    containerData = get();
                    if (containerData.isEmpty()) {
                        lblStatus.setText("Calisma container'i bulunamadi. Docker'in calistiginden emin olun.");
                    } else {
                        for (Map<String, String> c : containerData) {
                            containerListModel.addElement(c.get("id") + "  |  " + c.get("name") + "  (" + c.get("image") + ")");
                        }
                        lblStatus.setText(containerData.size() + " container bulundu. Birini veya birkacini secin.");
                        btnScan.setEnabled(true);
                    }
                } catch (Exception ex) {
                    lblStatus.setText("Hata: " + ex.getMessage());
                } finally {
                    btnLoadContainers.setEnabled(true);
                }
            }
        }.execute();
    }

    private void scanSelected() {
        int mode = cbMode.getSelectedIndex();
        int[] indices = containerJList.getSelectedIndices();
        if (indices.length == 0) {
            JOptionPane.showMessageDialog(this, "Lutfen listeden en az bir container secin.", "Uyari", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String path = txtRemotePath.getText().trim();
        lblStatus.setText("Tarama baslatildi: " + indices.length + " container taranacak.");
        btnScan.setEnabled(false);

        new SwingWorker<List<Project>, Project>() {
            @Override protected List<Project> doInBackground() throws Exception {
                List<Project> results = new ArrayList<>();
                for (int idx : indices) {
                    Map<String, String> selected = containerData.get(idx);
                    String id   = selected.get("id");
                    String name = selected.get("name");
                    if (mode == 0) {
                        results.addAll(dockerScanner.scanContainerCli(id, name, path));
                    } else {
                        results.addAll(dockerScanner.scanContainerApi(id, name, path));
                    }
                }
                return results;
            }
            @Override protected void done() {
                try {
                    List<Project> projects = get();
                    for (Project p : projects) {
                        parentListModel.addElement(p);
                        repository.saveProject(p);
                    }
                    lblStatus.setText("Tamamlandi! " + projects.size() + " proje eklendi.");
                    JOptionPane.showMessageDialog(DockerScanDialog.this,
                            projects.size() + " adet proje basariyla eklendi.",
                            "Basarili", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } catch (Exception ex) {
                    lblStatus.setText("Hata: " + ex.getMessage());
                    JOptionPane.showMessageDialog(DockerScanDialog.this,
                            "Hata: " + ex.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
                } finally {
                    btnScan.setEnabled(true);
                }
            }
        }.execute();
    }
}
