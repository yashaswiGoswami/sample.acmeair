package com.acmeair.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.acmeair.entities.Customer;

/**
 * AcmeAir test.
 */
public class AcmeAirTest {

    private static String baseURL;
    private static String userId = "uid0@email.com";
    private static String userPassword = "password";
    
    private HttpClient client;
        
    static {
        String httpPort = System.getProperty("httpPort", "9081");
        baseURL = "http://localhost:" + httpPort + "/acmeair";
    }
    
    @BeforeClass
    public static void init() throws Exception {
        HttpClient client = new HttpClient();
        GetMethod method = new GetMethod(baseURL + "/rest/api/loader/loadSmall");

        try {
            int statusCode = client.executeMethod(method);
            assertEquals("HTTP GET failed", HttpStatus.SC_OK, statusCode);

            String response = method.getResponseBodyAsString();            
            assertTrue("Unexpected response body", response.contains("Sample data loaded."));                   
        } finally {
            method.releaseConnection();
        } 
    }
    
    @Before
    public void initClient() {
        client = new HttpClient();
    }
    
    @Test
    public void testLoginLogout() throws Exception {      
        Cookie sessionCookie = login(userId, userPassword);
        logout(sessionCookie);
    }
        
    @Test
    public void testUpdateCustomer() throws Exception {      
        Cookie sessionCookie = login(userId, userPassword);
        Customer customer;
        Customer updatedCustomer;        
        try {
            customer = getCustomer(userId, sessionCookie);            
            String address = "apt. " + System.currentTimeMillis();                             
            // update & check miles info;
            customer.getAddress().setStreetAddress2(address);
            updatedCustomer = updateCustomer(userId, sessionCookie, customer);            
            assertEquals(address, updatedCustomer.getAddress().getStreetAddress2());
            // double check if miles info was updated
            customer = getCustomer(userId, sessionCookie);
            assertEquals(address, customer.getAddress().getStreetAddress2());
        } finally {
            logout(sessionCookie);
        }
    }
    
    @Test
    public void testQueryFlights() throws Exception {      
        Cookie sessionCookie = login(userId, userPassword);
     
        try {
            JsonNode node = queryFlights(sessionCookie, "JFK", "CDG");            
            assertEquals(2, node.get("tripLegs").getIntValue());
            
            assertTrue(node.get("tripFlights").isArray());
            assertTrue(node.get("tripFlights").get(0).get("flightsOptions").size() > 0);
            assertTrue(node.get("tripFlights").get(1).get("flightsOptions").size() > 0);
        } finally {
            logout(sessionCookie);
        }
    }
    
    private Cookie login(String username, String password) throws Exception {
        PostMethod method = new PostMethod(baseURL + "/rest/api/login");
        method.addParameter("login", username);
        method.addParameter("password", password);     
        
        try {
            int statusCode = client.executeMethod(method);
            assertEquals("HTTP POST failed", HttpStatus.SC_OK, statusCode);

            String response = method.getResponseBodyAsString();
            assertTrue("Unexpected response body", response.contains("logged in"));
            
            return getSessionCookie();
        } finally {
            method.releaseConnection();
        }
    }
    
    private void logout(Cookie sessionCookie) throws Exception {
        GetMethod getMethod = new GetMethod(baseURL + "/rest/api/login/logout");
        getMethod.setRequestHeader("Cookie", sessionCookie.toExternalForm());
        
        try {
            int statusCode = client.executeMethod(getMethod);
            assertEquals("HTTP GET failed", HttpStatus.SC_OK, statusCode);

            String response = getMethod.getResponseBodyAsString();
            assertTrue("Unexpected response body", response.contains("logged out"));
        } finally {
            getMethod.releaseConnection();
        }
    }
    
    private Customer getCustomer(String id, Cookie sessionCookie) throws Exception {
        GetMethod getMethod = new GetMethod(baseURL + "/rest/api/customer/byid/" + id);
        getMethod.setRequestHeader("Cookie", sessionCookie.toExternalForm());
        
        try {
            int statusCode = client.executeMethod(getMethod);
            assertEquals("HTTP GET failed", HttpStatus.SC_OK, statusCode);
            String response = getMethod.getResponseBodyAsString();
            ObjectMapper mapper = new ObjectMapper();
            Customer c = mapper.readValue(response, Customer.class);
            return c;
        } finally {
            getMethod.releaseConnection();
        }
    }
    
    private Customer updateCustomer(String id, Cookie sessionCookie, Customer customer) throws Exception {
        PostMethod method = new PostMethod(baseURL + "/rest/api/customer/byid/" + id);
        method.setRequestHeader("Cookie", sessionCookie.toExternalForm());
        
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(customer);
        StringRequestEntity requestEntity = new StringRequestEntity(json, "application/json", "UTF-8");
        method.setRequestEntity(requestEntity);
        
        try {
            int statusCode = client.executeMethod(method);
            assertEquals("HTTP GET failed", HttpStatus.SC_OK, statusCode);
            String response = method.getResponseBodyAsString();
            Customer c = mapper.readValue(response, Customer.class);
            return c;
        } finally {
            method.releaseConnection();
        }
    }
    
    private JsonNode queryFlights(Cookie sessionCookie, String fromAirport, String toAirport) throws Exception {
        PostMethod method = new PostMethod(baseURL + "/rest/api/flights/browseflights");
        method.addParameter("fromAirport", fromAirport);
        method.addParameter("toAirport", toAirport);    
        method.addParameter("oneWay", "false");   
        
        try {
            int statusCode = client.executeMethod(method);
            assertEquals("HTTP POST failed", HttpStatus.SC_OK, statusCode);

            String response = method.getResponseBodyAsString();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response);
            return node;
        } finally {
            method.releaseConnection();
        }
    }
    
    private Cookie getSessionCookie() {
        Cookie[] cookies = client.getState().getCookies();
        for(int i = 0; i < cookies.length; i++) {
            if ("sessionid".equals(cookies[i].getName())) {
                 return cookies[i];
            }
        }
        throw new RuntimeException("Session cookie not found");
    }
       
}
