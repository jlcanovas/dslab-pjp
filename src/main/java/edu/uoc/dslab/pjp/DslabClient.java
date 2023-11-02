package edu.uoc.dslab.pjp;

import java.io.File;
import java.util.Base64;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Main class to interact with DSLab API
 * 
 * There is a {@link #main(String[])} method to use this class as a command line tool,
 * but basically the procedure is:
 *   1. Get the token via {@link #token(String, String, String, String)}
 *   2. Call {@link #submit(String, String, String)} to submit the zip file
 *   3. Call {@link #compile(String, String)} to compile the project
 *   4. Call {@link #evaluate(String, String, String)} to evaluate the project
 * 
 * This code has been inspired by the code provided by the DSLab developers: 
 *   - Joan Manuel Marquès Puig (jmarquesp@uoc.edu)
 *   - David Borrega Borrella (dborrega@uoc.edu)
 */
public class DslabClient {
    private static boolean DISABLE_SSL = true;
    private static String DEFAULT_CHARSET = "UTF-8";
    private static String HTTP_HOST = "https://dpcscodes.uoc.edu/dslab-api/";

    private static final Logger LOGGER = LogManager.getLogger(DslabClient.class);

    /**
     * Get the token to interact with the DSLab API
     * 
     * @param url The URL to access DSLab API
     * @param tenant The name of the project/tenant (contact DSLab developers to get it)
     * @param username The username (pay attention between student vs. professor usernames)
     * @param password The password
     * @return The token as String
     */
    public String token(String url,String tenant, String username, String password) {
        CloseableHttpResponse response = null;
        String result = null;
        
        LOGGER.debug("Method token() url: [" + url + "] tenant: [" + tenant + "] username: [" + username + "] password: [" + password + "]");
        try {
            // Adding headers
            HttpPost post = new HttpPost(url);
            post.addHeader("X-TenantID", tenant);
            // Adding authorization
            String valueToEncode = username + ":" + password;
            post.addHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes()));
            // Create HTTP Client
            CloseableHttpClient client = getHttpClient();
            // Execute client
            LOGGER.debug("Executing client...");
            response = client.execute(post);
            // Handle response
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                result = EntityUtils.toString(entity, DEFAULT_CHARSET);
            } 
        } catch (Exception e) {
            e.printStackTrace();
        } 
        
        LOGGER.debug("Token received [" + result + "]");
        return result;
    }

    /**
     * Submit a project in zip format
     * 
     * @param url The URL to access DSLab API
     * @param token The token generated by {@link #token(String, String, String, String)}
     * @param zipFile The path to the zip file with the project
     * @return The project ID as String
     */
    public String submit(String url, String token, String zipFile){
        CloseableHttpResponse response = null;
        String result = null;

        LOGGER.debug("Method submit() url: [" + url + "] token: [" + token + "] zipFile: [" + zipFile + "]");
        try { 
            // Create HTTP Client
            CloseableHttpClient client = getHttpClient();
            // Getting the file
            File file = new File(zipFile);
            // Building the multipart
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            String boundary = "--" + System.currentTimeMillis();
            builder.setBoundary(boundary);
            builder.addBinaryBody("files", file, ContentType.create("application/zip"), file.getName());
            HttpEntity multipartEntity = builder.build();
            // Creating the POST request
            HttpPost post = new HttpPost(url);
            post.setEntity(multipartEntity);
            // Adding headers
            post.addHeader("Authorization", "Bearer " + token);
            post.addHeader("Content-Type", "multipart/form-data; boundary=" + boundary);
            // Execute client
            LOGGER.debug("Executing client...");
            response = client.execute(post);
            // Handle response
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String entityDigested = EntityUtils.toString(entity, DEFAULT_CHARSET);
                JsonObject messageJson = JsonParser.parseString(entityDigested).getAsJsonObject();
                if (messageJson.getAsJsonObject("data").has("id_projecte")) {
                    result = messageJson.getAsJsonObject("data").get("id_projecte").getAsString();
                } 
            } 
        } catch (Exception e) {
            LOGGER.fatal(e.getMessage());
            // e.printStackTrace();
        } 
        return result;

    }

    /**
     * Compiles the project at DSLab side
     * 
     * @param url The URL to access DSLab API
     * @param token The token generated by {@link #token(String, String, String, String)}
     */
    public void compile(String url, String token){
        
        LOGGER.debug("Method compile() url: [" + url + "] token: [" + token + "]");
        try {
            // Execute request
            HttpGet get = new HttpGet(url);
            // Adding headers
            get.addHeader("Authorization", "Bearer " + token);
            // Create HTTP Client
            CloseableHttpClient client = getHttpClient();
            // Execute client
            client.execute(get);
        } catch (Exception e) {
            e.printStackTrace();
        } 
    }

    /**
     * Evaluates the project at DSLab side
     * 
     * @param url The URL to access DSLab API
     * @param token The token generated by {@link #token(String, String, String, String)}
     * @param projectId The project ID generated by {@link #submit(String, String, String)}
     */
    public void evaluate(String url, String token, String projectId){ 

        LOGGER.debug("Method compile() url: [" + url + "] token: [" + token + "]" + " projectId: [" + projectId + "]");
        try {
            HttpPost post = new HttpPost(url);
            // Adding headers
            post.addHeader("Authorization", "Bearer " + token);
            // Setting payload
            post.setEntity(new StringEntity("{\"projectes\":["+ projectId +"]}", ContentType.APPLICATION_JSON));
            // Create HTTP Client
            CloseableHttpClient client = getHttpClient();
            // Execute client
            client.execute(post);
        } catch (Exception e) {
            e.printStackTrace();
        } 
    }

    /**
     * Factorizes the code to create the HTTP Client
     * @return A CloseableHttpClient
     */
    private CloseableHttpClient getHttpClient() {
        CloseableHttpClient client = null;
        try {
            if (DISABLE_SSL) {
                client = HttpClients.custom()
                        .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
                        .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                        .build();
            } else {
                client = HttpClients.createDefault();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } 
        return client;
    }

    /**
     * Main method to use this class as a command line tool
     * 
     * @param args String array with the following parameters:
     * args[0] = tenant
     * args[1] = DSLab project
     * args[2] = username
     * args[3] = password
     * args[4] = path to zip file
     * 
     */
    public static void main(String[] args) {
        String tenant = args[0];
        String project = args[1];
        String username = args[2];
        String password = args[3];
        String zip = args[4];

        LOGGER.info("Starting DSLab client...");
        DslabClient client = new DslabClient();

        LOGGER.info("1/4: Getting token...");
        String token = client.token(HTTP_HOST+"token", tenant, username, password);
        if (token == null) {
            LOGGER.fatal("No token received. Check your credentials and try again.");
            return;
        }
        
        LOGGER.info("2/4: Submitting the project...");
        String projectId = client.submit(HTTP_HOST+"projectes/desa/"+project, token, zip);
        if (projectId == null) {
            LOGGER.fatal("No project ID received. Check your project id and try again.");
            return;
        }
        
        LOGGER.info("3/4: Compiling the project...");
        client.compile(HTTP_HOST+"projectes/compila/"+projectId, token);
        
        LOGGER.info("4/4: Evaluating the project...");
        client.evaluate(HTTP_HOST+"enviaments/corregeix", token, projectId);
        
        LOGGER.info("Done!");
    }

}

