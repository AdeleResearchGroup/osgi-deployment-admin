package de.akquinet.gomobile.deployment.mojo;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.plugin.MojoExecutionException;

import de.akquinet.gomobile.deployment.api.DeploymentPackage;
import de.akquinet.gomobile.deployment.api.internals.Store;
import de.akquinet.gomobile.deployment.api.internals.StreamUtils;
import de.akquinet.gomobile.deployment.mojo.util.FileUtil;
import de.akquinet.gomobile.deployment.mojo.util.ManifestBuilder;

/**
 * <p>
 * From the Deployment Admin Specification:
 * </p>
 * <p>
 * A Deployment Package consists of installable <i>resources</i>. Resources are
 * described in the <i>Name sections</i> of the Manifest. They are stored in the
 * JAR file under a path. This path is called the <i>resource id</i>. Subsets of
 * these resources are the bundles. Bundles are treated differently from the
 * other resources by the Deployment Admin service.
 * </p>
 */
public class BundleResource implements Resource {

    private String m_groupId;

    private String m_artifactId;

    private String m_version;
    
    private String m_classifier;

    private File m_resolvedFile;

    private String m_targetPath;

    private de.akquinet.gomobile.deployment.api.BundleResource m_bundle;

    private DeploymentPackageMojo m_mojo;
    
    /**
     * The header entries.
     */
    private List<Header> m_headerEntries = null;

    /**
     * Constructor which initializes the instance of the {@link BundleResource}.
     */
    public BundleResource() {
        m_groupId = null;
        m_artifactId = null;
        m_version = null;
        m_targetPath = null;
        m_mojo = null;
        m_headerEntries = new ArrayList<Header>();

        m_bundle = new de.akquinet.gomobile.deployment.api.BundleResource();
    }

    public final void setMojo(DeploymentPackageMojo mojo) {
        m_mojo = mojo;
    }

    public void resolve(DeploymentPackage dp, File baseDir) throws MojoExecutionException {
        if (m_resolvedFile == null) {
            m_resolvedFile = m_mojo.resolveResource(m_groupId, m_artifactId,
                    m_version, m_classifier);
            try {
                m_bundle.setURL(m_resolvedFile.toURI().toURL());
            } catch (Exception e) {
                throw new MojoExecutionException(
                        "Cannot compute the bundle url : " + e.getMessage());
            }
        }
        
        final List<Header> addedHeaders = getHeaders();
		if ((addedHeaders != null) && (!addedHeaders.isEmpty())) {
			try {
				// modify manifest file to add header values
				JarFile bundleFile = new JarFile(m_resolvedFile);
				Manifest manifest = bundleFile.getManifest();
				ManifestBuilder mfBuilder = new ManifestBuilder();
				for (Header header : addedHeaders) {
					if (!(header instanceof BundleHeader))
						continue;
					
					mfBuilder.addHeader((BundleHeader) header);
				}

				// modify manifest
				Manifest manipulatedMf = mfBuilder.build(manifest);
				File manipulatedMfFile = File.createTempFile("mf_", ".mf");
				FileOutputStream mfFos = new FileOutputStream(manipulatedMfFile);
				manipulatedMf.write(mfFos);
				mfFos.flush();
				mfFos.close();

				File manipulatedBundleFile = File.createTempFile("bundle_",
						".jar");
				if (manipulatedBundleFile == null)
					throw new MojoExecutionException("Cannot create temp file");

				FileUtil.copyBundleFile(bundleFile, manipulatedBundleFile,
						manipulatedMfFile);

				m_bundle.setURL(manipulatedBundleFile.toURI().toURL());
			} catch (Exception e) {
				throw new MojoExecutionException(
						"Cannot manipulate manifest file of the original bundle url : " + e.getMessage());
			}
		}

        String resourceId = m_resolvedFile.getName();
        if (m_targetPath != null && m_targetPath.length() > 0) {
            resourceId = m_targetPath + "/" + resourceId;
        }

        m_bundle.setPath(resourceId);

        // Add the resource.
        dp.addBundle(m_bundle);
    }

    /**
     * @return the path and the name of the resource
     * @see de.akquinet.gomobile.deployment.mojo.Resource#getResourceId()
     */
    public final String getResourceId() {
        return m_bundle.getName();
    }

    /**
     * @return the targetPath
     */
    public final String getTargetPath() {
        return m_targetPath;
    }

    /**
     * @param path the targetPath to set
     */
    public final void setTargetPath(final String path) {
        m_targetPath = path;
    }

    /**
     * @return the customizer
     */
    public final boolean isCustomizer() {
        return m_bundle.isCustomizer();
    }

    /**
     * @param customizer the customizer to set
     */
    public final void setCustomizer(final boolean customizer) {
        m_bundle.setCustomizer(customizer);
    }

    public final boolean getMissing() {
        return m_bundle.isMissing();
    }

    public final void setMissing(final boolean missing) {
        m_bundle.setMissing(missing);
    }

    /**
     * @return the groupId
     */
    public final String getGroupId() {
        return m_groupId;
    }

    /**
     * @param group the groupId to set
     */
    public final void setGroupId(final String group) {
        m_groupId = group;
    }
    
    /**
     * @return the classifier
     */
    public final String getClassifier() {
        return m_classifier;
    }

    /**
     * @param group the groupId to set
     */
    public final void setClassifier(final String classifier) {
        m_classifier = classifier;
    }

    /**
     * @return the artifactId
     */
    public final String getArtifactId() {
        return m_artifactId;
    }

    /**
     * @param artifact the artifactId to set
     */
    public final void setArtifactId(final String artifact) {
        m_artifactId = artifact;
    }

    /**
     * @return the version
     */
    public final String getVersion() {
        return m_version;
    }

    /**
     * @param version the version to set
     */
    public final void setVersion(final String version) {
        m_version = version;
    }
    
    /**
     * @return the header entries
     */
    public final List<Header> getHeaders() {
        return m_headerEntries;
    }
    
    /**
     * @param resources the resources to set
     */
    public final void setHeaders(final List<Header> headerEntries) {
        // Not processed yet...
        m_headerEntries = headerEntries;
    }

    /**
     * @return the resolvedFile
     * @throws MojoExecutionException
     */
    public final File getResolvedFile() throws MojoExecutionException {
        if (m_resolvedFile == null) {
        	//System.out.println("MOJO : " + m_mojo);
        	System.out.println("Artifact : " + m_groupId + ":" + m_artifactId + ":" + m_version);

            m_resolvedFile = m_mojo.resolveResource(m_groupId, m_artifactId,
                    m_version, m_classifier);
            try {
                m_bundle.setURL(m_resolvedFile.toURI().toURL());
            } catch (Exception e) {
                throw new MojoExecutionException(e.getMessage());
            }
        }
        return m_resolvedFile;
    }

    public Object getResource() {
        return m_bundle;
    }

    /**
     * @see java.lang.Object#toString()
     * @return a string representation of a {@link BundleResource}.
     */
    public final String toString() {

        String rf = null;
        try {
            rf = getResolvedFile().getAbsolutePath();
        } catch (Throwable e) {
           // Silently ignore the exception.
        }

        final StringBuffer buffer = new StringBuffer(this.getClass().getName());
        buffer.append("[");
        buffer.append("groupId:");
        buffer.append(getGroupId());
        buffer.append(", ");
        buffer.append("artifactId:");
        buffer.append(getArtifactId());
        buffer.append(", ");
        buffer.append("version:");
        buffer.append(getVersion());
        buffer.append(", ");
        buffer.append("resolvedFile:");
        buffer.append(rf);
        buffer.append("]");
        return buffer.toString();
    }


}
