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

public class CustomBrowserAuthenticator implements Authenticator {
    private static final Logger logger = Logger.getLogger(CustomBrowserAuthenticator.class);
    private static final String LOGIN_FORM = "login"; // Must match the FTL file name in theme

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        logger.info("CustomBrowserAuthenticator: Rendering custom login form...");

        Response challengeResponse = context.form()
                .setAttribute("realm", context.getRealm())
                .createForm(LOGIN_FORM + ".ftl");

        context.challenge(challengeResponse);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String username = getValueOrEmpty(formData.getFirst("username"));
        String branch = getValueOrEmpty(formData.getFirst("branch"));
        logger.info("Login Attempt - Username: " + username + ", Branch: " + branch);

        if (branch.isEmpty()) {
            String errorMsg = "Missing branch field";
            logger.warn("[action] Branch field is missing");
            Response errorResponse = context.form()
                    .setError(errorMsg)
                    .createForm(LOGIN_FORM + ".ftl");
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, errorResponse);
            return;
        }
        UserModel user =  context.getSession().users().getUserByUsername(context.getRealm(), username);
        logger.info("username :"+ user.getEmail());

        if (user == null) {
            logger.warn("[action] User not set in context");
            Response errorResponse = context.form()
                    .setError("User not found")
                    .createForm(LOGIN_FORM + ".ftl");
            context.failureChallenge(AuthenticationFlowError.INVALID_USER, errorResponse);
            return;
        }

        logger.debugf("[authenticate] Validating branch [%s] against user [%s]", branch, user.getUsername());
        boolean isBranchValid = CustomValidator.validateUserBranch(branch, user);
        logger.info("Is BranchValid :" + isBranchValid);
        if (!isBranchValid) {
            String errorMsg = "Invalid Branch";
            String errorDesc = String.format("Branch validation failed for user [%s]. Provided: %s", user.getUsername(), branch);
            logger.warn("[authenticate] " + errorMsg);
            Response errorResponse = createErrorResponse(Response.Status.BAD_REQUEST, errorMsg, errorDesc);
            context.failure(AuthenticationFlowError.INVALID_CREDENTIALS, errorResponse);
            return;
        }

        logger.infof("[authenticate] Branch validation passed for user: %s", user.getUsername());


        context.success();
    }

    @Override
    public boolean requiresUser() {
        return false; // Should be false for initial browser login
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // Not used in browser login flow
    }

    @Override
    public void close() {
        // No cleanup needed
    }

    private String getValueOrEmpty(String value) {
        return value != null ? value.trim() : "";
    }
}
