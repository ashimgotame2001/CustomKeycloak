package org.example.provider;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Collections;
import java.util.List;

public class CustomBrowserAuthenticatorFactory implements AuthenticatorFactory {
    private static final Logger logger = Logger.getLogger(CustomBrowserAuthenticatorFactory.class);
    public static final String PROVIDER_ID = "branch-form-authenticator";
    private static final CustomUsernamePasswordBranchForm INSTANCE = new CustomUsernamePasswordBranchForm();

    @Override
    public String getDisplayType() {
        return "Username/Password + Branch";
    }

    @Override
    public String getReferenceCategory() {
        return "branch-form";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[]{
                AuthenticationExecutionModel.Requirement.REQUIRED,
                AuthenticationExecutionModel.Requirement.DISABLED
        };
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Validates branch field alongside username/password in browser login.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        logger.info("Creating new instance of CustomBrowserAuthenticator");
        return INSTANCE;
    }

    @Override
    public void init(Config.Scope config) {
        logger.info("Initializing CustomBrowserBranchAuthenticatorFactory");
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        logger.info("Post-initialization of CustomBrowserBranchAuthenticatorFactory");
    }

    @Override
    public void close() {
        logger.info("Closing CustomBrowserBranchAuthenticatorFactory");
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
