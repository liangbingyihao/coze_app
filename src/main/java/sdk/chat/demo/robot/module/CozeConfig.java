package sdk.chat.demo.robot.module;

import sdk.chat.core.base.BaseNetworkAdapter;
import sdk.chat.demo.robot.handlers.CozeNetworkAdapter;
import sdk.guru.common.BaseConfig;

public class CozeConfig<T> extends BaseConfig<T> {

//    public interface ConnectionConfigProvider {
//        void config(XMPPTCPConnectionConfiguration.Builder builder);
//    }

    public String domain;
    public String hostAddress;
    public int port = 5222;
    public String resource = null;
    //    public boolean xmppSslEnabled;
//    public boolean xmppAcceptAllCertificates;
//    public boolean xmppDisableHostNameVerification;
//    public boolean xmppAllowClientSideAuthentication;
    public boolean compressionEnabled = true;
    public String securityMode = "disabled";
    public int mucMessageHistoryDownloadLimit = 20;
    public int messageHistoryDownloadLimit = 200;
    public boolean debugEnabled = false;
    public int pingInterval = 15;
    public boolean streamManagementEnabled = true;
//    public ConnectionConfigProvider connectionConfigProvider;

    public boolean reciprocalPresenceRequests = true;
//    public Roster.SubscriptionMode subscriptionMode = Roster.SubscriptionMode.accept_all;

    public boolean saveNameToVCardNickname = true;

//    public List<Presence.Mode> onlinePresenceModes = new ArrayList<Presence.Mode>() {{
//        add(Presence.Mode.chat);
//        add(Presence.Mode.available);
//        add(Presence.Mode.dnd);
//        add(Presence.Mode.away);
//        add(Presence.Mode.xa);
//    }};

    public boolean allowServerConfiguration = true;

    public boolean notifyForNewMamMessages = false;

    public boolean sendSystemMessageForRoleChange = true;
    public boolean sendSystemMessageForAffiliationChange = false;

    public Class<? extends BaseNetworkAdapter> networkAdapter = CozeNetworkAdapter.class;

    public CozeConfig(T onBuild) {
        super(onBuild);
    }

    public CozeConfig<T> setXMPP(String hostAddress) {
        return setXMPP(hostAddress, 0);
    }

    public CozeConfig<T> setXMPP(String hostAddress, String domain) {
        return setXMPP(hostAddress, domain, 0);
    }

    public CozeConfig<T> setXMPP(String hostAddress, int port) {
        return setXMPP(hostAddress, hostAddress, port);
    }

    public CozeConfig<T> setXMPP(String hostAddress, String domain, int port) {
        return setXMPP(hostAddress, domain, port, null);
    }

    /**
     * Set the XMPP server details
     * @param hostAddress XMPP server address
     * @param domain XMPP server domain
     * @param port
     * @param resource
     * @return
     */
    public CozeConfig<T> setXMPP(String hostAddress, String domain, int port, String resource) {
        this.hostAddress = hostAddress;
        this.domain = domain;
        if (port != 0) {
            this.port = port;
        }
        this.resource = resource;
        return this;
    }

//    public XMPPConfig<T> xmppSslEnabled(boolean sslEnabled) {
//        this.xmppSslEnabled = sslEnabled;
//        return this;
//    }

    /**
     * How many historic messages to load when joining a multi-user chat
     * @param limit
     * @return
     */
    public CozeConfig<T> setMucMessageHistoryDownloadLimit(int limit) {
        this.mucMessageHistoryDownloadLimit = limit;
        return this;
    }

    /**
     * How many historic 1-to-1 messages to load
     * @param limit
     * @return
     */
    public CozeConfig<T> setMessageHistoryDownloadLimit(int limit) {
        this.messageHistoryDownloadLimit = limit;
        return this;
    }

//    public XMPPConfig<T> xmppDisableHostNameVerification(boolean disableHostNameVerification) {
//        this.xmppDisableHostNameVerification = disableHostNameVerification;
//        return this;
//    }

