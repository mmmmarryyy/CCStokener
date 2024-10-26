package com.osgi.desktop.ui.plugins;

import com.onetwork.core.resource.Resource;
import com.onetwork.core.resource.ResourceType;
import com.osgi.desktop.domain.SitePlugin;
import com.osgi.desktop.file.PluginWrapper;
import com.osgi.desktop.file.PluginWrapperImpl;
import com.osgi.desktop.ui.util.SwingUtil;
import com.osgi.desktop.ui.util.UtilIcons;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTreeTable;
import org.knopflerfish.framework.Framework;
import org.osgi.framework.Bundle;

public class PluginsPanel extends javax.swing.JPanel implements BundleSelectionListener, SitePluginSelectionListener {

    private Bundle selectedBundle;

    private SitePlugin selectedSitePlugin;

    private PluginsDisponiveisChangeListener disponiveisChangeListener;

    public PluginsPanel() {
        initComponents();
        this.desativarButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                Framework framework = Resource.get(ResourceType.OSGIFramework);
                try {
                    if (selectedBundle.getState() == Bundle.ACTIVE) {
                        framework.stopBundle(selectedBundle.getBundleId());
                        desativarButton.setToolTipText("Ativar");
                        desativarButton.setIcon(UtilIcons.pluginsAtivarIcon());
                    } else {
                        framework.startBundle(selectedBundle.getBundleId());
                        desativarButton.setToolTipText("Desativar");
                        desativarButton.setIcon(UtilIcons.pluginsDesativarIcon());
                    }
                    updateUI();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        this.atualizarButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                Framework framework = Resource.get(ResourceType.OSGIFramework);
                long idBundle = framework.getBundleId(selectedSitePlugin != null ? selectedSitePlugin.getUrl().toString() : selectedBundle.getLocation());
                try {
                    if (idBundle == -1) {
                        framework.installBundle(selectedSitePlugin.getUrl().toString(), selectedSitePlugin.getUrl().openStream());
                        if (disponiveisChangeListener != null) disponiveisChangeListener.notifyPluginsDisponiveisChangeListener(selectedSitePlugin);
                    } else if (idBundle > 0) framework.getSystemBundleContext().getBundle(idBundle).update();
                    selectedBundle = null;
                    selectedBundle = null;
                    atualizarButton.setVisible(false);
                } catch (Exception e) {
                    SwingUtil.displayError("Erro ao instalar ou atualizar plugin!");
                    e.printStackTrace();
                }
            }
        });
        this.desativarButton.setVisible(false);
        this.desativarButton.setText("");
        this.atualizarButton.setVisible(false);
        this.atualizarButton.setText("");
    }

    public void addPluginsDisponiveisChangeListener(PluginsDisponiveisChangeListener disponiveisChangeListener) {
        this.disponiveisChangeListener = disponiveisChangeListener;
    }

    public void desativarButtonVisible(boolean isVisible) {
        desativarButton.setVisible(isVisible);
    }

    void setTable(JXTable table) {
        this.mainScrollPane.getViewport().add(table);
    }

    void setTable(JXTreeTable table) {
        this.mainScrollPane.getViewport().add(table);
    }

    @Override
    public void notifySitePluginSelected(SitePlugin sitePlugin) {
        this.desativarButton.setVisible(false);
        this.atualizarButton.setVisible(false);
        this.limpar();
        if (sitePlugin != null) {
            this.atualizarButton.setVisible(true);
            Framework framework = Resource.get(ResourceType.OSGIFramework);
            this.selectedSitePlugin = sitePlugin;
            long idBundle = framework.getBundleId(this.selectedSitePlugin.getNomeSimbolico());
            if (idBundle == -1) {
                atualizarButton.setIcon(UtilIcons.pluginsInstalarIcon());
                atualizarButton.setToolTipText("Instalar");
            }
            nomePluginLabel.setText(sitePlugin.getNome());
            versaoLabel.setText(sitePlugin.getVersao());
            autorLabel.setText(sitePlugin.getAutor());
            categoriaLabel.setText(sitePlugin.getCategoria());
            activatorLabel.setText(sitePlugin.getActivator());
            locationLabel.setText(sitePlugin.getUrl().toString());
            descricaoTextArea.setText(sitePlugin.getDescricao());
            importTextArea.setText(sitePlugin.getImportPackage());
            exportTextArea.setText(sitePlugin.getExportPackage());
            classPathTextArea.setText(sitePlugin.getClasspath());
        }
    }

    public void limpar() {
        nomePluginLabel.setText("");
        versaoLabel.setText("");
        autorLabel.setText("");
        categoriaLabel.setText("");
        activatorLabel.setText("");
        locationLabel.setText("");
        descricaoTextArea.setText("");
        importTextArea.setText("");
        exportTextArea.setText("");
        classPathTextArea.setText("");
    }

    @Override
    public void notifyBundleSelected(Bundle bundle) {
        this.limpar();
        this.selectedBundle = bundle;
        this.desativarButton.setVisible(true);
        if (this.existePossibilidadeDeAtualizacao(bundle)) {
            this.atualizarButton.setVisible(true);
            this.atualizarButton.setIcon(UtilIcons.pluginsAtualizarIcon());
            this.atualizarButton.setToolTipText("Atualizar");
        } else this.atualizarButton.setVisible(false);
        if (selectedBundle.getState() == Bundle.ACTIVE) {
            desativarButton.setToolTipText("Desativar");
            desativarButton.setIcon(UtilIcons.pluginsDesativarIcon());
        } else {
            desativarButton.setToolTipText("Ativar");
            desativarButton.setIcon(UtilIcons.pluginsAtivarIcon());
        }
        updateUI();
        nomePluginLabel.setText(bundle.getSymbolicName());
        Dictionary<String, String> dictionary = bundle.getHeaders();
        versaoLabel.setText(dictionary.get("Bundle-Version"));
        autorLabel.setText(dictionary.get("Bundle-Vendor"));
        categoriaLabel.setText(dictionary.get("Bundle-Category"));
        activatorLabel.setText(dictionary.get("Bundle-Activator"));
        locationLabel.setText(bundle.getLocation());
        descricaoTextArea.setText(dictionary.get("Bundle-Description"));
        importTextArea.setText(dictionary.get("Import-Package"));
        exportTextArea.setText(dictionary.get("Export-Package"));
        classPathTextArea.setText(dictionary.get("Bundle-Classpath"));
    }

    public boolean existePossibilidadeDeAtualizacao(Bundle bundle) {
        boolean retorno = false;
        Dictionary<String, String> dictionary = bundle.getHeaders();
        String localBundleVersion = dictionary.get("Bundle-Version");
        String strURLJar = "jar:" + bundle.getLocation() + "!/";
        try {
            URL urlJar = new URL(strURLJar);
            JarURLConnection jarURLConnection = (JarURLConnection) urlJar.openConnection();
            JarFile jarFile = jarURLConnection.getJarFile();
            PluginWrapper pluginWrapper = new PluginWrapperImpl(jarFile, new URL(bundle.getLocation()));
            String remoteBundleVersion = pluginWrapper.getManifest().bundleVersion();
            retorno = !(localBundleVersion.equals(remoteBundleVersion));
        } catch (MalformedURLException ex) {
            Logger.getLogger(PluginsPanel.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ioException) {
            Logger.getLogger(PluginsPanel.class.getName()).log(Level.SEVERE, null, ioException);
        }
        return retorno;
    }

    private void initComponents() {
        jXTaskPane1 = new org.jdesktop.swingx.JXTaskPane();
        splitPane = new javax.swing.JSplitPane();
        jPanel3 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        nomePluginLabel = new javax.swing.JLabel();
        desativarButton = new javax.swing.JButton();
        atualizarButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        versaoLabel = new javax.swing.JLabel();
        jLabel = new javax.swing.JLabel();
        autorLabel = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        activatorLabel = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        locationLabel = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        categoriaLabel = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        importTextArea = new javax.swing.JTextArea();
        jLabel6 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        exportTextArea = new javax.swing.JTextArea();
        jLabel7 = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        classPathTextArea = new javax.swing.JTextArea();
        jLabel8 = new javax.swing.JLabel();
        jScrollPane5 = new javax.swing.JScrollPane();
        descricaoTextArea = new javax.swing.JTextArea();
        mainScrollPane = new javax.swing.JScrollPane();
        splitPane.setDividerLocation(350);
        splitPane.setOneTouchExpandable(true);
        jPanel4.setBackground(new java.awt.Color(204, 204, 255));
        jPanel4.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true));
        nomePluginLabel.setFont(new java.awt.Font("Tahoma", 1, 16));
        desativarButton.setText("Desativar");
        atualizarButton.setText("Atualizar");
        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup().addContainerGap().addComponent(nomePluginLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 215, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(desativarButton).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(atualizarButton).addContainerGap()));
        jPanel4Layout.setVerticalGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel4Layout.createSequentialGroup().addGap(19, 19, 19).addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(atualizarButton).addComponent(desativarButton)).addContainerGap(21, Short.MAX_VALUE)).addComponent(nomePluginLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 63, Short.MAX_VALUE));
        jLabel1.setText("Versao:");
        jLabel.setText("Autor:");
        jLabel2.setText("Activator:");
        jLabel3.setText("Import:");
        jLabel4.setText("Url:");
        jLabel5.setText("Categoria:");
        importTextArea.setColumns(20);
        importTextArea.setRows(3);
        jScrollPane2.setViewportView(importTextArea);
        jLabel6.setText("Export:");
        exportTextArea.setColumns(20);
        exportTextArea.setRows(3);
        jScrollPane3.setViewportView(exportTextArea);
        jLabel7.setText("Class-Path:");
        classPathTextArea.setColumns(20);
        classPathTextArea.setRows(3);
        jScrollPane4.setViewportView(classPathTextArea);
        jLabel8.setText("Descricao:");
        descricaoTextArea.setColumns(20);
        descricaoTextArea.setRows(3);
        jScrollPane5.setViewportView(descricaoTextArea);
        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGroup(jPanel3Layout.createSequentialGroup().addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel3Layout.createSequentialGroup().addContainerGap().addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(jLabel1).addComponent(jLabel5))).addGroup(jPanel3Layout.createSequentialGroup().addGap(44, 44, 44).addComponent(jLabel4)).addGroup(jPanel3Layout.createSequentialGroup().addContainerGap().addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(jLabel3).addComponent(jLabel8).addComponent(jLabel6).addComponent(jLabel7)))).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 320, Short.MAX_VALUE).addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 320, Short.MAX_VALUE).addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 320, Short.MAX_VALUE).addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 320, Short.MAX_VALUE).addGroup(jPanel3Layout.createSequentialGroup().addGap(5, 5, 5).addComponent(locationLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 315, Short.MAX_VALUE)).addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel3Layout.createSequentialGroup().addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(versaoLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 111, Short.MAX_VALUE).addComponent(categoriaLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 111, Short.MAX_VALUE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel3Layout.createSequentialGroup().addComponent(jLabel2).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(activatorLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 149, Short.MAX_VALUE)).addGroup(jPanel3Layout.createSequentialGroup().addComponent(jLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(autorLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 166, Short.MAX_VALUE))))).addContainerGap()));
        jPanel3Layout.setVerticalGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel3Layout.createSequentialGroup().addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel3Layout.createSequentialGroup().addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLabel).addComponent(autorLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(versaoLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(jLabel2).addComponent(categoriaLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(activatorLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE))).addGroup(jPanel3Layout.createSequentialGroup().addComponent(jLabel1).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jLabel5))).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(jLabel4).addComponent(locationLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)).addGap(7, 7, 7).addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLabel8).addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabel3)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabel6)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabel7)).addGap(26, 26, 26)));
        splitPane.setRightComponent(jPanel3);
        splitPane.setLeftComponent(mainScrollPane);
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(splitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 757, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(splitPane));
    }

    private javax.swing.JLabel activatorLabel;

    private javax.swing.JButton atualizarButton;

    private javax.swing.JLabel autorLabel;

    private javax.swing.JLabel categoriaLabel;

    private javax.swing.JTextArea classPathTextArea;

    private javax.swing.JButton desativarButton;

    private javax.swing.JTextArea descricaoTextArea;

    private javax.swing.JTextArea exportTextArea;

    private javax.swing.JTextArea importTextArea;

    private javax.swing.JLabel jLabel;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JLabel jLabel4;

    private javax.swing.JLabel jLabel5;

    private javax.swing.JLabel jLabel6;

    private javax.swing.JLabel jLabel7;

    private javax.swing.JLabel jLabel8;

    private javax.swing.JPanel jPanel3;

    private javax.swing.JPanel jPanel4;

    private javax.swing.JScrollPane jScrollPane2;

    private javax.swing.JScrollPane jScrollPane3;

    private javax.swing.JScrollPane jScrollPane4;

    private javax.swing.JScrollPane jScrollPane5;

    private org.jdesktop.swingx.JXTaskPane jXTaskPane1;

    private javax.swing.JLabel locationLabel;

    private javax.swing.JScrollPane mainScrollPane;

    private javax.swing.JLabel nomePluginLabel;

    private javax.swing.JSplitPane splitPane;

    private javax.swing.JLabel versaoLabel;
}
