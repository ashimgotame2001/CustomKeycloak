package org.example.provider;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.example.utils.CustomValidator;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import static org.example.exceptions.CustomExcetions.createErrorResponse;

public class CustomBrowserAuthenticatorWithBranch implements Authenticator {
    private static final Logger logger = Logger.getLogger(CustomBrowserAuthenticatorWithBranch.class);
    private static final String LOGIN_FORM = "login"; // Must match the FTL file name in theme

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        logger.info("🖼️  [authenticate] Rendering custom login form...");

        Response challengeResponse = context.form()
                .setAttribute("realm", context.getRealm())
                .createForm(LOGIN_FORM + ".ftl");

        context.challenge(challengeResponse);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        logger.info("🚦 [action] Processing login form submission...");

        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String username = getValueOrEmpty(formData.getFirst("username"));
        String branch = getValueOrEmpty(formData.getFirst("branch"));

        logger.infof("🔐 Login Attempt - Username: %s | Branch: %s", username, branch);

        if (branch.isEmpty()) {
            logger.warn("⚠️  [action] Branch field is missing");
            Response errorResponse = context.form()
                    .setError("Missing branch field")
                    .createForm(LOGIN_FORM + ".ftl");
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, errorResponse);
            return;
        }

        UserModel user = context.getSession().users().getUserByUsername(context.getRealm(), username);

        if (user == null) {
            logger.warnf("❌ [action] User [%s] not found in Keycloak", username);
            Response errorResponse = context.form()
                    .setError("User not found")
                    .createForm(LOGIN_FORM + ".ftl");
            context.failureChallenge(AuthenticationFlowError.INVALID_USER, errorResponse);
            return;
        }

        logger.infof("🔍 Validating branch [%s] for user [%s]", branch, user.getUsername());
        boolean isBranchValid = CustomValidator.validateUserBranch(branch, user);

        if (!isBranchValid) {
            String errorMsg = "Invalid Branch";
            String errorDesc = String.format("Branch validation failed for user [%s]. Provided: %s", user.getUsername(), branch);
            logger.warnf("⚠️  [action] %s", errorDesc);
            Response errorResponse = createErrorResponse(Response.Status.BAD_REQUEST, errorMsg, errorDesc);
            context.failure(AuthenticationFlowError.INVALID_CREDENTIALS, errorResponse);
            return;
        }

        logger.infof("✅ [action] Branch validation passed for user: %s", user.getUsername());

        context.success();
    }

    @Override
    public boolean requiresUser() {
        return false; // Initial browser login doesn't need user
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true; // No additional config required
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        logger.debug("ℹ️  [setRequiredActions] No required actions needed for user.");
    }

    @Override
    public void close() {
        logger.debug("🔚 [close] Cleaning up CustomBrowserAuthenticatorWithBranch.");
    }

    private String getValueOrEmpty(String value) {
        return value != null ? value.trim() : "";
    }
}