    /**
     * This setting is not currently implemented
     *
     * @param allowClientSideAuthentication
     * @return
     */
//    public XMPPConfig<T> xmppAllowClientSideAuthentication(boolean allowClientSideAuthentication) {
//        this.xmppAllowClientSideAuthentication = allowClientSideAuthentication;
//        return this;
//    }
//

    /**
     * Is XMPP Compression enabled
     * @param compressionEnabled
     * @return
     */
    public CozeConfig<T> setCompressionEnabled(boolean compressionEnabled) {
        this.compressionEnabled = compressionEnabled;
        return this;
    }

    //
//
//    public XMPPConfig<T> xmppAcceptAllCertificates(boolean acceptAllCertificates) {
//        this.xmppAcceptAllCertificates = acceptAllCertificates;
//        return this;
//    }

    /**
     * Set TSL security mode. Allowable values are
     * "required"
     * "ifpossible"
     * "disabled"
     *
     * @param securityMode
     * @return
     */
    public CozeConfig<T> setSecurityMode(String securityMode) {
        this.securityMode = securityMode;
        return this;
    }

    /**
     * Enable debug mode
     * @param debugEnabled
     * @return
     */
    public CozeConfig<T> setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
        return this;
    }

    /**
     * Enable or disable stream management
     * @param enabled
     * @return
     */
    public CozeConfig<T> setStreamManagementEnabled(boolean enabled) {
        this.streamManagementEnabled = streamManagementEnabled;
        return this;
    }

    /**
     * Ping interval in seconds
     * @param interval
     * @return
     */
    public CozeConfig<T> setPingInterval(int interval) {
        this.pingInterval = pingInterval;
        return this;
    }

    /**
     * Allow the user to define a custom server
     * @param allowServerConfiguration
     * @return
     */
    public CozeConfig<T> setAllowServerConfiguration(boolean allowServerConfiguration) {
        this.allowServerConfiguration = allowServerConfiguration;
        return this;
    }

    /**
     * Should the user's name be saved to the nickname field in the vCard
     * @param value
     * @return
     */
    public CozeConfig<T> setSaveNameToVCardNickname(boolean value) {
        this.saveNameToVCardNickname = value;
        return this;
    }

    /**
     * Override the Firebase network adapter class
     * @param networkAdapter
     * @return
     */
    public CozeConfig<T> setNetworkAdapter(Class<? extends BaseNetworkAdapter> networkAdapter) {
        this.networkAdapter = networkAdapter;
        return this;
    }

    /**
     * Should we send a local message when our role changes
     * @param sendSystemMessageForRoleChange
     * @return
     */
    public CozeConfig<T> setSendSystemMessageForRoleChange(boolean sendSystemMessageForRoleChange) {
        this.sendSystemMessageForRoleChange = sendSystemMessageForRoleChange;
        return this;
    }

    /**
     * Should we send a local message when our affiliation changes
     * @param sendSystemMessageForAffiliationChange
     * @return
     */
    public CozeConfig<T> setSendSystemMessageForAffiliationChange(boolean sendSystemMessageForAffiliationChange) {
        this.sendSystemMessageForAffiliationChange = sendSystemMessageForAffiliationChange;
        return this;
    }

    /**
     * Customize the XMPP connection
     * @return
     */
//    public CozeConfig<T> setConnectionConfigProvider(ConnectionConfigProvider provider) {
//        this.connectionConfigProvider = provider;
//        return this;
//    }

//    public CozeConfig<T> setOnlinePresenceModes(List<Presence.Mode> modes) {
//        this.onlinePresenceModes = modes;
//        return this;
//    }
//
//    public CozeConfig<T> setSubscriptionMode(Roster.SubscriptionMode mode) {
//        this.subscriptionMode = mode;
//        return this;
//    }

    public CozeConfig<T> setReciprocalPresenceRequest(boolean value) {
        this.reciprocalPresenceRequests = value;
        return this;
    }

    public CozeConfig<T> setNotifyForNewMamMessages(boolean value) {
        this.notifyForNewMamMessages = value;
        return this;
    }
}
