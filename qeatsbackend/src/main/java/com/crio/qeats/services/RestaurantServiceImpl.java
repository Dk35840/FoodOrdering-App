
/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.services;

import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.exchanges.GetRestaurantsRequest;
import com.crio.qeats.exchanges.GetRestaurantsResponse;
import com.crio.qeats.repositoryservices.RestaurantRepositoryService;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class RestaurantServiceImpl implements RestaurantService {

  private final Double peakHoursServingRadiusInKms = 3.0;
  private final Double normalHoursServingRadiusInKms = 5.0;
  @Autowired
  private RestaurantRepositoryService restaurantRepositoryService;


  @Override
  public GetRestaurantsResponse findAllRestaurantsCloseBy(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {

    //For peak hours: 8AM - 10AM, 1PM-2PM, 7PM-9PM     

    int hour = currentTime.getHour();
    int min = currentTime.getMinute();

    List<Restaurant> restaurant;

    if (hour >= 8 && hour < 10 || hour == 10 && min == 0 || hour >= 13 && hour < 14
        || hour == 14 && min == 0 || hour >= 19 && hour < 21 || hour == 21 && min == 0) {
      restaurant = restaurantRepositoryService
      .findAllRestaurantsCloseBy(getRestaurantsRequest.getLatitude(),
      getRestaurantsRequest.getLongitude(),currentTime,peakHoursServingRadiusInKms);
    } else {
      restaurant = restaurantRepositoryService
      .findAllRestaurantsCloseBy(getRestaurantsRequest.getLatitude(),
      getRestaurantsRequest.getLongitude(),currentTime,normalHoursServingRadiusInKms);
    }
   
    //Extra to trim the restaurant
    // restaurant = restaurant.subList(0, 50);
    if (restaurant == null) {
      restaurant = new ArrayList<>();
    }

    GetRestaurantsResponse restaurantsResponse = new GetRestaurantsResponse(restaurant);
     
    // System.out.println("Res Called" + restaurantsResponse);

    return restaurantsResponse;

  }


  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Implement findRestaurantsBySearchQuery. The request object has the search string.
  // We have to combine results from multiple sources:
  // 1. Restaurants by name (exact and inexact)
  // 2. Restaurants by cuisines (also called attributes)
  // 3. Restaurants by food items it serves
  // 4. Restaurants by food item attributes (spicy, sweet, etc)
  // Remember, a restaurant must be present only once in the resulting list.
  // Check RestaurantService.java file for the interface contract.
  @Override
  public GetRestaurantsResponse findRestaurantsBySearchQuery(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {

     return null;
  }

}

