package io.leangen.graphql.spqr.spring.web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import io.leangen.graphql.spqr.spring.autoconfigure.SpqrAutoConfiguration;
import io.leangen.graphql.spqr.spring.autoconfigure.SpqrMvcAutoConfiguration;
import io.leangen.graphql.spqr.spring.test.ResolverBuilder_TestConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest
@ContextConfiguration(classes = {SpqrAutoConfiguration.class, SpqrMvcAutoConfiguration.class,
        ResolverBuilder_TestConfig.class})
@TestPropertySource(locations = "classpath:application.properties")
public class GraphQLControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${graphql.spqr.http.endpoint}")
    private String apiContext;

    @Test
    public void defaultControllerTest_POST_applicationGraphql_noQueryParams() throws Exception {
        mockMvc.perform(
                    post("/"+apiContext)
                    .contentType("application/graphql")
                    .content("{greetingFromBeanSource_wiredAsComponent_byAnnotation}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hello world")));
    }

    @Test
    public void defaultControllerTest_POST_applicationJson_noQueryParams() throws Exception {
        mockMvc.perform(
                post("/"+apiContext)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content("{\"query\":\"{greetingFromBeanSource_wiredAsComponent_byAnnotation}\",\"variables\":null,\"operationName\":null}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hello world")));
    }

    @Test
    public void defaultControllerTest_POST_formUrlEncoded_noQueryParams() throws Exception {
        mockMvc.perform(
                post("/"+apiContext)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .content("query="+ URLEncoder.encode("{greetingFromBeanSource_wiredAsComponent_byAnnotation}", StandardCharsets.UTF_8.toString())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hello world")));
    }

    @Test
    public void defaultControllerTest_GET() throws Exception {
        mockMvc.perform(
                get("/"+apiContext)
                        .param("query","{greetingFromBeanSource_wiredAsComponent_byAnnotation}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hello world")));
    }

    @Test
    public void defaultControllerTest_POST_applicationGraphql_INVALID() throws Exception {
        mockMvc.perform(
                post("/"+apiContext)
                        .contentType("application/graphql")
                        .content("{INVALID_QUERY}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("FieldUndefined: Field 'INVALID_QUERY'")));
    }

    @Test
    public void defaultControllerTest_POST_applicationGraphql_overridingQueryParams() throws Exception {
        mockMvc.perform(
                post("/"+apiContext)
                        .param("query","{greetingFromBeanSource_wiredAsComponent_byAnnotation}")
                        .contentType("application/graphql")
                        .content("{INVALID_QUERY}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hello world")));
    }

    @Test
    public void defaultControllerTest_POST_applicationJson_INVALID() throws Exception {
        mockMvc.perform(
                post("/"+apiContext)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content("{\"query\":\"{INVALID_QUERY}\",\"variables\":null,\"operationName\":null}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("FieldUndefined: Field 'INVALID_QUERY'")));
    }

    @Test
    public void defaultControllerTest_POST_applicationJson_overridingQueryParams() throws Exception {
        mockMvc.perform(
                post("/" + apiContext)
                        .param("query", "{greetingFromBeanSource_wiredAsComponent_byAnnotation}")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content("{\"query\":\"{INVALID_QUERY}\",\"variables\":null,\"operationName\":null}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hello world")));
    }

    @Test
    public void defaultControllerTest_GET_with_variables() throws Exception {
        mockMvc.perform(
                get("/" + apiContext)
                        .param("query", "query Echo($contentInput: String){ echo(content: $contentInput)}")
                        .param("variables", "{\"contentInput\": \"Hello world\"}")
        )
                .andExpect(status().isOk())
                .andExpect(content().json("{\"data\":{\"echo\":\"Hello world\"}}", true));
    }

    @Test
    public void defaultControllerTest_POST_with_variables() throws Exception {
        mockMvc.perform(
                post("/" + apiContext)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content("{\"query\":\"{INVALID_QUERY}\",\"variables\":{\"contentInput\": \"Hello world2\"},\"operationName\":null}")
                        .param("query", "query Echo($contentInput: String){ echo(content: $contentInput)}")
                        .param("variables", "{\"contentInput\": \"Hello world1\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"data\":{\"echo\":\"Hello world1\"}}", true));
    }

    @Test
    public void defaultControllerTest_POST_formUrlEncoded_INVALID() throws Exception {
        mockMvc.perform(
                post("/"+apiContext)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .content("query="+ URLEncoder.encode("{INVALID_QUERY}", StandardCharsets.UTF_8.toString())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("FieldUndefined: Field 'INVALID_QUERY'")));
    }

    @Test
    public void defaultControllerTest_POST_formUrlEncoded_overridingQueryParams() throws Exception {
        mockMvc.perform(
                post("/" + apiContext)
                        .param("query", "{greetingFromBeanSource_wiredAsComponent_byAnnotation}")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .content("query=" + URLEncoder.encode("{INVALID_QUERY}", StandardCharsets.UTF_8.toString())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hello world")));
    }

    @Test
    public void uploadFile() throws Exception {
        mockMvc.perform(
                multipart("/" + apiContext)
                        .file("1", "content1".getBytes())
                        .file("2", "content2".getBytes())
                        .param("operations", "{\n" +
                                "  \"operationName\": \"Upload\",\n" +
                                "  \"variables\": {\n" +
                                "    \"file1\": null,\n" +
                                "    \"file2\": null\n" +
                                "  },\n" +
                                "  \"query\": \"query Upload($file1: FileUpload, $file2: FileUpload) {\\n  upload(file1: $file1, file2: $file2) }\\n\"\n" +
                                "}")
                        .param("map", "{  \"1\": [\"variables.file1\"],  \"2\": [\"variables.file2\"] }")

        )
                .andExpect(status().isOk())
                .andExpect(content().json("{\"data\":{\"upload\":[\"content1\",\"content2\"]}}", true));
    }

    @Test
    public void uploadMultipleFile() throws Exception {
        mockMvc.perform(
                multipart("/" + apiContext)
                        .file("1", "content1".getBytes())
                        .file("2", "content2".getBytes())
                        .param("operations", "{\n" +
                                "  \"operationName\": \"UploadFiles\",\n" +
                                "  \"variables\": {\n" +
                                "    \"files\": [null]\n" +
                                "  },\n" +
                                "  \"query\": \"query UploadFiles($files: [FileUpload]) {\\n  uploadFiles(files: $files) }\\n\"\n" +
                                "}")
                        .param("map", "{  \"1\": [\"variables.files.0\"],  \"2\": [\"variables.files.1\"] }")

        )
                .andExpect(status().isOk())
                .andExpect(content().json("{\"data\":{\"uploadFiles\":[\"content1\",\"content2\"]}}", true));
    }
}
