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

/**
 * Factory class for CustomBrowserAuthenticatorWithBranch.
 *
 * Registers the provider under the ID `branch-form-authenticator`.
 */
public class CustomBrowserAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "branch-form-authenticator";

    private static final Logger logger = Logger.getLogger(CustomBrowserAuthenticatorFactory.class);
    private static final CustomBrowserAuthenticatorWithBranch SINGLETON = new CustomBrowserAuthenticatorWithBranch();

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public String getDisplayType() {
        return "Username Password Branch Form";
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
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Custom authenticator that includes a branch field in the login form.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList(); // Could be extended later
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        logger.debug("Creating instance of CustomBrowserAuthenticatorWithBranch");
        return SINGLETON;
    }

    @Override
    public void init(Config.Scope config) {
        logger.debug("Initializing CustomBrowserAuthenticatorFactory");
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        logger.debug("Post-init CustomBrowserAuthenticatorFactory");
    }

    @Override
    public void close() {
        logger.debug("Closing CustomBrowserAuthenticatorFactory");
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
