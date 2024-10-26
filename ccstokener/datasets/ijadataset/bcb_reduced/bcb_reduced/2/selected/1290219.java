package com.safi.workshop.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceImpl;
import org.eclipse.emf.edit.command.AbstractOverrideableCommand;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.workspace.util.WorkspaceSynchronizer;
import org.eclipse.gmf.runtime.emf.core.resources.GMFResource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.ide.StatusUtil;
import org.eclipse.ui.internal.wizards.newresource.ResourceMessages;
import org.eclipse.ui.statushandlers.StatusAdapter;
import org.eclipse.ui.statushandlers.StatusManager;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.osgi.framework.Version;
import com.safi.core.actionstep.ActionStepFactory;
import com.safi.core.actionstep.DynamicValue;
import com.safi.core.actionstep.DynamicValueType;
import com.safi.core.saflet.SafletPackage;
import com.safi.core.scripting.ScriptingPackage;
import com.safi.db.DBResource;
import com.safi.db.DbPackage;
import com.safi.db.Query;
import com.safi.db.QueryParameter;
import com.safi.db.server.config.ConfigFactory;
import com.safi.db.server.config.Saflet;
import com.safi.db.server.config.SafletProject;
import com.safi.db.server.config.ServerResource;
import com.safi.db.server.config.User;
import com.safi.server.manager.SafiServerRemoteManager;
import com.safi.server.plugin.SafiServerPlugin;
import com.safi.server.saflet.manager.DBManager;
import com.safi.server.saflet.manager.DBManagerException;
import com.safi.server.saflet.util.FileUtils;
import com.safi.workshop.SafiProjectNature;
import com.safi.workshop.navigator.PersistenceProperties;
import com.safi.workshop.navigator.db.PublishSafletDialog;
import com.safi.workshop.navigator.db.SelectSafletPanel;
import com.safi.workshop.part.AsteriskDiagramEditor;
import com.safi.workshop.part.AsteriskDiagramEditorPlugin;
import com.safi.workshop.part.SafiWorkshopEditorUtil;
import com.safi.workshop.part.AsteriskDiagramEditorPlugin.ActionPak;
import com.safi.workshop.part.AsteriskDiagramEditorPlugin.ActionPakJar;
import com.safi.workshop.part.AsteriskDiagramEditorPlugin.SafiServerJar;
import com.safi.workshop.sqlexplorer.dbproduct.Alias;
import com.safi.workshop.sqlexplorer.dbproduct.AliasManager;
import com.safi.workshop.sqlexplorer.dbproduct.DriverManager;
import com.safi.workshop.sqlexplorer.dbproduct.ManagedDriver;
import com.safi.workshop.sqlexplorer.plugin.SQLExplorerPlugin;

public class SafletPersistenceManager {

    public static final QualifiedName RES_ID_KEY = new QualifiedName(AsteriskDiagramEditorPlugin.ID, PersistenceProperties.RES_ID);

    public static final QualifiedName MODIFIED_KEY = new QualifiedName(AsteriskDiagramEditorPlugin.ID, PersistenceProperties.LAST_MODIFIED);

    public static final QualifiedName UPDATED_KEY = new QualifiedName(AsteriskDiagramEditorPlugin.ID, PersistenceProperties.LAST_UPDATED);

    public static final QualifiedName SAFLET_NAME_KEY = new QualifiedName(AsteriskDiagramEditorPlugin.ID, PersistenceProperties.SAFLET_NAME);

    private static SafletPersistenceManager instance = new SafletPersistenceManager();

    public static SafletPersistenceManager getInstance() {
        return instance;
    }

