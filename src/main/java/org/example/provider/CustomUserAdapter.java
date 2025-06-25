package org.example.provider;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

import java.util.*;

public class CustomUserAdapter extends AbstractUserAdapterFederatedStorage {

    private final Set<RoleModel> roleModels;
    private final Set<String> permissions;
    private final String username;
    private final String branch;
    private final String firstName;
    private final String lastName;

    public CustomUserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model,
                             String username, Set<RoleModel> roles, Set<String> permissions,
                             String branch, String firstName, String lastName) {
        super(session, realm, model);
        this.roleModels = roles;
        this.permissions = permissions;
        this.username = username;
        this.branch = branch;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setUsername(String username) {

    }

    @Override
    public boolean hasRole(RoleModel role) {
        return roleModels.contains(role);
    }


    @Override
    public Map<String, List<String>> getAttributes() {
        Map<String, List<String>> attrs = new HashMap<>(super.getAttributes());

        if (branch != null && !branch.isEmpty()) attrs.put("branch", List.of(branch));
        if (firstName != null && !firstName.isEmpty()) attrs.put("firstName", List.of(firstName));
        if (lastName != null && !lastName.isEmpty()) attrs.put("lastName", List.of(lastName));
        if (!permissions.isEmpty()) attrs.put("permissions", new ArrayList<>(permissions));

        return attrs;
    }

    @Override
    public String getFirstAttribute(String name) {
        switch (name.toLowerCase()) {
            case "branch":
                return branch;
            case "firstname":
                return firstName;
            case "lastname":
                return lastName;
            case "permissions":
                return permissions.isEmpty() ? null : permissions.iterator().next();
            default:
                return super.getFirstAttribute(name);
        }
    }
}
