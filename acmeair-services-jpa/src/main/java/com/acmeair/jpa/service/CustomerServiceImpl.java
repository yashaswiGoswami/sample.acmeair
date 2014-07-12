/*******************************************************************************
* Copyright (c) 2013 IBM Corp.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/
package com.acmeair.jpa.service;

import java.util.Calendar;
import java.util.Date;

import javax.annotation.Resource;
import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.acmeair.entities.Customer;
import com.acmeair.entities.Customer.MemberShipStatus;
import com.acmeair.entities.Customer.PhoneType;
import com.acmeair.entities.CustomerAddress;
import com.acmeair.entities.CustomerSession;
import com.acmeair.service.CustomerService;
import com.acmeair.service.KeyGenerator;

@Service("customerService")
public class CustomerServiceImpl implements CustomerService{

	private static final int DAYS_TO_ALLOW_SESSION = 1;
	
	@Autowired
	EntityManager em;
	
	@Resource
	KeyGenerator keyGenerator;
			
	@Transactional(propagation=Propagation.REQUIRED)
	@Override
	public Customer createCustomer(String username, String password,
			MemberShipStatus status, int total_miles, int miles_ytd,
			String phoneNumber, PhoneType phoneNumberType,
			CustomerAddress address) {
		Customer customer = new Customer(username, password, status,
				total_miles, miles_ytd, address, phoneNumber, phoneNumberType);
		try {
			em.persist(customer);
			return customer;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Transactional(propagation=Propagation.REQUIRED)
	@Override
	public Customer updateCustomer(Customer updatedCustomer) {
		try {
			Customer customer = em.find(Customer.class, updatedCustomer.getUsername());
			
			CustomerAddress customerAddress = customer.getAddress();
			customerAddress.setCity(updatedCustomer.getAddress().getCity());
			customerAddress.setCountry(updatedCustomer.getAddress().getCountry());
			customerAddress.setPostalCode(updatedCustomer.getAddress().getPostalCode());
			customerAddress.setStateProvince(updatedCustomer.getAddress().getStateProvince());
			customerAddress.setStreetAddress1(updatedCustomer.getAddress().getStreetAddress1());
			customerAddress.setStreetAddress2(updatedCustomer.getAddress().getStreetAddress2());
			
			customer.setMiles_ytd(updatedCustomer.getMiles_ytd());
			customer.setPassword(updatedCustomer.getPassword());
			customer.setPhoneNumber(updatedCustomer.getPhoneNumber());
			customer.setPhoneNumberType(updatedCustomer.getPhoneNumberType());
			customer.setStatus(updatedCustomer.getStatus());
			customer.setTotal_miles(updatedCustomer.getTotal_miles());
			
			em.persist(customer);
			return customer;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Customer getCustomerByUsername(String username) {
		Customer customer = em.find(Customer.class, username);
		return customer;
	}

	@Override
	public boolean validateCustomer(String username, String password) {
		boolean validatedCustomer = false;
		Customer customerToValidate = getCustomerByUsername(username);
		if (customerToValidate != null) {
			validatedCustomer = password.equals(customerToValidate.getPassword());
		}
		return validatedCustomer;
	}

	@Override
	public Customer getCustomerByUsernameAndPassword(String username, String password) {
		Customer c = getCustomerByUsername(username);
		if (!c.getPassword().equals(password)) {
			return null;
		}
		return c;
	}

	@Transactional(propagation=Propagation.REQUIRED)
	@Override
	public CustomerSession validateSession(String sessionid) {
		try {
			CustomerSession cSession = em.find(CustomerSession.class, sessionid);
			if (cSession == null) {
				return null;
			}

			Date now = new Date();

			if (cSession.getTimeoutTime().before(now)) {
				em.remove(cSession);
				return null;
			}
			return cSession;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Transactional(propagation=Propagation.REQUIRED)
	@Override
	public CustomerSession createSession(String customerId) {
		try {
			String sessionId = keyGenerator.generate().toString();
			Date now = new Date();
			Calendar c = Calendar.getInstance();
			c.setTime(now);
			c.add(Calendar.DAY_OF_YEAR, DAYS_TO_ALLOW_SESSION);
			Date expiration = c.getTime();
			CustomerSession cSession = new CustomerSession(sessionId, customerId, now, expiration);
			em.persist(cSession);
			return cSession;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Transactional(propagation=Propagation.REQUIRED)
	@Override
	public void invalidateSession(String sessionid) {
		try {
			CustomerSession cSession = em.find(CustomerSession.class, sessionid);
			if (cSession != null) {
				em.remove(cSession);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
}
