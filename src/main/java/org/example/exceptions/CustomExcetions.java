package org.example.exceptions;

/*
 * @Created At 23/06/2025
 * @Author ashim.gotame
 */

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;

public class CustomExcetions {

    public static Response createErrorResponse(Response.Status status, String message, String description) {
        Map<String, String> errorBody = new HashMap<>();
        errorBody.put("errorDescription", description);
        errorBody.put("error", message);


        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(errorBody)
                .build();
    }
}
