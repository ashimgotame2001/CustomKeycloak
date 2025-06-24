package org.example.utils;

/*
 * @Created At 18/06/2025
 * @Author ashim.gotame
 */

import org.jboss.logging.Logger;
import org.keycloak.models.UserModel;

import java.util.Collections;
import java.util.List;

public class CustomValidator {

    private static final Logger logger = Logger.getLogger(CustomValidator.class);
    private static final String BRANCH_ATTRIBUTE = "branch";


    public static boolean validateUserBranch(String branch, UserModel userModel) {
        logger.info("Validating user branch...");
        List<String> branchList = getUserBranchAttributes(userModel);
        branchList.forEach(b -> logger.info("User branch attribute: '" + b + "'"));

        if (branchList.isEmpty()) {
            logger.warn("User has no branch attributes");
            return false;
        }

        String userBranch = branchList.get(0).trim();
        String expectedBranch = branch.trim();

        logger.info("User Branch (first element): '" + userBranch + "'");
        logger.info("Expected Branch: '" + expectedBranch + "'");

        return userBranch.equalsIgnoreCase(expectedBranch);
    }


    private static List<String> getUserBranchAttributes(UserModel user) {
        return user.getAttributes().getOrDefault(BRANCH_ATTRIBUTE, Collections.emptyList());
    }
}
