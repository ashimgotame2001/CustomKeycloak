package org.example.constants;

/*
 * @Created At 24/06/2025
 * @Author ashim.gotame
 */

public class SqlConstants {

    public static final String GET_ROLE_NAME_BY_USER_ID = "SELECT r.name FROM roles r JOIN user_roles ur ON ur.role_id = r.id WHERE ur.user_id = ?";

}
