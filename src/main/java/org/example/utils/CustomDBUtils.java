package org.example.utils;

import org.example.config.DbConnectorConfig;
import org.example.provider.CustomUserAdapter;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.*;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

import java.sql.*;
import java.util.*;

public class CustomDBUtils {

    private static final Logger logger = Logger.getLogger(CustomDBUtils.class);

    private final KeycloakSession session;
    private final DbConnectorConfig dbConnectorConfig;
    private final ComponentModel model;

    public CustomDBUtils(KeycloakSession session, DbConnectorConfig dbConnectorConfig, ComponentModel model) {
        this.session = session;
        this.dbConnectorConfig = dbConnectorConfig;
        this.model = model;
    }

    /**
     * 🔍 Fetch user from external DB based on a field (e.g., username)
     */
    public UserModel fetchUserFromDatabase(RealmModel realm, String field, String value) {
        logger.infof("🔍 Attempting to fetch user where %s = %s", field, value);

        try (Connection conn = dbConnectorConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE " + field + " = ?")) {

            stmt.setString(1, value);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                logger.warnf("⚠️  No user found in DB where %s = %s", field, value);
                return null;
            }

            UUID userId = rs.getObject("id", UUID.class);
            String username = rs.getString("username");
            String email = rs.getString("email");
            String branch = rs.getString("branch");
            String firstName = rs.getString("first_name");
            String lastName = rs.getString("last_name");

            logger.infof("✅ User found in DB: %s (UUID: %s)", username, userId);

            UserRolesPermissions urp = getUserRolesAndPermissionsFromDB(realm, userId);

            CustomUserAdapter adapter = new CustomUserAdapter(
                    session, realm, model,
                    username, urp.roles, urp.permissions,
                    branch, firstName, lastName
            );

            adapter.setEmail(email);
            adapter.setEnabled(true);

            logger.infof("✅ UserModel successfully created for: %s", username);
            return adapter;

        } catch (Exception e) {
            logger.error("❌ Failed to fetch user from external DB", e);
            throw new RuntimeException("Error fetching user from external DB", e);
        }
    }

    /**
     * 🔍 Fetch user roles and permissions from external DB
     */
    private UserRolesPermissions getUserRolesAndPermissionsFromDB(RealmModel realm, UUID userId) throws SQLException {
        logger.debugf("🔍 Fetching roles & permissions for user UUID: %s", userId);

        Set<RoleModel> roles = new HashSet<>();
        Set<String> permissions = new HashSet<>();

        String sql = """
            SELECT r.name AS role_name, p.name AS permission_name
            FROM roles r
            JOIN user_roles ur ON ur.role_id = r.id
            LEFT JOIN role_permissions rp ON rp.role_id = r.id
            LEFT JOIN permissions p ON p.id = rp.permission_id
            WHERE ur.user_id = ?
        """;

        try (Connection conn = dbConnectorConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String roleName = rs.getString("role_name");
                String permissionName = rs.getString("permission_name");

                RoleModel role = realm.getRole(roleName);
                if (role == null) {
                    role = realm.addRole(roleName);
                    logger.infof("➕ Created missing realm role: %s", roleName);
                }

                roles.add(role);
                logger.debugf("✅ Role loaded: %s", roleName);

                if (permissionName != null) {
                    permissions.add(permissionName);
                    logger.debugf("🔐 Permission added: %s", permissionName);
                }
            }

            logger.infof("✅ Retrieved %d roles and %d permissions for user UUID: %s", roles.size(), permissions.size(), userId);

        } catch (Exception e) {
            logger.error("❌ Failed to fetch roles/permissions from external DB", e);
            throw new RuntimeException("Error loading roles/permissions", e);
        }

        return new UserRolesPermissions(roles, permissions);
    }

    /**
     * 🧠 Utility class for grouping user roles and permissions
     */
    private static class UserRolesPermissions {
        final Set<RoleModel> roles;
        final Set<String> permissions;

        UserRolesPermissions(Set<RoleModel> roles, Set<String> permissions) {
            this.roles = roles;
            this.permissions = permissions;
        }
    }

    /**
     * 🧩 Converts ResultSet row to AbstractUserAdapterFederatedStorage-based UserModel
     */
    public UserModel mapToUserModel(RealmModel realm, ResultSet rs) {
        logger.info("🔄 Starting user mapping from ResultSet to UserModel");

        return new AbstractUserAdapterFederatedStorage(session, realm, model) {

            private final String username;
            private final String email;
            private final String firstName;
            private final String lastName;
            private final String branch;
            private final Set<RoleModel> roleModels;
            private final Set<String> permissions;

            {
                try {
                    logger.debug("📥 Reading user info from ResultSet");

                    username = rs.getString("username");
                    email = rs.getString("email");
                    firstName = rs.getString("first_name");
                    lastName = rs.getString("last_name");
                    branch = rs.getString("branch");
                    UUID userId = rs.getObject("id", UUID.class);

                    logger.debugf("🆔 User ID: %s, Username: %s", userId, username);

                    // Load roles and permissions
                    UserRolesPermissions urp = getUserRolesAndPermissionsFromDB(realm, userId);
                    roleModels = urp.roles != null ? urp.roles : Collections.emptySet();
                    permissions = urp.permissions != null ? urp.permissions : Collections.emptySet();

                    logger.infof("✅ Mapped %d roles and %d permissions", roleModels.size(), permissions.size());

                    for (RoleModel role : roleModels) {
                        logger.debugf("🔗 Granting role: %s", role.getName());
                        grantRole(role);  // Important for admin console visibility
                    }

                    logger.debug("📌 Setting user attributes");
                    setEmail(email);
                    setEnabled(true);
                    setAttribute("firstName", List.of(firstName));
                    setAttribute("lastName", List.of(lastName));
                    setAttribute("branch", List.of(branch));
                    setAttribute("permissions", new ArrayList<>(permissions));

                    logger.info("✅ User mapping completed for: " + username);

                } catch (SQLException e) {
                    logger.error("❌ SQL error while initializing user model", e);
                    throw new RuntimeException("Failed to initialize user fields", e);
                }
            }

            @Override public String getUsername() { return username; }

            @Override public void setUsername(String username) {
                logger.warn("⚠️ setUsername() is not supported in adapter");
            }

            @Override public String getEmail() { return email; }

            @Override public void setEmail(String email) {
                logger.warn("⚠️ setEmail() is not supported in adapter");
            }

            @Override public String getFirstName() { return firstName; }

            @Override public void setFirstName(String firstName) {
                logger.debugf("✏️ Updating firstName: %s", firstName);
                setAttribute("firstName", List.of(firstName));
            }

            @Override public String getLastName() { return lastName; }

            @Override public void setLastName(String lastName) {
                logger.debugf("✏️ Updating lastName: %s", lastName);
                setAttribute("lastName", List.of(lastName));
            }

            @Override
            public Map<String, List<String>> getAttributes() {
                logger.debug("📦 Fetching all federated attributes");
                Map<String, List<String>> attributes = new HashMap<>(super.getAttributes());

                if (branch != null) {
                    attributes.put("branch", List.of(branch));
                }
                if (!permissions.isEmpty()) {
                    attributes.put("permissions", new ArrayList<>(permissions));
                }
                return attributes;
            }

            @Override
            public String getFirstAttribute(String name) {
                switch (name.toLowerCase()) {
                    case "branch": return branch;
                    case "firstname": return firstName;
                    case "lastname": return lastName;
                    case "permissions": return permissions.stream().findFirst().orElse(null);
                    default: return super.getFirstAttribute(name);
                }
            }

            @Override
            public void setAttribute(String name, List<String> values) {
                logger.debugf("📝 Setting attribute: %s = %s", name, values);
                getFederatedStorage().setAttribute(realm, getId(), name, values);
            }
        };
    }
}
