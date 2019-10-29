package io.leangen.graphql.spqr.spring.web;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQL;
import io.leangen.graphql.spqr.spring.web.dto.GraphQLRequest;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.validation.DataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public abstract class GraphQLController<R> {

    protected final GraphQL graphQL;
    protected final GraphQLExecutor<R> executor;
    private ObjectMapper objectMapper;

    public GraphQLController(GraphQL graphQL, GraphQLExecutor<R> executor, ObjectMapper objectMapper) {
        this.graphQL = graphQL;
        this.executor = executor;
        this.objectMapper = objectMapper;
    }

    @PostMapping(
            value = "${graphql.spqr.http.endpoint:/graphql}",
            consumes = {MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.APPLICATION_JSON_VALUE},
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    @ResponseBody
    public Object executeJsonPost(@RequestBody GraphQLRequest requestBody,
                                  @RequestParam(value = "query", required = false) String requestQuery,
                                  @RequestParam(value = "operationName", required = false) String requestOperationName,
                                  @RequestParam(value = "variables", required = false) String variablesJsonString,
                                  R request) throws IOException {
        String query = requestQuery == null ? requestBody.getQuery() : requestQuery;
        String operationName = requestOperationName == null ? requestBody.getOperationName() : requestOperationName;
        //noinspection unchecked
        Map<String, Object> variables = variablesJsonString == null ? requestBody.getVariables() : objectMapper.readValue(variablesJsonString, Map.class);

        return executor.execute(graphQL, new GraphQLRequest(query, operationName, variables), request);
    }

    @PostMapping(
            value = "${graphql.spqr.http.endpoint:/graphql}",
            consumes = {"application/graphql", "application/graphql;charset=UTF-8"},
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    @ResponseBody
    public Object executeGraphQLPost(@RequestBody String queryBody,
                                   GraphQLRequest graphQLRequest,
                                   R request) {
        String query = graphQLRequest.getQuery() == null ? queryBody : graphQLRequest.getQuery();
        return executor.execute(graphQL, new GraphQLRequest(query, graphQLRequest.getOperationName(), graphQLRequest.getVariables()), request);
    }

    @RequestMapping(
            method = RequestMethod.POST,
            value = "${graphql.spqr.http.endpoint:/graphql}",
            consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE, "application/x-www-form-urlencoded;charset=UTF-8"},
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    @ResponseBody
    public Object executeFormPost(@RequestParam Map<String, String> queryParams,
                                GraphQLRequest graphQLRequest,
                                R request) {
        String queryParam = queryParams.get("query");
        String operationNameParam = queryParams.get("operationName");

        String query = StringUtils.isEmpty(queryParam) ? graphQLRequest.getQuery() : queryParam;
        String operationName = StringUtils.isEmpty(operationNameParam) ? graphQLRequest.getOperationName() : operationNameParam;

        return executor.execute(graphQL, new GraphQLRequest(query, operationName, graphQLRequest.getVariables()), request);
    }

    @GetMapping(
            value = "${graphql.spqr.http.endpoint:/graphql}",
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE,
            headers = "Connection!=Upgrade"
    )
    @ResponseBody
    public Object executeGet(@RequestParam(value = "query") String requestQuery,
                             @RequestParam(value = "operationName", required = false) String requestOperationName,
                             @RequestParam(value = "variables", required = false) String variablesJsonString,
                             R request) throws IOException {
        //noinspection unchecked
        Map<String, Object> variables = variablesJsonString == null ? Collections.emptyMap() : objectMapper.readValue(variablesJsonString, Map.class);
        return executor.execute(graphQL, new GraphQLRequest(requestQuery, requestOperationName, variables), request);
    }

    @PostMapping(
            value = "${graphql.spqr.http.endpoint:/graphql}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    public Object executeMultipartFileUpload(
            @RequestParam("operations") String requestString,
            @RequestParam("map") String mappingString,
            @RequestParam Map<String, MultipartFile> multipartFiles,
            R request) throws IOException {
        GraphQLRequest graphQLRequest = objectMapper.readValue(requestString, GraphQLRequest.class);
        //noinspection unchecked
        Map<String, List<String>> fileMappings = objectMapper.readValue(mappingString, Map.class);

        Map<String, Object> values = new LinkedHashMap<>();
        fileMappings.forEach((fileKey, variables) -> {
            for (String variable : variables) {
                String[] parts = variable.split("\\.");
                String path = parts[0] + Arrays.stream(parts).skip(1).collect(Collectors.joining("][", "[", "]"));
                values.put(path,  multipartFiles.get(fileKey));
            }
        });

        DataBinder binder = new DataBinder(graphQLRequest, "operations");
        binder.setIgnoreUnknownFields(false);
        binder.setIgnoreInvalidFields(false);
        binder.bind(new MutablePropertyValues(values));

        return executor.execute(graphQL, graphQLRequest, request);
    }
}