    public static int commitSaflet(com.safi.core.saflet.Saflet handler, Resource emfResource, IResource platformResource, final AsteriskDiagramEditor editor) {
        Resource emfRez = null;
        int success = IStatus.OK;
        if (handler == null) {
            IFile r = (IFile) platformResource;
            handler = SafletPersistenceManager.getInstance().getHandler(r);
            emfRez = SafletPersistenceManager.getInstance().getResourceLoader().getResource(URI.createFileURI(r.getFullPath().toPortableString()), true);
            emfResource = emfRez;
        }
        if (handler != null) {
            final PublishSafletDialog dlg = new PublishSafletDialog(SafiWorkshopEditorUtil.getActiveShell(), handler.getName(), handler.getDescription());
            if (Window.OK == dlg.open()) {
                String name = dlg.getName();
                boolean changed = false;
                try {
                    final com.safi.core.saflet.Saflet h = handler;
                    if (editor != null && editor.getDocumentProvider() != null && editor.isEditable() && editor.getEditingDomain() != null && editor.getEditingDomain().getCommandStack() != null) {
                        Command cmd = new AbstractOverrideableCommand(editor.getEditingDomain()) {

                            @Override
                            public void doExecute() {
                                h.setName(dlg.getName());
                                h.setDescription(dlg.getDescription());
                            }

                            @Override
                            public void doRedo() {
                            }

                            @Override
                            public void doUndo() {
                            }

                            @Override
                            public boolean doCanExecute() {
                                return true;
                            }
                        };
                        editor.getEditingDomain().getCommandStack().execute(cmd);
                    } else {
                        if (!name.equals(handler.getName())) {
                            handler.setName(name);
                        }
                        String description = dlg.getDescription();
                        if (!description.equals(handler.getDescription())) {
                            handler.setDescription(description);
                            changed = true;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    MessageDialog.openError(SafiWorkshopEditorUtil.getActiveShell(), "Write Error", "Couldn't save Saflet editor contents to disk: " + e.getLocalizedMessage());
                    success = IStatus.ERROR;
                }
                if (emfRez != null && changed) {
                    try {
                        emfRez.save(null);
                    } catch (IOException e) {
                        e.printStackTrace();
                        MessageDialog.openError(SafiWorkshopEditorUtil.getActiveShell(), "Write Error", "Couldn't save Saflet to disk: " + e.getLocalizedMessage());
                        success = IStatus.ERROR;
                    }
                }
                try {
                    TreeIterator<EObject> ti = emfResource.getAllContents();
                    SafletPersistenceManager.getInstance().publishSaflet(platformResource, emfResource, handler, dlg.isEnabled(), editor);
                } catch (Exception e) {
                    e.printStackTrace();
                    MessageDialog.openError(SafiWorkshopEditorUtil.getActiveShell(), "Publish Error", "Saflet " + handler.getName() + " could not be published: " + e.getLocalizedMessage());
                    success = IStatus.ERROR;
                }
            } else success = IStatus.CANCEL;
        }
        return success;
    }

    public void publishSaflet(IResource safletResource, Resource emfResource, com.safi.core.saflet.Saflet handler, boolean enabled, AsteriskDiagramEditor editor) throws PublishResourceException {
        Session session = null;
        try {
            session = DBManager.getInstance().createSession();
        } catch (DBManagerException e1) {
            throw new PublishResourceException(e1);
        }
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            Date now = new Date();
            IProject project = safletResource.getProject();
            int pid = getResourceId(project);
            boolean isNewProject = false;
            SafletProject parentProject = null;
            if (pid != -1) {
                List results = session.createCriteria(SafletProject.class).add(Restrictions.eq("id", pid)).list();
                if (!results.isEmpty()) {
                    parentProject = (SafletProject) results.get(0);
                }
            }
            List results = session.createCriteria(SafletProject.class).add(Restrictions.eq("name", project.getName())).list();
            SafletProject sameNameProj = null;
            if (!results.isEmpty()) {
                sameNameProj = (SafletProject) results.get(0);
            }
            if (parentProject == null) {
                if (sameNameProj != null) {
                    boolean ok = MessageDialog.openQuestion(SafiWorkshopEditorUtil.getActiveShell(), "Project Exists", "A SafiProject named " + project.getName() + " exists on the server.  Press OK to merge this project into the one on the serveror " + "press Cancel to halt the publishing process.");
                    if (!ok) return;
                    parentProject = sameNameProj;
                }
            } else if (sameNameProj != null && sameNameProj != parentProject) {
                boolean ok = MessageDialog.openQuestion(SafiWorkshopEditorUtil.getActiveShell(), "Project Was Renamed", "The local project was renamed from  " + parentProject.getName() + " to " + project.getName() + " but a project by that name exists on the server.  Press OK to revert the local project name to " + parentProject.getName() + " or press Cancel to halt the publishing process.");
                if (!ok) {
                    return;
                }
                IProjectDescription desc = project.getDescription();
                desc.setName(parentProject.getName());
                project.move(desc, true, null);
            }
            Saflet saflet = null;
            if (parentProject == null) {
                isNewProject = true;
                parentProject = ConfigFactory.eINSTANCE.createSafletProject();
                project.setPersistentProperty(MODIFIED_KEY, String.valueOf(now.getTime()));
                project.setPersistentProperty(UPDATED_KEY, String.valueOf(now.getTime()));
                parentProject.setName(project.getName());
                session.save(parentProject);
                project.setPersistentProperty(RES_ID_KEY, String.valueOf(parentProject.getId()));
            } else {
                project.setPersistentProperty(RES_ID_KEY, String.valueOf(parentProject.getId()));
                project.setPersistentProperty(MODIFIED_KEY, String.valueOf(now.getTime()));
                project.setPersistentProperty(UPDATED_KEY, String.valueOf(now.getTime()));
                Saflet sameName = null;
                Saflet sameId = null;
                int safletId = getResourceId(safletResource);
                for (Saflet s : parentProject.getSaflets()) {
                    if (safletId != -1) {
                        if (s.getId() == safletId) {
                            sameId = s;
                        }
                        if (StringUtils.equals(s.getName(), handler.getName())) {
                            sameName = s;
                        }
                    }
                }
                if (sameName == null) {
                    results = session.createCriteria(Saflet.class).add(Restrictions.eq("name", handler.getName())).list();
                    if (!results.isEmpty()) {
                        Saflet s = (Saflet) results.get(0);
                        boolean ok = MessageDialog.openQuestion(SafiWorkshopEditorUtil.getActiveShell(), "Saflet Exists", "A Saflet named " + handler.getName() + " exists on the server under project " + s.getProject().getName() + ".  Do you want to overwrite?");
                        if (!ok) {
                            transaction.rollback();
                            return;
                        }
                        EcoreUtil.remove(s);
                        session.delete(s);
                    }
                }
                if (sameId != null && sameId == sameName) {
                    saflet = sameId;
                } else if (sameName != null) {
                    boolean ok = MessageDialog.openQuestion(SafiWorkshopEditorUtil.getActiveShell(), "Saflet Exists", "A Saflet named " + handler.getName() + " exists on the server.  Do you want to overwrite?");
                    if (!ok) {
                        return;
                    }
                    EcoreUtil.remove(sameName);
                    session.delete(sameName);
                    if (sameId != null) saflet = sameId;
                } else if (sameId != null) {
                    saflet = sameId;
                }
            }
            User user = SafiServerPlugin.getDefault().getCurrentUser();
            if (saflet == null) {
                saflet = ConfigFactory.eINSTANCE.createSaflet();
                saflet.setCreatedBy(user);
                parentProject.getSaflets().add(saflet);
            }
            saflet.setName(handler.getName());
            saflet.setDescription(handler.getDescription());
            saflet.setLastModified(now);
            saflet.setModifiedBy(user);
            saflet.setLastUpdated(now);
            ByteArrayOutputStream strema = new ByteArrayOutputStream();
            emfResource.save(strema, null);
            printProject(saflet.getProject());
            saflet.setCode(strema.toByteArray());
            session.save(saflet);
            if (!StringUtils.equals(project.getName(), parentProject.getName())) {
                parentProject.setName(project.getName());
                session.save(parentProject);
            }
            if (editor != null) {
                EAttribute nameAttr = SafletPackage.eINSTANCE.getSaflet_Id();
                editor.getEditingDomain().getCommandStack().execute(SetCommand.create(editor.getEditingDomain(), handler, nameAttr, saflet.getId()));
                editor.doSave(new NullProgressMonitor());
            } else handler.setId(saflet.getId());
            safletResource.setPersistentProperty(MODIFIED_KEY, String.valueOf(now.getTime()));
            safletResource.setPersistentProperty(UPDATED_KEY, String.valueOf(now.getTime()));
            safletResource.setPersistentProperty(RES_ID_KEY, String.valueOf(saflet.getId()));
            transaction.commit();
            project.refreshLocal(IResource.DEPTH_INFINITE, null);
        } catch (Exception e) {
            e.printStackTrace();
            if (transaction != null) transaction.rollback();
            throw new PublishResourceException(e);
        } finally {
            session.close();
        }
    }

    public void updateSaflet(com.safi.core.saflet.Saflet handler, IFile resource, Resource emfResource, AsteriskDiagramEditor editor) throws UpdateSafletException {
        Session session = null;
        try {
            session = DBManager.getInstance().createSession();
        } catch (DBManagerException e1) {
            throw new UpdateSafletException(e1);
        }
        try {
            int pid = getResourceId(resource);
            Saflet saflet = null;
            if (pid != -1) {
                List results = session.createCriteria(Saflet.class).add(Restrictions.eq("id", pid)).list();
                if (!results.isEmpty()) {
                    saflet = (Saflet) results.get(0);
                } else return;
            } else return;
            if (saflet.getCode() == null) {
                saflet.setCode(DBManager.getInstance().getSafletCode(saflet.getId()));
            }
            IPath fullPath = writeSafletToExistingFile(resource, saflet);
            if (editor != null) {
                AsteriskDiagramEditorPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage().closeEditor(editor, false);
                editor = (AsteriskDiagramEditor) SafiWorkshopEditorUtil.openDiagram(URI.createFileURI(fullPath.toPortableString()), false, true);
            }
        } catch (Exception e) {
            throw new UpdateSafletException(e);
        } finally {
            session.close();
        }
    }

    public IPath writeSafletToExistingFile(IFile resource, Saflet saflet) throws FileNotFoundException, IOException, CoreException {
        IPath rootPath = resource.getWorkspace().getRoot().getLocation();
        resource.setContents(new ByteArrayInputStream(saflet.getCode()), true, true, null);
        IPath fullPath = rootPath.append(resource.getFullPath());
        Date now = new Date();
        resource.setPersistentProperty(RES_ID_KEY, String.valueOf(saflet.getId()));
        resource.setPersistentProperty(MODIFIED_KEY, String.valueOf(now.getTime()));
        resource.setPersistentProperty(UPDATED_KEY, String.valueOf(now.getTime()));
        resource.setPersistentProperty(SAFLET_NAME_KEY, saflet.getName());
        return fullPath;
    }

    public int getResourceId(IResource resource) {
        try {
            String pid = resource.getPersistentProperty(RES_ID_KEY);
            if (StringUtils.isBlank(pid)) return -1;
            return Integer.parseInt(pid);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public boolean setResourceId(IResource resource, int id) {
        try {
            resource.setPersistentProperty(RES_ID_KEY, String.valueOf(id));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public Date getLastUpdated(IResource p) {
        try {
            String lastMod = p.getPersistentProperty(SafletPersistenceManager.UPDATED_KEY);
            if (StringUtils.isBlank(lastMod)) return null;
            long lastModTime = Long.parseLong(lastMod);
            return new Date(lastModTime);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean setLastUpdated(IResource resource, long time) {
        try {
            resource.setPersistentProperty(SafletPersistenceManager.UPDATED_KEY, String.valueOf(time));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public Date getLastModified(IResource p) {
        try {
            String lastMod = p.getPersistentProperty(SafletPersistenceManager.MODIFIED_KEY);
            if (lastMod == null) return null;
            long lastModTime = Long.parseLong(lastMod);
            return new Date(lastModTime);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean setLastModified(IResource resource, long time) {
        try {
            resource.setPersistentProperty(SafletPersistenceManager.MODIFIED_KEY, String.valueOf(time));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void updateProject(IProject project) throws UpdateSafletException {
        Session session = null;
        try {
            session = DBManager.getInstance().createSession();
        } catch (DBManagerException e1) {
            throw new UpdateSafletException(e1);
        }
        try {
            int pid = getResourceId(project);
            SafletProject sproj = null;
            if (pid != -1) {
                List results = session.createCriteria(SafletProject.class).add(Restrictions.eq("id", pid)).list();
                if (!results.isEmpty()) {
                    sproj = (SafletProject) results.get(0);
                } else return;
            } else {
                return;
            }
            updateLocalProject(project, sproj);
        } catch (Exception e) {
            throw new UpdateSafletException(e);
        } finally {
            session.close();
        }
    }

    public void updateLocalProject(IProject project, SafletProject sproj) throws CoreException {
        Date now = new Date();
        project.setPersistentProperty(RES_ID_KEY, String.valueOf(sproj.getId()));
        project.setPersistentProperty(MODIFIED_KEY, String.valueOf(now.getTime()));
        project.setPersistentProperty(UPDATED_KEY, String.valueOf(now.getTime()));
        if (!StringUtils.equals(sproj.getName(), project.getName())) {
            IProjectDescription desc = project.getDescription();
            desc.setName(sproj.getName());
            project.move(desc, true, new NullProgressMonitor());
            project.refreshLocal(IResource.DEPTH_INFINITE, null);
        }
    }

    @SuppressWarnings("unchecked")
    public List<SafletProject> getAllProjects() throws UpdateSafletException {
        Session session = null;
        try {
            session = DBManager.getInstance().createSession();
        } catch (DBManagerException e1) {
            throw new UpdateSafletException(e1);
        }
        try {
            return session.createCriteria(SafletProject.class).list();
        } catch (Exception e) {
            throw new UpdateSafletException(e);
        } finally {
            session.close();
        }
    }

    public Saflet getSaflet(int safletId) throws DBManagerException {
        Session session = null;
        session = DBManager.getInstance().createSession();
        try {
            return (Saflet) session.get(Saflet.class, safletId);
        } finally {
            session.close();
        }
    }

    public void saveEmptyProject(IProject project) throws PublishResourceException {
        Session session = null;
        try {
            session = DBManager.getInstance().createSession();
        } catch (DBManagerException e1) {
            throw new PublishResourceException(e1);
        }
        try {
            session.beginTransaction();
            int pid = getResourceId(project);
            SafletProject sameId = null;
            if (pid != -1) {
                List results = session.createCriteria(SafletProject.class).add(Restrictions.eq("id", pid)).list();
                if (!results.isEmpty()) {
                    sameId = (SafletProject) results.get(0);
                }
            }
            SafletProject sameName = null;
            List results = session.createCriteria(SafletProject.class).add(Restrictions.eq("name", project.getName())).list();
            if (!results.isEmpty()) {
                sameName = (SafletProject) results.get(0);
            }
            if (sameName != null) {
                if (sameId != null && sameId != sameName) {
                    MessageDialog.openWarning(SafiWorkshopEditorUtil.getActiveShell(), "Project Renamed", "A Project named " + project.getName() + " already exists on the server. Publishing cannot continue.");
                    return;
                }
                Date now = new Date();
                project.setPersistentProperty(RES_ID_KEY, String.valueOf(sameName.getId()));
                project.setPersistentProperty(MODIFIED_KEY, String.valueOf(now.getTime()));
                project.setPersistentProperty(UPDATED_KEY, String.valueOf(now.getTime()));
                return;
            }
            if (sameId != null) {
                Date now = new Date();
                sameId.setName(project.getName());
                sameId.setLastUpdated(now);
                session.update(sameId);
                project.setPersistentProperty(RES_ID_KEY, String.valueOf(sameId.getId()));
                project.setPersistentProperty(MODIFIED_KEY, String.valueOf(now.getTime()));
                project.setPersistentProperty(UPDATED_KEY, String.valueOf(now.getTime()));
            } else {
                Date now = new Date();
                SafletProject newp = ConfigFactory.eINSTANCE.createSafletProject();
                newp.setName(project.getName());
                newp.setLastModified(now);
                newp.setLastUpdated(now);
                session.save(newp);
                int id = newp.getId();
                project.setPersistentProperty(RES_ID_KEY, String.valueOf(id));
                project.setPersistentProperty(MODIFIED_KEY, String.valueOf(now.getTime()));
                project.setPersistentProperty(UPDATED_KEY, String.valueOf(now.getTime()));
            }
            session.getTransaction().commit();
        } catch (Exception e) {
            throw new PublishResourceException(e);
        } finally {
            session.close();
        }
    }

    public void deleteProject(SafletProject project) throws UpdateSafletException {
        Session session = null;
        try {
            session = DBManager.getInstance().createSession();
        } catch (DBManagerException e1) {
            throw new UpdateSafletException(e1);
        }
        try {
            session.beginTransaction();
            session.delete(project);
            session.getTransaction().commit();
        } catch (Exception e) {
            throw new UpdateSafletException(e);
        } finally {
            session.close();
        }
    }

    public void deleteSaflet(Saflet saflet) throws UpdateSafletException {
        Session session = null;
        try {
            session = DBManager.getInstance().createSession();
        } catch (DBManagerException e1) {
            throw new UpdateSafletException(e1);
        }
        try {
            session.delete(saflet);
        } catch (Exception e) {
            throw new UpdateSafletException(e);
        } finally {
            session.close();
        }
    }

    public void deleteSaflets(List<Saflet> orphans) throws UpdateSafletException {
        Session session = null;
        try {
            session = DBManager.getInstance().createSession();
        } catch (DBManagerException e1) {
            throw new UpdateSafletException(e1);
        }
        try {
            session.beginTransaction();
            for (Saflet saflet : orphans) {
                session.delete(saflet);
            }
            session.getTransaction().commit();
        } catch (Exception e) {
            throw new UpdateSafletException(e);
        } finally {
            session.close();
        }
    }

    private void printSaflet(Saflet saflet) {
        try {
            XMLResource xmlResource = new XMLResourceImpl();
            xmlResource.getContents().add(saflet);
            StringWriter sw = new StringWriter();
            xmlResource.save(sw, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void printProject(SafletProject proj) {
        try {
            XMLResource xmlResource = new XMLResourceImpl();
            xmlResource.getContents().add(proj);
            StringWriter sw = new StringWriter();
            xmlResource.save(sw, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void prepareProjects(Shell shell, List<ServerResource> serverResourceList, Map<SafletProject, IProject> projectToResourceMap, List<IProject> existingProjects) {
        for (ServerResource sr : serverResourceList) {
            IProject sameId = null;
            IProject sameName = null;
            if (sr instanceof SafletProject) {
                for (IProject p : existingProjects) {
                    if (p.getName().equals(sr.getName())) sameName = p;
                    int pid = SafletPersistenceManager.getInstance().getResourceId(p);
                    if (sr.getId() == pid && pid != -1) sameId = p;
                }
                if (sameId != null && sameName != null && sameName != sameId) {
                    MessageDialog.openWarning(shell, "Project Name Conflict", "A project with the name " + sameName.getName() + " already exists and has been persisted under a different name. Project " + sameName.getName() + " cannot be retrieved");
                    continue;
                } else if (sameId != null && sameName == null) {
                    boolean ok = MessageDialog.openQuestion(shell, "Project Renamed", "The local project " + sameId.getName() + " has been renamed on the SafiServer.  Press OK to rename the local project instance or press " + "'Cancel' to skip this project.");
                    if (!ok) continue;
                    try {
                        IProjectDescription desc = sameId.getDescription();
                        desc.setName(sr.getName());
                        sameId.move(desc, true, null);
                        projectToResourceMap.put((SafletProject) sr, sameId);
                    } catch (CoreException e) {
                        e.printStackTrace();
                        MessageDialog.openError(shell, "Move Project Error", "Couldn't move project " + sameId.getName() + ": " + e.getLocalizedMessage());
                        AsteriskDiagramEditorPlugin.getInstance().logError("Move Project Error", e);
                        continue;
                    }
                } else if (sameId != null && sameId == sameName) {
                    projectToResourceMap.put((SafletProject) sr, sameId);
                } else if (sameName != null) {
                    projectToResourceMap.put((SafletProject) sr, sameName);
                } else {
                    try {
                        IProject newProj = createNewProject((SafletProject) sr);
                        projectToResourceMap.put((SafletProject) sr, newProj);
                    } catch (CoreException e) {
                        e.printStackTrace();
                        MessageDialog.openError(shell, "Create Project Error", "Couldn't create project " + sr.getName() + ": " + e.getLocalizedMessage());
                        AsteriskDiagramEditorPlugin.getInstance().logError("Create Project Error", e);
                        continue;
                    }
                }
            }
        }
    }

    public IProject createNewProject(SafletProject sr) throws CoreException {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        final IProject project = root.getProject(sr.getName());
        final IProjectDescription desc = ResourcesPlugin.getWorkspace().newProjectDescription(project.getName());
        desc.setLocation(null);
        String[] natures = desc.getNatureIds();
        String[] newNatures = new String[natures.length + 1];
        System.arraycopy(natures, 0, newNatures, 0, natures.length);
        newNatures[natures.length] = SafiProjectNature.NATURE_ID;
        desc.setNatureIds(newNatures);
        IRunnableWithProgress op = new IRunnableWithProgress() {

            public void run(IProgressMonitor monitor) throws InvocationTargetException {
                CreateProjectOperation op = new CreateProjectOperation(desc, ResourceMessages.NewProject_windowTitle);
                try {
                    PlatformUI.getWorkbench().getOperationSupport().getOperationHistory().execute(op, monitor, WorkspaceUndoUtil.getUIInfoAdapter(SafiWorkshopEditorUtil.getActiveShell()));
                } catch (ExecutionException e) {
                    throw new InvocationTargetException(e);
                }
            }
        };
        ProgressMonitorDialog pd = new ProgressMonitorDialog(SafiWorkshopEditorUtil.getActiveShell());
        try {
            pd.run(false, true, op);
        } catch (InterruptedException e) {
            return null;
        } catch (InvocationTargetException e) {
            Throwable t = e.getTargetException();
            if (t instanceof ExecutionException && t.getCause() instanceof CoreException) {
                CoreException cause = (CoreException) t.getCause();
                StatusAdapter status;
                if (cause.getStatus().getCode() == IResourceStatus.CASE_VARIANT_EXISTS) {
                    status = new StatusAdapter(StatusUtil.newStatus(IStatus.WARNING, NLS.bind(ResourceMessages.NewProject_caseVariantExistsError, project.getName()), cause));
                } else {
                    status = new StatusAdapter(StatusUtil.newStatus(cause.getStatus().getSeverity(), ResourceMessages.NewProject_errorMessage, cause));
                }
                status.setProperty(StatusAdapter.TITLE_PROPERTY, ResourceMessages.NewProject_errorMessage);
                StatusManager.getManager().handle(status, StatusManager.BLOCK);
            } else {
                StatusAdapter status = new StatusAdapter(new Status(IStatus.WARNING, IDEWorkbenchPlugin.IDE_WORKBENCH, 0, NLS.bind(ResourceMessages.NewProject_internalError, t.getMessage()), t));
                status.setProperty(StatusAdapter.TITLE_PROPERTY, ResourceMessages.NewProject_errorMessage);
                StatusManager.getManager().handle(status, StatusManager.LOG | StatusManager.BLOCK);
            }
        }
        return project;
    }

    public void addOrUpdateSaflets(IProject project, final List<Saflet> saflets, final boolean update, final boolean interactive) throws CoreException {
        final List<Saflet> safletsCopy = new ArrayList<Saflet>(saflets);
        project.accept(new IResourceVisitor() {

            @Override
            public boolean visit(IResource resource) throws CoreException {
                if (resource.getType() == IResource.FILE && "saflet".equals(resource.getFileExtension())) {
                    boolean skipAll = false, overwriteAll = false;
                    int pid = SafletPersistenceManager.getInstance().getResourceId(resource);
                    String existingName = SafletPersistenceManager.getInstance().getSafletName(resource);
                    for (Saflet saflet : saflets) {
                        final boolean sameName = StringUtils.equals(existingName, saflet.getName());
                        if ((pid == saflet.getId() && pid != -1) || sameName) {
                            safletsCopy.remove(saflet);
                            if (!update) continue;
                            if (interactive) {
                                if (skipAll) continue;
                                if (!overwriteAll) {
                                    String dialogMessage = null;
                                    if (sameName) dialogMessage = "A Saflet with name " + saflet.getName() + " already exists in the workspace. Do you wish to skip or overwrite? "; else dialogMessage = "Saflet (" + saflet.getName() + ") exists with the same ID a different name in the workspace (" + existingName + "). Do you wish to skip or overwrite? ";
                                    MessageDialog dlg = new MessageDialog(SafiWorkshopEditorUtil.getActiveShell(), "Overwrite Existing Saflet?", null, dialogMessage, MessageDialog.QUESTION, new String[] { "Skip", "Skip all", "Overwrite", "Overwrite All" }, 4);
                                    int result = dlg.open();
                                    switch(result) {
                                        case 0:
                                            continue;
                                        case 1:
                                            skipAll = true;
                                            continue;
                                        case 2:
                                            break;
                                        case 3:
                                            overwriteAll = true;
                                            break;
                                    }
                                }
                            }
                            IPath fullPath = null;
                            try {
                                fullPath = SafletPersistenceManager.getInstance().writeSafletToExistingFile((IFile) resource, saflet);
                                AsteriskDiagramEditor editor = getOpenEditor((IFile) resource);
                                if (editor != null) {
                                    AsteriskDiagramEditorPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage().closeEditor(editor, false);
                                    editor = (AsteriskDiagramEditor) SafiWorkshopEditorUtil.openDiagram(URI.createFileURI(fullPath.toPortableString()), false, true);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                MessageDialog.openError(SafiWorkshopEditorUtil.getActiveShell(), "Update Error", "Couldn't update Saflet: " + e.getLocalizedMessage());
                                AsteriskDiagramEditorPlugin.getInstance().logError("Couldn't update Saflet", e);
                                break;
                            }
                        }
                    }
                }
                return true;
            }
        });
        for (Saflet saflet : safletsCopy) {
            String filename = SafiWorkshopEditorUtil.getUniqueFileName(project, saflet.getName(), "saflet");
            IFile file = project.getFile(filename);
            try {
                byte[] code = saflet.getCode() == null ? DBManager.getInstance().getSafletCode(saflet.getId()) : (saflet.getCode() == null ? null : saflet.getCode());
                file.create(new ByteArrayInputStream(code), true, null);
                Date now = new Date();
                file.setPersistentProperty(RES_ID_KEY, String.valueOf(saflet.getId()));
                file.setPersistentProperty(MODIFIED_KEY, String.valueOf(now.getTime()));
                file.setPersistentProperty(UPDATED_KEY, String.valueOf(now.getTime()));
                file.setPersistentProperty(SAFLET_NAME_KEY, saflet.getName());
            } catch (DBManagerException e) {
                throw new CoreException(new Status(IStatus.ERROR, AsteriskDiagramEditorPlugin.ID, "Couldn't write Saflet to local file", e));
            }
        }
        updateLocalProject(project, saflets.get(0).getProject());
    }

    public AsteriskDiagramEditor getOpenEditor(IFile platformResource) {
        IWorkbenchPage[] pages = new IWorkbenchPage[0];
        IWorkbenchWindow activeWorkbenchWindow = AsteriskDiagramEditorPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
        if (activeWorkbenchWindow != null) pages = (activeWorkbenchWindow.getPages());
        for (IWorkbenchPage page : pages) {
            IEditorReference refs[] = page.getEditorReferences();
            for (IEditorReference ref : refs) {
                IEditorPart part = ref.getEditor(false);
                if (part == null) continue;
                if (part instanceof AsteriskDiagramEditor) {
                    ResourceSet set = ((AsteriskDiagramEditor) part).getEditingDomain().getResourceSet();
                    Resource r = set.getResources().get(0);
                    if (platformResource.equals(WorkspaceSynchronizer.getFile(r))) {
                        return (AsteriskDiagramEditor) part;
                    }
                }
            }
        }
        return null;
    }

    public com.safi.core.saflet.Saflet getHandler(IFile resource) {
        com.safi.core.saflet.Saflet handler = null;
        try {
            AsteriskDiagramEditor editor = getOpenEditor(resource);
            if (editor != null) {
                GMFResource gmfResource = null;
                ResourceSet set = editor.getEditingDomain().getResourceSet();
                for (Resource r : set.getResources()) {
                    if (r instanceof GMFResource && ("saflet".equalsIgnoreCase(r.getURI().fileExtension()))) {
                        gmfResource = (GMFResource) r;
                        break;
                    }
                }
                if (gmfResource != null) {
                    if (resource.equals(WorkspaceSynchronizer.getFile(gmfResource))) {
                        handler = (com.safi.core.saflet.Saflet) gmfResource.getContents().get(0);
                    }
                }
            }
            Resource emfRez = null;
            if (handler == null) {
                emfRez = getResourceLoader().getResource(URI.createFileURI(resource.getFullPath().toPortableString()), true);
                handler = (com.safi.core.saflet.Saflet) emfRez.getContents().get(0);
            }
        } catch (Exception e) {
            AsteriskDiagramEditorPlugin.getInstance().logError("Couldn't find Saflet " + resource, e);
            e.printStackTrace();
        }
        return handler;
    }

    public boolean renameSaflet(IFile resource, String newName) {
        try {
            com.safi.core.saflet.Saflet handler = null;
            String suffix = ".saflet";
            if (newName.endsWith(suffix)) newName = newName.substring(0, newName.length() - suffix.length());
            AsteriskDiagramEditor editor = getOpenEditor(resource);
            if (editor != null) {
                GMFResource gmfResource = null;
                ResourceSet set = editor.getEditingDomain().getResourceSet();
                for (Resource r : set.getResources()) {
                    if (r instanceof GMFResource && ("saflet".equalsIgnoreCase(r.getURI().fileExtension()))) {
                        gmfResource = (GMFResource) r;
                        break;
                    }
                }
                if (gmfResource != null) {
                    if (resource.equals(WorkspaceSynchronizer.getFile(gmfResource))) {
                        handler = (com.safi.core.saflet.Saflet) gmfResource.getContents().get(0);
                    }
                }
            }
            Resource emfRez = null;
            if (handler == null) {
                emfRez = getResourceLoader().getResource(URI.createFileURI(resource.getFullPath().toPortableString()), true);
                handler = (com.safi.core.saflet.Saflet) emfRez.getContents().get(0);
            }
            if (handler != null) {
                if (StringUtils.equals(handler.getName(), newName)) return true;
                handler.setName(newName);
                if (editor != null) editor.doSave(new NullProgressMonitor());
                if (emfRez != null) {
                    try {
                        emfRez.save(null);
                    } catch (IOException e) {
                        e.printStackTrace();
                        AsteriskDiagramEditorPlugin.getInstance().logError("Couldn't save Saflet file " + resource + " after renaming", e);
                        return false;
                    }
                }
                try {
                    resource.setPersistentProperty(SAFLET_NAME_KEY, newName);
                    resource.setPersistentProperty(MODIFIED_KEY, String.valueOf(System.currentTimeMillis()));
                } catch (CoreException e) {
                    AsteriskDiagramEditorPlugin.getInstance().logError("Couldn't set Saflet name persistent property " + resource + " after renaming", e);
                    e.printStackTrace();
                    return false;
                }
                return true;
            }
        } catch (Exception e) {
            AsteriskDiagramEditorPlugin.getInstance().logError("Couldn't rename Saflet " + resource, e);
            e.printStackTrace();
        }
        return false;
    }

    public String getSafletName(IResource resource) {
        String name = null;
        try {
            name = resource.getPersistentProperty(SafletPersistenceManager.SAFLET_NAME_KEY);
        } catch (CoreException e) {
            e.printStackTrace();
        }
        if (name != null) {
            return name;
        }
        name = resource.getName();
        return name.substring(0, name.length() - (resource.getFileExtension().length() + 1));
    }

    public void disconnectLocalResources() {
        IWorkspace ws = ResourcesPlugin.getWorkspace();
        IProject[] projects = ws.getRoot().getProjects();
        List<IProject> plist = new ArrayList<IProject>(Arrays.asList(projects));
        final Map<String, ServerResource> localResources = new HashMap<String, ServerResource>();
        for (final IProject p : plist) {
            try {
                String id = p.getPersistentProperty(SafletPersistenceManager.RES_ID_KEY);
                if (StringUtils.isNotBlank(id)) {
                    try {
                        p.setPersistentProperty(SafletPersistenceManager.RES_ID_KEY, null);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
                String lm = p.getPersistentProperty(SafletPersistenceManager.MODIFIED_KEY);
                if (StringUtils.isNotBlank(lm)) {
                    try {
                        p.setPersistentProperty(SafletPersistenceManager.MODIFIED_KEY, null);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
                String lu = p.getPersistentProperty(SafletPersistenceManager.UPDATED_KEY);
                if (StringUtils.isNotBlank(lu)) {
                    try {
                        p.setPersistentProperty(SafletPersistenceManager.UPDATED_KEY, null);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
                p.accept(new IResourceVisitor() {

                    @Override
                    public boolean visit(IResource resource) throws CoreException {
                        if (resource.getType() == IResource.FILE && "saflet".equals(resource.getFileExtension())) {
                            String id = resource.getPersistentProperty(SafletPersistenceManager.RES_ID_KEY);
                            if (StringUtils.isNotBlank(id)) {
                                try {
                                    resource.setPersistentProperty(SafletPersistenceManager.RES_ID_KEY, null);
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                }
                            }
                            String lm = resource.getPersistentProperty(SafletPersistenceManager.MODIFIED_KEY);
                            if (StringUtils.isNotBlank(lm)) {
                                try {
                                    resource.setPersistentProperty(SafletPersistenceManager.MODIFIED_KEY, null);
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                }
                            }
                            String lu = resource.getPersistentProperty(SafletPersistenceManager.UPDATED_KEY);
                            if (StringUtils.isNotBlank(lu)) {
                                try {
                                    resource.setPersistentProperty(SafletPersistenceManager.UPDATED_KEY, null);
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        return true;
                    }
                });
            } catch (CoreException e) {
                e.printStackTrace();
            }
        }
        DriverManager manager = SQLExplorerPlugin.getDefault().getDriverModel();
        Set<DBResource> resources = new HashSet<DBResource>();
        for (ManagedDriver md : manager.getDrivers()) {
            if (md.getDriver() != null) {
                resources.add(md.getDriver());
            }
        }
        AliasManager am = SQLExplorerPlugin.getDefault().getAliasManager();
        for (Alias a : am.getAliases()) {
            if (a.getConnection() != null) {
                resources.add(a.getConnection());
                for (Query q : a.getConnection().getQueries()) {
                    resources.add(q);
                    for (QueryParameter qp : q.getParameters()) resources.add(qp);
                }
            }
        }
        for (DBResource res : resources) {
            res.setId(-1);
            res.setLastModified(null);
            res.setLastUpdated(null);
        }
    }

    public boolean hasConnectedResources() {
        IWorkspace ws = ResourcesPlugin.getWorkspace();
        IProject[] projects = ws.getRoot().getProjects();
        for (final IProject p : projects) {
            try {
                String id = p.getPersistentProperty(SafletPersistenceManager.RES_ID_KEY);
                if (StringUtils.isNotBlank(id)) {
                    return true;
                }
                final boolean[] res = new boolean[] { false };
                p.accept(new IResourceVisitor() {

                    @Override
                    public boolean visit(IResource resource) throws CoreException {
                        if (res[0]) return false;
                        if (resource.getType() == IResource.FILE && "saflet".equals(resource.getFileExtension())) {
                            String id = resource.getPersistentProperty(SafletPersistenceManager.RES_ID_KEY);
                            if (StringUtils.isNotBlank(id) && !StringUtils.equals("-1", id)) {
                                res[0] = true;
                                return false;
                            }
                        }
                        return true;
                    }
                });
                if (res[0]) return true;
            } catch (CoreException e) {
                e.printStackTrace();
            }
        }
        DriverManager manager = SQLExplorerPlugin.getDefault().getDriverModel();
        if (manager != null) for (ManagedDriver md : manager.getDrivers()) {
            if (md.getDriver() != null && md.getDriver().getId() > 0 - 1 && md.getDriver().getLastModified() != null) return true;
        }
        AliasManager am = SQLExplorerPlugin.getDefault().getAliasManager();
        for (Alias a : am.getAliases()) {
            if (a.getConnection() != null) {
                if (a.getConnection().getId() > 0) return true;
                for (Query q : a.getConnection().getQueries()) {
                    if (q.getId() != -1) return true;
                    for (QueryParameter qp : q.getParameters()) if (qp.getId() > 0) return true;
                }
            }
        }
        return false;
    }

    public Map<String, ServerResource> getLocalProjectCopies(final boolean safletsOnly) {
        IWorkspace ws = ResourcesPlugin.getWorkspace();
        IProject[] projects = ws.getRoot().getProjects();
        List<IProject> plist = new ArrayList<IProject>(Arrays.asList(projects));
        final Map<String, ServerResource> localResources = new HashMap<String, ServerResource>();
        for (final IProject p : plist) {
            try {
                final SafletProject[] projResult = new SafletProject[1];
                if (!safletsOnly) {
                    final SafletProject sp = ConfigFactory.eINSTANCE.createSafletProject();
                    String id = p.getPersistentProperty(SafletPersistenceManager.RES_ID_KEY);
                    if (StringUtils.isNotBlank(id)) {
                        try {
                            sp.setId(Integer.valueOf(id));
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                    sp.setLastModified(SafletPersistenceManager.getInstance().getLastModified(p));
                    sp.setLastUpdated(SafletPersistenceManager.getInstance().getLastUpdated(p));
                    sp.setName(p.getName());
                    localResources.put(sp.getName(), sp);
                    projResult[0] = sp;
                }
                p.accept(new IResourceVisitor() {

                    @Override
                    public boolean visit(IResource resource) throws CoreException {
                        if (resource.getType() == IResource.FILE && "saflet".equals(resource.getFileExtension())) {
                            int pid = SafletPersistenceManager.getInstance().getResourceId(resource);
                            Saflet saflet = ConfigFactory.eINSTANCE.createSaflet();
                            saflet.setId(pid);
                            saflet.setLastModified(SafletPersistenceManager.getInstance().getLastModified(resource));
                            saflet.setLastUpdated(SafletPersistenceManager.getInstance().getLastUpdated(resource));
                            saflet.setName(SafletPersistenceManager.getInstance().getSafletName(resource));
                            localResources.put(p.getName() + "/" + saflet.getName(), saflet);
                            if (!safletsOnly) projResult[0].getSaflets().add(saflet);
                        }
                        return true;
                    }
                });
            } catch (CoreException e) {
                e.printStackTrace();
            }
        }
        return localResources;
    }

    public byte[] getLocalSafletCode(Saflet saflet) throws CoreException, IOException {
        IWorkspace ws = ResourcesPlugin.getWorkspace();
        IProject[] projects = ws.getRoot().getProjects();
        SafletProject proj = saflet.getProject();
        IProject project = ws.getRoot().getProject(proj.getName());
        if (project != null) {
            IFile safletFile = project.getFile(saflet.getName() + ".saflet");
            if (safletFile != null) {
                InputStream stream = safletFile.getContents(true);
                return FileUtils.convertStreamToString(stream).getBytes();
            }
        }
        return null;
    }

    public List<SafletProject> getProjectAndSafletTreeCloned(Map<String, ServerResource> localResources) {
        List<SafletProject> projectList = null;
        try {
            projectList = DBManager.getInstance().getProjects();
        } catch (DBManagerException e) {
            e.printStackTrace();
        }
        if (projectList == null) projectList = new ArrayList<SafletProject>();
        List<SafletProject> newProjects = new ArrayList<SafletProject>();
        for (ServerResource res : localResources.values()) {
            if (res instanceof SafletProject) {
                boolean found = false;
                for (SafletProject p : projectList) {
                    if (res.getName().equals(p.getName())) {
                        found = true;
                        addLocalSaflets((SafletProject) res, p);
                        break;
                    }
                }
                if (!found) newProjects.add((SafletProject) res);
            }
        }
        newProjects.addAll(projectList);
        return newProjects;
    }

    private void addLocalSaflets(SafletProject local, SafletProject parent) {
        List<Saflet> toadd = new ArrayList<Saflet>();
        for (Saflet s : local.getSaflets()) {
            boolean found = false;
            for (Saflet s1 : parent.getSaflets()) {
                if (s1.getName().equals(s.getName())) {
                    found = true;
                    break;
                }
            }
            if (!found) toadd.add(s);
        }
        parent.getSaflets().addAll(toadd);
    }

    public ResourceSet getResourceLoader() {
        ResourceSetImpl resourceLoader = new ResourceSetImpl();
        XMIResourceFactoryImpl resourceFactoryImpl = new XMIResourceFactoryImpl();
        resourceLoader.getResourceFactoryRegistry().getExtensionToFactoryMap().put("saflet", resourceFactoryImpl);
        SafletPackage handlerPackage = SafletPackage.eINSTANCE;
        ScriptingPackage scriptingPackage = ScriptingPackage.eINSTANCE;
        DbPackage dbPackage = DbPackage.eINSTANCE;
        resourceLoader.getLoadOptions().put(XMLResource.OPTION_RECORD_UNKNOWN_FEATURE, Boolean.TRUE);
        return resourceLoader;
    }

    public static DynamicValue openSelectSafletDynamicValueDialog(DynamicValue dynamicValue, Shell shell) {
        final Map<String, ServerResource> localResources = SafletPersistenceManager.getInstance().getLocalProjectCopies(false);
        IWorkspace ws = ResourcesPlugin.getWorkspace();
        IProject[] projects = ws.getRoot().getProjects();
        List<IProject> plist = new ArrayList<IProject>(Arrays.asList(projects));
        List<SafletProject> projectList = SafletPersistenceManager.getInstance().getProjectAndSafletTreeCloned(localResources);
        String selectedSaflet = null;
        if (dynamicValue != null) {
            selectedSaflet = dynamicValue.getText();
            dynamicValue = (DynamicValue) EcoreUtil.copy(dynamicValue);
        } else {
            dynamicValue = ActionStepFactory.eINSTANCE.createDynamicValue();
            dynamicValue.setType(DynamicValueType.LITERAL_TEXT);
            dynamicValue.setText("");
        }
        List<ServerResource> list = SelectSafletPanel.openSelectDialog(shell, projectList, localResources, SelectSafletPanel.Mode.SELECT_SAFLET, Collections.singletonList(selectedSaflet));
        if (list != null) {
            Map<SafletProject, IProject> projectToResourceMap = new HashMap<SafletProject, IProject>();
            SafletPersistenceManager.getInstance().prepareProjects(shell, list, projectToResourceMap, plist);
            List<Saflet> saflets = new ArrayList<Saflet>();
            for (ServerResource sr : list) {
                if (sr instanceof Saflet) {
                    saflets.add((Saflet) sr);
                }
            }
            Map<IProject, List<Saflet>> perProjectMap = new HashMap<IProject, List<Saflet>>();
            for (Saflet saflet : saflets) {
                SafletProject proj = saflet.getProject();
                IProject p = projectToResourceMap.get(proj);
                if (p != null) {
                    List<Saflet> sl = perProjectMap.get(p);
                    if (sl == null) {
                        sl = new ArrayList<Saflet>();
                        perProjectMap.put(p, sl);
                    }
                    sl.add(saflet);
                }
            }
            for (Map.Entry<IProject, List<Saflet>> entry : perProjectMap.entrySet()) {
                try {
                    SafletPersistenceManager.getInstance().addOrUpdateSaflets(entry.getKey(), entry.getValue(), false, false);
                } catch (CoreException e) {
                    MessageDialog.openError(SafiWorkshopEditorUtil.getActiveShell(), "Retrieve Error", "Couldn't retrieve Saflet: " + e.getLocalizedMessage());
                    AsteriskDiagramEditorPlugin.getInstance().logError("Couldn't retrieve Saflet", e);
                }
            }
            if (!saflets.isEmpty()) {
                Saflet saflet = saflets.get(0);
                dynamicValue.setText(saflet.getProject().getName() + "/" + saflet.getName());
            }
        }
        return dynamicValue;
    }

    public Set<ActionPak> getActionPaksForUpdate() {
        Set<ActionPak> paks = new HashSet<ActionPak>();
        for (ActionPak pak : AsteriskDiagramEditorPlugin.getInstance().getActionPaks()) {
            ActionPak copy = new ActionPak();
            copy.description = pak.description;
            copy.name = pak.name;
            if (pak.actionPakJars != null) for (ActionPakJar apj : pak.actionPakJars) {
                if (apj.url == null) continue;
                File f = new File(apj.url.getFile());
                boolean needsUpdate = false;
                try {
                    needsUpdate = SafiServerRemoteManager.getInstance().needsUpdate(apj.version, apj.bundleSymbolicName);
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
                if (needsUpdate) {
                    ActionPakJar newjar = new ActionPakJar();
                    newjar.url = apj.url;
                    newjar.bundleSymbolicName = apj.bundleSymbolicName;
                    newjar.version = apj.version;
                    copy.addActionPakJar(newjar);
                    paks.add(copy);
                }
            }
        }
        return paks;
    }

    public List<SafiServerJar> getNewServerJars() {
        List<SafiServerJar> jars = AsteriskDiagramEditorPlugin.getInstance().getServerJars();
        if (jars.isEmpty()) return Collections.emptyList();
        Map<String, SafiServerJar> map = new HashMap<String, SafiServerJar>();
        for (SafiServerJar pak : jars) {
            URL url = pak.url;
            if (url == null) continue;
            File f = new File(url.getFile());
            if (!f.exists()) continue;
            SafiServerJar jl = map.get(pak.bundleSymbolicName);
            if (jl != null) {
                if (Version.parseVersion(jl.version).compareTo(Version.parseVersion(pak.version)) > 0) continue;
            }
            map.put(pak.bundleSymbolicName, pak);
        }
        List<SafiServerJar> result = new ArrayList<SafiServerJar>();
        for (SafiServerJar jar : map.values()) {
            URL url = jar.url;
            if (SafiServerRemoteManager.getInstance().needsUpdate(jar.version, jar.bundleSymbolicName)) result.add(jar);
        }
        return result;
    }

    public boolean transferActionPakJar(String symbolicName, URL url) throws Exception {
        File f = new File(url.getFile());
        if (!f.exists()) return false;
        byte[] data = new byte[(int) f.length()];
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(url.openStream());
            bis.read(data);
            SafiServerRemoteManager.getInstance().transfer(f.getName(), symbolicName, data);
            return true;
        } finally {
            if (bis != null) try {
                bis.close();
            } catch (Exception e) {
            }
        }
    }

    public boolean transferServerJar(String symbolicName, URL url) throws Exception {
        File f = new File(url.getFile());
        if (!f.exists()) return false;
        byte[] data = new byte[(int) f.length()];
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(url.openStream());
            bis.read(data);
            SafiServerRemoteManager.getInstance().transfer(f.getName(), symbolicName, data);
            return true;
        } finally {
            if (bis != null) try {
                bis.close();
            } catch (Exception e) {
            }
        }
    }
}
