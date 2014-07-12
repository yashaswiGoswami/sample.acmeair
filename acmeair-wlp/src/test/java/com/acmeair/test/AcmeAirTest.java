package com.acmeair.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
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
        GetMethod method = new GetMethod(baseURL + "/rest/api/loader/loadCustomers");

        try {
            int statusCode = client.executeMethod(method);

            assertEquals("HTTP GET failed", HttpStatus.SC_OK, statusCode);

            String response = method.getResponseBodyAsString();
            
            assertTrue("Unexpected response body", response.contains("Customer data loaded."));                   
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
    public void testGetCustomer() throws Exception {      
        Cookie sessionCookie = login(userId, userPassword);
        try {
            Customer customer = getCustomer(userId, sessionCookie);
            assertEquals(Customer.MemberShipStatus.GOLD, customer.getStatus());
            assertEquals(Customer.PhoneType.BUSINESS, customer.getPhoneNumberType());
            assertEquals("919-123-4567", customer.getPhoneNumber());
        } finally {
            logout(sessionCookie);
        }
    }
    
    @Test
    public void testUpdateCustomer() throws Exception {      
        Cookie sessionCookie = login(userId, userPassword);
        String oldNumber = "919-123-4567";
        String newNumber = "999-999-9999";
        try {
            Customer customer = getCustomer(userId, sessionCookie);
            assertEquals(Customer.MemberShipStatus.GOLD, customer.getStatus());
            assertEquals(Customer.PhoneType.BUSINESS, customer.getPhoneNumberType());            
            assertEquals(oldNumber, customer.getPhoneNumber());
            
            Customer updatedCustomer;
            
            customer.setPhoneNumber(newNumber);
            updatedCustomer = updateCustomer(userId, sessionCookie, customer);            
            assertEquals(newNumber, updatedCustomer.getPhoneNumber());
            
            updatedCustomer.setPhoneNumber(oldNumber);
            updatedCustomer.setPassword(userPassword);
            updatedCustomer = updateCustomer(userId, sessionCookie, updatedCustomer);            
            assertEquals(oldNumber, updatedCustomer.getPhoneNumber());
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
            assertEquals("Login failed", HttpStatus.SC_OK, statusCode);

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
            assertEquals("Logout failed", HttpStatus.SC_OK, statusCode);

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
