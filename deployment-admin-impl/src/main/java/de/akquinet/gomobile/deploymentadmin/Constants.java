package de.akquinet.gomobile.deploymentadmin;

public interface Constants extends org.osgi.framework.Constants {

    // manifest main attribute header constants
    public static final String DEPLOYMENTPACKAGE_SYMBOLICMAME = "DeploymentPackage-SymbolicName";
    public static final String DEPLOYMENTPACKAGE_VERSION = "DeploymentPackage-Version";
    public static final String DEPLOYMENTPACKAGE_FIXPACK = "DeploymentPackage-FixPack";

    // manifest 'name' section header constants
    public static final String RESOURCE_PROCESSOR = "Resource-Processor";
    public static final String DEPLOYMENTPACKAGE_MISSING = "DeploymentPackage-Missing";
    public static final String DEPLOYMENTPACKAGE_CUSTOMIZER = "DeploymentPackage-Customizer";

    // event topics and properties
    public static final String EVENTTOPIC_INSTALL = "org/osgi/service/deployment/INSTALL";
    public static final String EVENTTOPIC_UNINSTALL = "org/osgi/service/deployment/UNINSTALL";
    public static final String EVENTTOPIC_COMPLETE = "org/osgi/service/deployment/COMPLETE";
    public static final String EVENTPROPERTY_DEPLOYMENTPACKAGE_NAME = "deploymentpackage.name";
    public static final String EVENTPROPERTY_SUCCESSFUL = "successful";

    // miscellaneous constants
    public static final String BUNDLE_LOCATION_PREFIX = "osgi-dp:";
}