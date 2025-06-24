package org.example.provider;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordForm;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.UserModel;

import static org.example.exceptions.CustomExcetions.createErrorResponse;

public class CustomUsernamePasswordBranchForm extends UsernamePasswordForm {
    private static final Logger logger = Logger.getLogger(CustomUsernamePasswordBranchForm.class);
    private static final String BRANCH = "branch";

    @Override
    protected Response challenge(AuthenticationFlowContext context, String error, String field) {
        logger.debugf("Challenge initiated with error: %s and field: %s", error, field);
        setBranchPlaceholder(context);
        Response response = super.challenge(context, error, field);
        logger.debug("Challenge response generated.");
        return response;
    }

    @Override
    protected Response challenge(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
        logger.debug("Challenge initiated with form data.");
        setBranchPlaceholder(context);
        Response response = super.challenge(context, formData);
        logger.debug("Challenge response generated.");
        return response;
    }

    @Override
    protected boolean validateForm(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
        logger.debug("Form validation started.");
        boolean isValid = super.validateForm(context, formData);
        if (!isValid) {
            logger.warn("Form validation failed.");     return false;
        }

        String branch = trim(formData.getFirst(BRANCH));
        logger.infof("Branch entered: %s", branch);

        if (branch == null || branch.trim().isEmpty()) {
            String errorMsg = "Missing parameters";
            String errorDesc = "Branch parameter not found";
            logger.warnf("Authentication failed: %s - %s", errorMsg, errorDesc);
            Response errorResponse = createErrorResponse(Response.Status.BAD_REQUEST, errorMsg, errorDesc);
            context.failure(AuthenticationFlowError.INVALID_CREDENTIALS, errorResponse);
            return false;
        }

        UserModel user = context.getUser();
        if (user == null) {
            String errorMsg = "Invalid User";
            String errorDesc = "User is null in context";
            logger.warnf("Authentication failed: %s - %s", errorMsg, errorDesc);
            Response errorResponse = createErrorResponse(Response.Status.BAD_REQUEST, errorMsg, errorDesc);
            context.failure(AuthenticationFlowError.INVALID_USER, errorResponse);
            return false;
        }

        logger.debug("Form validation succeeded.");
        return true;
    }

    private void setBranchPlaceholder(AuthenticationFlowContext ctx) {
        LoginFormsProvider form = ctx.form();
        form.setAttribute(BRANCH, "");
        logger.debug("Branch placeholder set.");
    }

    private String trim(String val) {
        return val == null ? "" : val.trim();
    }
}
