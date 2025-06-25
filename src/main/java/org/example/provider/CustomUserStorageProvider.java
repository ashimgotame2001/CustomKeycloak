package org.example.provider;

import org.example.config.DbConnectorConfig;
import org.example.utils.CustomDBUtils;
import org.jboss.logging.Logger;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class CustomUserStorageProvider implements UserStorageProvider, UserLookupProvider, CredentialInputValidator, UserQueryProvider {

    private final DbConnectorConfig dbConnectorConfig;
    private final CustomDBUtils cloakUtils;
    private static final Logger logger = Logger.getLogger(CustomUserStorageProvider.class);

    public CustomUserStorageProvider(DbConnectorConfig dbConnectorConfig, CustomDBUtils cloakUtils) {
        this.dbConnectorConfig = dbConnectorConfig;
        this.cloakUtils = cloakUtils;
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        logger.infof("🔍 [getUserByUsername] Realm: %s | Username: %s", realm.getName(), username);
        return cloakUtils.fetchUserFromDatabase(realm, "username", username);
    }

    @Override
    public UserModel getUserById(RealmModel realm, String storageId) {
        StorageId sid = new StorageId(storageId);
        logger.infof("🔍 [getUserById] Realm: %s | StorageId: %s", realm.getName(), storageId);
        return getUserByUsername(realm, sid.getExternalId());
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        logger.infof("🔍 [getUserByEmail] Realm: %s | Email: %s", realm.getName(), email);
        return cloakUtils.fetchUserFromDatabase(realm, "email", email);
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return supportsCredentialType(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        logger.infof("🔐 [isValid] Validating password for user: %s", user.getUsername());

        if (!supportsCredentialType(input.getType())) {
            logger.warnf("⚠️ [isValid] Unsupported credential type: %s", input.getType());
            return false;
        }

        String username = user.getUsername();
        String rawPassword = input.getChallengeResponse();

        logger.debugf("📥 [isValid] Input password for user %s: %s", username, rawPassword);

        try (Connection conn = dbConnectorConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT password FROM users WHERE username = ?")) {

            logger.debug("📦 [isValid] Preparing SQL statement");
            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                logger.debug("🔍 [isValid] Executing password query");

                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    logger.debugf("🔐 [isValid] Stored password for %s: %s", username, storedPassword);

                    boolean matches = storedPassword.equals(rawPassword);
                    logger.infof("✅ [isValid] Password match for user %s: %s", username, matches);

                    return matches;
                } else {
                    logger.warnf("❌ [isValid] User not found: %s", username);
                }
            }
        } catch (Exception e) {
            logger.errorf(e, "❌ [isValid] Error while validating password for user: %s", username);
        }

        logger.infof("❌ [isValid] Returning false for user: %s", username);
        return false;
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
        String search = params.getOrDefault(UserModel.INCLUDE_SERVICE_ACCOUNT, "");
        logger.infof("🔍 [searchForUserStream] search=%s | firstResult=%d | maxResults=%d", search, firstResult, maxResults);

        try (Connection connection = dbConnectorConfig.getConnection();
             PreparedStatement stmt = connection.prepareStatement("SELECT * FROM users")) {
            ResultSet rs = stmt.executeQuery();
            logger.info("✅ [searchForUserStream] Query executed, mapping ResultSet to UserModel stream");
            return mapToUserModelStream(realm, rs);
        } catch (Exception e) {
            logger.error("❌ [searchForUserStream] Error searching for users", e);
            return Stream.empty();
        }
    }

    private Stream<UserModel> mapToUserModelStream(RealmModel realm, ResultSet rs) {
        logger.info("🔁 [mapToUserModelStream] Mapping ResultSet to UserModel list");
        List<UserModel> list = new ArrayList<>();

        try {
            while (rs.next()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                StringBuilder rowLog = new StringBuilder("📊 Row Data: ");

                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = rs.getObject(i);
                    rowLog.append(String.format("%s=%s, ", columnName, value));
                }

                logger.info(rowLog.toString());
                list.add(cloakUtils.mapToUserModel(realm, rs));
            }
        } catch (Exception e) {
            logger.error("❌ [mapToUserModelStream] Error mapping ResultSet", e);
        }

        return list.stream();
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        logger.info("ℹ️ [getGroupMembersStream] Not implemented");
        return Stream.empty();
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        logger.infof("ℹ️ [searchForUserByUserAttributeStream] Not implemented | attr: %s = %s", attrName, attrValue);
        return Stream.empty();
    }

    @Override
    public void close() {
        logger.info("🛑 [close] Cleaning up CustomUserStorageProvider (no-op)");
    }
}
