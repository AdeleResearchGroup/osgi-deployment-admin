package de.akquinet.gomobile.deploymentadmin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Manifest;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.deploymentadmin.BundleInfo;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.osgi.service.deploymentadmin.spi.ResourceProcessor;

/**
 * Base class for various types of deployment packages. Indifferent in regard to how the
 * deployment package data is obtained, this should be handled by extending classes.
 */
public abstract class AbstractDeploymentPackage implements DeploymentPackage {

    private final BundleContext m_bundleContext;
    private final DeploymentPackageManifest m_manifest;
    private final Map m_nameToBundleInfo = new HashMap();
    private final Map m_pathToEntry = new HashMap();
    private final BundleInfoImpl[] m_bundleInfos;
    private final ResourceInfoImpl[] m_resourceInfos;
    private final String[] m_resourcePaths;
    private final boolean m_isFixPackage;
    private final DeploymentAdminImpl m_admin;
    private boolean m_isStale;
    protected static final AbstractDeploymentPackage emptyPackage = new AbstractDeploymentPackage() {
        public String getHeader(String header) {
            if (Constants.DEPLOYMENTPACKAGE_SYMBOLICMAME.equals(header)) { return ""; }
            else if (Constants.DEPLOYMENTPACKAGE_VERSION.equals(header)) { return Version.emptyVersion.toString(); }
            else { return null; }
        }
        public Bundle getBundle(String symbolicName) { return null; }
        public BundleInfo[] getBundleInfos() { return new BundleInfoImpl[] {}; }
        public BundleInfoImpl[] getBundleInfoImpls() { return new BundleInfoImpl[] {}; }
        public String getName() { return ""; }
        public String getResourceHeader(String resource, String header) { return null; }
        public ServiceReference getResourceProcessor(String resource) { return null; }
        public String[] getResources() { return new String[] {}; }
        public Version getVersion() { return Version.emptyVersion; }
        public boolean isStale() { return true; }
        public void uninstall() throws DeploymentException { throw new IllegalStateException("Can not uninstall stale DeploymentPackage"); }
        public boolean uninstallForced() throws DeploymentException { throw new IllegalStateException("Can not uninstall stale DeploymentPackage"); }
        public InputStream getBundleStream(String symbolicName) throws IOException { return null; }
        public BundleInfoImpl[] getOrderedBundleInfos() { return new BundleInfoImpl[] {}; }
        public ResourceInfoImpl[] getOrderedResourceInfos() { return new ResourceInfoImpl[] {}; }
        public InputStream getCurrentEntryStream() { throw new UnsupportedOperationException(); }
        public AbstractInfo getNextEntry() throws IOException { throw new UnsupportedOperationException(); }
        public String getDisplayName() { return null; }
        public URL getIcon() { return null;	}
    };

    /* Constructor only for use by the emptyPackage static variable */
    private AbstractDeploymentPackage() {
        m_bundleContext = null;
        m_manifest = null;
        m_bundleInfos = null;
        m_resourceInfos = null;
        m_resourcePaths = null;
        m_isFixPackage = false;
        m_admin = null;
    }

    /**
     * Creates an instance of this class.
     *
     * @param manifest The manifest of the deployment package.
     * @param bundleContext The bundle context.
     * @param admin the deployment admin impl
     * @throws DeploymentException Thrown if the specified manifest does not describe a valid deployment package.
     */
    public AbstractDeploymentPackage(Manifest manifest, BundleContext bundleContext, DeploymentAdminImpl admin) throws DeploymentException {
        m_manifest = new DeploymentPackageManifest(manifest);
        m_admin = admin;
        m_isFixPackage = m_manifest.getFixPackage() != null;
        m_bundleContext = bundleContext;
        m_bundleInfos = (BundleInfoImpl[]) m_manifest.getBundleInfos().toArray(new BundleInfoImpl[0]);
        for(int i = 0; i < m_bundleInfos.length; i++) {
            m_nameToBundleInfo.put(m_bundleInfos[i].getSymbolicName(), m_bundleInfos[i]);
            m_pathToEntry.put(m_bundleInfos[i].getPath(), m_bundleInfos[i]);
        }
        m_resourceInfos =  (ResourceInfoImpl[]) m_manifest.getResourceInfos().toArray(new ResourceInfoImpl[0]);
        for (int i = 0; i < m_resourceInfos.length; i++) {
            m_pathToEntry.put(m_resourceInfos[i].getPath(), m_resourceInfos[i]);
        }
        m_resourcePaths = (String[]) m_pathToEntry.keySet().toArray(new String[m_pathToEntry.size()]);
    }

