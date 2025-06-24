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

public class CustomDirectGrantAuthenticator implements Authenticator {

    private static final Logger logger = Logger.getLogger(CustomDirectGrantAuthenticator.class);
    private static final String BRANCH_ATTRIBUTE = "branch";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        logger.info("[authenticate] Starting custom direct grant authentication...");

        try {
            // Extract form parameters
            MultivaluedMap<String, String> formParams = context.getHttpRequest().getDecodedFormParameters();
            logger.debugf("[authenticate] Received form parameters: %s", formParams);

            String branch = formParams.getFirst(BRANCH_ATTRIBUTE);
            logger.debugf("[authenticate] Extracted branch value: %s", branch);

            if (branch == null || branch.trim().isEmpty()) {
                String errorMsg = "Missing  parameters";
                String errorDesc = "Branch parameter not found";
                logger.warn("[authenticate] " + errorMsg + "Description: " + errorDesc);
                Response errorResponse = createErrorResponse(Response.Status.BAD_REQUEST, errorMsg, errorDesc);
                context.failure(AuthenticationFlowError.INVALID_CREDENTIALS, errorResponse);
                return;
            }

            UserModel user = context.getUser();
            if (user == null) {
                String errorMsg = "Invalid User";
                String errorDesc = "User is null in context";
                logger.warn("[authenticate] " + errorMsg + "Description: " + errorDesc);
                Response errorResponse = createErrorResponse(Response.Status.BAD_REQUEST, errorMsg, errorDesc);
                context.failure(AuthenticationFlowError.INVALID_USER, errorResponse);
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

        } catch (Exception e) {
            String errorMsg = "Authentication Failed";
            String errorDesc = "Exception during authentication process: " + e.getMessage();
            logger.error("[authenticate] " + errorMsg, e);
            Response errorResponse = createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, errorMsg, errorDesc);
            context.failure(AuthenticationFlowError.INTERNAL_ERROR, errorResponse);
        }
    }


    @Override
    public void action(AuthenticationFlowContext context) {

    }


    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // No required actions for Direct Grant
    }

    @Override
    public void close() {
        // No resources to clean up
    }
}
