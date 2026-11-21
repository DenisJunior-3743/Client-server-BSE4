package com.trustbank.loanapp.web;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.trustbank.loanapp.common.ApiException;
import com.trustbank.loanapp.security.AuthUser;
import com.trustbank.loanapp.security.CurrentUser;
import com.trustbank.loanapp.service.LoanApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** JSON-RPC transport adapter. The application service owns authorization and loan queries. */
@RestController
@SecurityScheme(name = "rpcBearer", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class RpcController {
    private static final Logger log = LoggerFactory.getLogger(RpcController.class);
    private static final String VERSION = "2.0";
    private static final int MAX_BATCH = 10;

    private final LoanApplicationService loans;
    private final ObjectMapper json;
    private final ObjectReader requestReader;

    public RpcController(LoanApplicationService loans, ObjectMapper json) {
        this.loans = loans;
        this.json = json;
        this.requestReader = json.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
                DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
    }

    @PostMapping(path = "/api/v1/rpc", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Call a loan procedure using JSON-RPC 2.0",
            description = "Methods: loan.getApplication and loan.getRepaymentSchedule. Both take an applicationId UUID. "
                    + "Batches are limited to ten calls. Include an id when you want a response.",
            security = @SecurityRequirement(name = "rpcBearer"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "object"),
                            examples = @ExampleObject(value = """
                                    {
                                      "jsonrpc": "2.0",
                                      "method": "loan.getApplication",
                                      "params": {"applicationId": "11111111-1111-1111-1111-111111111111"},
                                      "id": "detail-1"
                                    }
                                    """))))
    public ResponseEntity<JsonNode> invoke(@CurrentUser AuthUser caller, @RequestBody(required = false) byte[] body) {
        JsonNode request;
        try {
            request = body == null ? null : requestReader.readTree(body);
        } catch (IOException ex) {
            return ResponseEntity.ok(error(NullNode.getInstance(), -32700, "Parse error"));
        }
        if (request == null || request.isMissingNode()) {
            return ResponseEntity.ok(error(NullNode.getInstance(), -32700, "Parse error"));
        }

        JsonNode response = request.isArray() ? batch(caller, request) : dispatch(caller, request);
        return response == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(response);
    }

    private JsonNode batch(AuthUser caller, JsonNode requests) {
        if (requests.isEmpty()) {
            return error(NullNode.getInstance(), -32600, "Invalid Request");
        }
        if (requests.size() > MAX_BATCH) {
            return error(NullNode.getInstance(), -32010, "A batch can contain at most 10 calls");
        }
        ArrayNode responses = json.createArrayNode();
        for (JsonNode request : requests) {
            JsonNode response = dispatch(caller, request);
            if (response != null) {
                responses.add(response);
            }
        }
        return responses.isEmpty() ? null : responses;
    }

    private ObjectNode dispatch(AuthUser caller, JsonNode request) {
        if (!isRequest(request)) {
            return error(NullNode.getInstance(), -32600, "Invalid Request");
        }

        ObjectNode response = call(caller, request.get("method").textValue(), request.get("params"), request.get("id"));
        // A well-formed request without an id is a notification, including when its method fails.
        return request.has("id") ? response : null;
    }

    private ObjectNode call(AuthUser caller, String method, JsonNode params, JsonNode id) {
        boolean detail = method.equals("loan.getApplication");
        if (!detail && !method.equals("loan.getRepaymentSchedule")) {
            return error(id, -32601, "Method not found");
        }
        UUID applicationId = applicationId(params);
        if (applicationId == null) {
            return error(id, -32602, "Expected one applicationId UUID");
        }
        try {
            Object result = detail ? loans.detail(caller, applicationId) : loans.schedule(caller, applicationId);
            return envelope(id).set("result", json.valueToTree(result));
        } catch (ApiException ex) {
            int code = ex.getStatus().value() == 404 ? -32004 : -32000;
            return applicationError(id, code, ex.getMessage(), ex.getStatus().value(), ex.getCode());
        } catch (CannotCreateTransactionException | DataAccessResourceFailureException
                | PessimisticLockingFailureException | QueryTimeoutException ex) {
            return applicationError(id, -32003, "The server is busy. Please retry shortly.", 503, "SERVER_BUSY");
        } catch (RuntimeException ex) {
            log.error("Loan RPC call failed", ex);
            return error(id, -32603, "Internal error");
        }
    }

    private static boolean isRequest(JsonNode request) {
        if (!request.isObject() || !VERSION.equals(request.path("jsonrpc").textValue())
                || !request.path("method").isTextual()) {
            return false;
        }
        JsonNode params = request.get("params");
        JsonNode id = request.get("id");
        return (params == null || params.isObject() || params.isArray())
                && (id == null || id.isNull() || id.isNumber() || id.isTextual());
    }

    private static UUID applicationId(JsonNode params) {
        if (params == null || params.size() != 1) {
            return null;
        }
        JsonNode value = params.isObject() ? params.path("applicationId") : params.path(0);
        if (!value.isTextual() || value.textValue().length() != 36) {
            return null;
        }
        try {
            UUID uuid = UUID.fromString(value.textValue());
            return uuid.toString().equalsIgnoreCase(value.textValue()) ? uuid : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private ObjectNode envelope(JsonNode id) {
        return json.createObjectNode().put("jsonrpc", VERSION).set("id", id);
    }

    private ObjectNode error(JsonNode id, int code, String message) {
        ObjectNode response = envelope(id);
        response.putObject("error").put("code", code).put("message", message);
        return response;
    }

    private ObjectNode applicationError(JsonNode id, int code, String message, int status, String apiCode) {
        ObjectNode response = error(id, code, message);
        ((ObjectNode) response.get("error")).putObject("data").put("status", status).put("code", apiCode);
        return response;
    }
}