    public Bundle getBundle(String symbolicName) {
        if (isStale()) {
            throw new IllegalStateException("Can not get bundle from stale deployment package.");
        }
        if (m_nameToBundleInfo.containsKey(symbolicName)) {
            Bundle[] bundles = m_bundleContext.getBundles();
            for (int i = 0; i < bundles.length; i++) {
                String sn = bundles[i].getSymbolicName();
                // OSGi R3 bundles do not have a symbolic name.
                if (sn != null && sn.equals(symbolicName)) {
                        return bundles[i];
                }
            }
        }
        return null;
    }

    public BundleInfo[] getBundleInfos() {
        return (BundleInfo[]) m_bundleInfos.clone();
    }

    /**
     * Returns the bundles of this deployment package as an array of <code>BundleInfoImpl</code> objects.
     *
     * @return Array containing <code>BundleInfoImpl</code> objects for each bundle this deployment package.
     */
    public BundleInfoImpl[] getBundleInfoImpls() {
        return (BundleInfoImpl[]) m_bundleInfos.clone();
    }

    /**
     * Returns the processed resources of this deployment package as an array of <code>ResourceInfoImpl</code> objects.
     *
     * @return Array containing <code>ResourceInfoImpl</code> objects for each processed resource of this deployment package.
     */
    public ResourceInfoImpl[] getResourceInfos() {
        return (ResourceInfoImpl[]) m_resourceInfos.clone();
    }

    /**
     * Determines whether this deployment package is a fix package.
     *
     * @return True if this deployment package is a fix package, false otherwise.
     */
    public boolean isFixPackage() {
        return m_isFixPackage;
    }

    public String getHeader(String header) {
        return m_manifest.getHeader(header);
    }

    public String getName() {
        return m_manifest.getSymbolicName();
    }

    public String getDisplayName() {
        return m_manifest.getHeader("DeploymentPackage-Name");

    }

    public URL getIcon() {
        String url = m_manifest.getHeader("DeploymentPackage-Icon");
        if (url == null) {
            return null;
        } else {
            // Do a local copy
            try {
                URL u = new URL(url);
                File dest = m_bundleContext.getDataFile(getName() + "-icon");
                if (dest == null) {
                    return null;
                }
                write(u.openStream(), new FileOutputStream(dest));
                return dest.toURI().toURL();
            } catch (IOException e) {
                //TODO Log This
                return null;
            }
        }
    }

    public static void write(InputStream in, OutputStream out) throws IOException {
        byte[] b = new byte[4096];
        for (int n; (n = in.read(b)) != -1;) {
            out.write(b, 0, n);
        }
        in.close();
        out.close();
    }



    public String getResourceHeader(String resource, String header) {
        AbstractInfo info = (AbstractInfo) m_pathToEntry.get(resource);
        if (info != null) {
            return info.getHeader(header);
        }
        return null;
    }


    public boolean isResourceProcessorProvidedByTheDeploymentPackage(ServiceReference ref) {
        if (ref == null) {
            return false;
        }

        Bundle bundle = ref.getBundle();
        if (bundle.getSymbolicName() == null) {
            return false;
        }

        BundleInfoImpl bi = (BundleInfoImpl) m_nameToBundleInfo.get(bundle.getSymbolicName());

        return bi != null  && bi.isCustomizer();
    }

    public ServiceReference getResourceProcessor(String resource) {
        if (isStale()) {
            throw new IllegalStateException("Can not get bundle from stale deployment package.");
        }
        String processor = getResourceProcessorName(resource);
        if (processor != null) {
            try {
                ServiceReference[] services = m_bundleContext.getServiceReferences(ResourceProcessor.class.getName(), "(" + org.osgi.framework.Constants.SERVICE_PID + "=" + processor + ")");
            if (services != null && services.length > 0) {
                return services[0];
            }
            else {
                return null;
            }

        }catch (InvalidSyntaxException e) {
            return null;
            }
        }
        return null;
    }

    /**
     * Retrieves the processor pid
     * @param resource the resource name.
     * @return the resource processor pid
     */
    public String getResourceProcessorName(String resource){
        AbstractInfo info = (AbstractInfo) m_pathToEntry.get(resource);
        if (info instanceof ResourceInfoImpl) {
            String processor = ((ResourceInfoImpl) info).getResourceProcessor();
            return processor;
        }
        return null;
    }

    public String[] getResources() {
        return (String[]) m_resourcePaths.clone();
    }

    public Version getVersion() {
        return m_manifest.getVersion();
    }

