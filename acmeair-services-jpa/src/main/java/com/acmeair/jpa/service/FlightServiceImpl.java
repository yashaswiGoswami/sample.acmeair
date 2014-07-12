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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.acmeair.entities.AirportCodeMapping;
import com.acmeair.entities.Flight;
import com.acmeair.entities.FlightPK;
import com.acmeair.entities.FlightSegment;
import com.acmeair.service.FlightService;
import com.acmeair.service.KeyGenerator;

@Service("flightService")
public class FlightServiceImpl implements FlightService {
	
	@Autowired
	EntityManager em;
	
	@Resource
	KeyGenerator keyGenerator;
	
	@Override
	public Flight getFlightByFlightKey(FlightPK key) {
		return em.find(Flight.class, key);
	}

	@Override
	public List<Flight> getFlightByAirportsAndDepartureDate(String fromAirport, String toAirport, Date deptDate) {
		List<Flight> flights = new ArrayList<Flight>();
		
		Query q = em.createQuery("SELECT obj FROM FlightSegment obj where obj.destPort=?1 and obj.originPort=?2");
		q.setParameter(1, toAirport);
		q.setParameter(2, fromAirport);
		
		List<FlightSegment> results = (List<FlightSegment>) q.getResultList();		
		for (FlightSegment seg : results) {
			Query qq = em.createQuery("SELECT obj FROM Flight obj where  obj.scheduledDepartureTime=?1 and obj.pkey.flightSegmentId=?2");
			qq.setParameter(1, deptDate);
			qq.setParameter(2,  seg.getFlightName());
			
			List<Flight> foundFlights = (List<Flight>) qq.getResultList();
			for (Flight flight : foundFlights) {
				flight.setFlightSegment(seg);
				flights.add(flight);
			}
		}
		
		return flights;
	}

	@Override
	public List<Flight> getFlightByAirports(String fromAirport, String toAirport) {
		Query q = em.createQuery("SELECT obj FROM FlightSegment obj where obj.destPort=?1 and obj.originPort=?2");
		q.setParameter(1, toAirport);
		q.setParameter(2, fromAirport);
		
		List<Flight> flights = new ArrayList<Flight>();
		
		List<FlightSegment> results = (List<FlightSegment>) q.getResultList();
		for (FlightSegment seg : results) {
			Query qq = em.createQuery("SELECT obj FROM Flight obj where obj.pkey.flightSegmentId=?1");
			qq.setParameter(1,  seg.getFlightName());
			
			List<Flight> foundFlights = (List<Flight>) qq.getResultList();
			for (Flight flight : foundFlights) {
				flight.setFlightSegment(seg);
				flights.add(flight);
			}
		}
		
		return flights;
	}

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void storeAirportMapping(AirportCodeMapping mapping) {
        Query q = em.createQuery("SELECT obj FROM AirportCodeMapping obj where obj.id=?1 and obj.airportName=?2");
        q.setParameter(1, mapping.getAirportCode());
        q.setParameter(2, mapping.getAirportName());
        if (q.getResultList().isEmpty()) {
            try {
                em.persist(mapping);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

	@Transactional(propagation=Propagation.REQUIRED)
	@Override
	public Flight createNewFlight(String flightSegmentId,
			Date scheduledDepartureTime, Date scheduledArrivalTime,
			BigDecimal firstClassBaseCost, BigDecimal economyClassBaseCost,
			int numFirstClassSeats, int numEconomyClassSeats,
			String airplaneTypeId) {
		try {
			String id = keyGenerator.generate().toString();
			Flight flight = new Flight(id, flightSegmentId,
					scheduledDepartureTime, scheduledArrivalTime,
					firstClassBaseCost, economyClassBaseCost,
					numFirstClassSeats, numEconomyClassSeats, airplaneTypeId);
			em.persist(flight);
			return flight;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}	
	}

	@Transactional(propagation=Propagation.REQUIRED)
	@Override
	public void storeFlightSegment(FlightSegment flightSeg) {		
		try {
			em.persist(flightSeg);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