    /**
     * If this deployment package is a fix package this method determines the version range this deployment package can be applied to.
     *
     * @return <code>VersionRange</code> the fix package can be applied to or <code>null</code> if it is not a fix package.
     */
    public VersionRange getVersionRange() {
        return m_manifest.getFixPackage();
    }

    public synchronized boolean isStale() {
        return m_isStale;
    }

    public void uninstall() throws DeploymentException {
        if (isStale()) {
            throw new IllegalStateException("Can not uninstall a stale deployment package.");
        }

        m_admin.uninstall(this, false);

        synchronized (this) {
            m_isStale = true;
        }

    }

    public boolean uninstallForced() throws DeploymentException {
        if (isStale()) {
            throw new IllegalStateException("Can not uninstall a stale deployment package.");
        }

        boolean success = m_admin.uninstall(this, true);

        synchronized (this) {
            m_isStale = true;
        }
        return success;
    }

    /**
     * Determines the bundles of this deployment package in the order in which they were originally received.
     *
     * @return Array containing <code>BundleInfoImpl</code> objects of the bundles in this deployment package, ordered in the way they appeared when the deployment package was first received.
     */
    public abstract BundleInfoImpl[] getOrderedBundleInfos();

    /**
     * Determines the resources of this deployment package in the order in which they were originally received.
     *
     * @return Array containing <code>ResourceInfoImpl</code> objects of all processed resources in this deployment package, ordered in the way they appeared when the deployment package was first received
     */
    public abstract ResourceInfoImpl[] getOrderedResourceInfos();

    /**
     * Determines the info about a processed resource based on it's path/resource-id.
     *
     * @param path String containing a (processed) resource path
     * @return <code>ResourceInfoImpl</code> for the resource identified by the specified path or null if the path is unknown or does not describe a processed resource
     */
    public ResourceInfoImpl getResourceInfoByPath(String path) {
        AbstractInfo info = (AbstractInfo) m_pathToEntry.get(path);
        if (info instanceof ResourceInfoImpl) {
            return (ResourceInfoImpl) info;
        }
        return null;
    }

    /**
     * Determines the info about either a bundle or processed resource based on it's path/resource-id.
     *
     * @param path String containing a resource path (either bundle or processed resource)
     * @return <code>AbstractInfoImpl</code> for the resource identified by the specified path or null if the path is unknown
     */
    protected AbstractInfo getAbstractInfoByPath(String path) {
        return (AbstractInfo) m_pathToEntry.get(path);
    }

    /**
     * Determines the info about a bundle based on it's path/resource-id.
     *
     * @param path String containing a bundle path
     * @return <code>BundleInfoImpl</code> for the bundle resource identified by the specified path or null if the path is unknown or does not describe a bundle resource
     */
    public BundleInfoImpl getBundleInfoByPath(String path) {
        AbstractInfo info = (AbstractInfo) m_pathToEntry.get(path);
        if (info instanceof BundleInfoImpl) {
            return (BundleInfoImpl) info;
        }
        return null;
    }

    /**
     * Determines the info about a bundle resource based on the bundle symbolic name.
     *
     * @param symbolicName String containing a bundle symbolic name
     * @return <code>BundleInfoImpl</code> for the bundle identified by the specified symbolic name or null if the symbolic name is unknown
     */
    public BundleInfoImpl getBundleInfoByName(String symbolicName) {
        return (BundleInfoImpl) m_nameToBundleInfo.get(symbolicName);
    }

    /**
     * Determines the data stream of a bundle resource based on the bundle symbolic name
     *
     * @param symbolicName Bundle symbolic name
     * @return Stream to the bundle identified by the specified symbolic name or null if no such bundle exists in this deployment package.
     * @throws IOException If the bundle can not be properly offered as an inputstream
     */
    public abstract InputStream getBundleStream(String symbolicName) throws IOException;

    /**
     * Determines the next resource entry in this deployment package based on the order in which the resources appeared when the package was originally received.
     *
     * @return <code>AbstractInfo</code> describing the next resource entry (as determined by the order in which the deployment package was received originally) or null if there is no next entry
     * @throws IOException if the next entry can not be properly determined
     */
    public abstract AbstractInfo getNextEntry() throws IOException;

    /**
     * Determines the data stream to the current entry of this deployment package, use this together with the <code>getNextEntry</code> method.
     *
     * @return Stream to the current resource in the deployment package (as determined by the order in which the deployment package was received originally) or null if there is no entry
     */
    public abstract InputStream getCurrentEntryStream();

}

