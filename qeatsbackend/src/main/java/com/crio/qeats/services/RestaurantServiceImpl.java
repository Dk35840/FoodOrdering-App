
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
import com.crio.qeats.taskexecutor.TaskFindRestaurantsByAttributes;
import com.crio.qeats.taskexecutor.TaskFindRestaurantsByItemAttributes;
import com.crio.qeats.taskexecutor.TaskFindRestaurantsByItemName;
import com.crio.qeats.taskexecutor.TaskFindRestaurantsByName;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    List<Restaurant> restaurant =  new ArrayList<>();

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
    
    Double lat = getRestaurantsRequest.getLatitude();
    Double lon = getRestaurantsRequest.getLongitude();
    String str = getRestaurantsRequest.getSearchFor();
    int hour = currentTime.getHour();
    int min = currentTime.getMinute();

    List<Restaurant> restaurant = new ArrayList<>();
    
    if (str.isEmpty()) {
      return new GetRestaurantsResponse(restaurant);
    } 

    TreeSet<Restaurant> set = new TreeSet<>((a,b) -> a.getRestaurantId()
        .compareTo(b.getRestaurantId()));
        
    if (hour >= 8 && hour < 10 || hour == 10 && min == 0 || hour >= 13 && hour < 14
        || hour == 14 && min == 0 || hour >= 19 && hour < 21 || hour == 21 && min == 0) {

      set.addAll(restaurantRepositoryService
          .findRestaurantsByName(lat, lon, str, currentTime,peakHoursServingRadiusInKms));   
      set.addAll(restaurantRepositoryService
          .findRestaurantsByAttributes(lat, lon, str, currentTime, peakHoursServingRadiusInKms));
      set.addAll(restaurantRepositoryService
          .findRestaurantsByItemAttributes(lat, lon, str, currentTime,peakHoursServingRadiusInKms));
      set.addAll(restaurantRepositoryService
          .findRestaurantsByItemName(lat, lon, str, currentTime,peakHoursServingRadiusInKms));
      

    } else {
      
     
      set.addAll(restaurantRepositoryService
          .findRestaurantsByAttributes(lat, lon, str, currentTime, normalHoursServingRadiusInKms));
      
      set.addAll(restaurantRepositoryService
          .findRestaurantsByItemAttributes(lat, lon,str,currentTime,normalHoursServingRadiusInKms));
          
      set.addAll(restaurantRepositoryService
          .findRestaurantsByItemName(lat, lon, str, currentTime,normalHoursServingRadiusInKms));

      
      set.addAll(restaurantRepositoryService
          .findRestaurantsByName(lat, lon, str, currentTime,normalHoursServingRadiusInKms));
      
    }

    restaurant.addAll(set);    

    System.out.println("GetRestaurantsResponse : " + restaurant);

    GetRestaurantsResponse restaurantsResponse = new GetRestaurantsResponse(restaurant);  

    return restaurantsResponse;
  }


  // TODO: CRIO_TASK_MODULE_MULTITHREADING
  // Implement multi-threaded version of RestaurantSearch.
  // Implement variant of findRestaurantsBySearchQuery which is at least 1.5x time faster than
  // findRestaurantsBySearchQuery.
  @Override
  public GetRestaurantsResponse findRestaurantsBySearchQueryMt(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {

    Double lat = getRestaurantsRequest.getLatitude();
    Double lon = getRestaurantsRequest.getLongitude();
    String str = getRestaurantsRequest.getSearchFor();
    int hour = currentTime.getHour();
    int min = currentTime.getMinute();
    ExecutorService es = Executors.newFixedThreadPool(4);
    List<Restaurant> restaurant = new ArrayList<>();

    List<Future<List<Restaurant>>> listOfFutureRestaurant = new ArrayList<>();

    if (str.isEmpty()) {
      return new GetRestaurantsResponse(restaurant);
    } 

    TreeSet<Restaurant> set = new TreeSet<>((a,b) -> a.getRestaurantId()
        .compareTo(b.getRestaurantId()));


    if (hour >= 8 && hour < 10 || hour == 10 && min == 0 || hour >= 13 && hour < 14
        || hour == 14 && min == 0 || hour >= 19 && hour < 21 || hour == 21 && min == 0) {

      listOfFutureRestaurant.add(es.submit(new TaskFindRestaurantsByAttributes(lat,
          lon,str,currentTime,peakHoursServingRadiusInKms)));
      listOfFutureRestaurant.add(es.submit(new TaskFindRestaurantsByItemAttributes(lat,
          lon,str,currentTime,peakHoursServingRadiusInKms)));    
      listOfFutureRestaurant.add(es.submit(new TaskFindRestaurantsByItemName(lat,
          lon,str,currentTime,peakHoursServingRadiusInKms)));
      listOfFutureRestaurant.add(es.submit(new TaskFindRestaurantsByName(lat,
          lon,str,currentTime,peakHoursServingRadiusInKms)));

    } else {
      listOfFutureRestaurant.add(es.submit(new TaskFindRestaurantsByAttributes(lat,
          lon,str,currentTime,normalHoursServingRadiusInKms)));
      listOfFutureRestaurant.add(es.submit(new TaskFindRestaurantsByItemAttributes(lat,
          lon,str,currentTime,normalHoursServingRadiusInKms)));    
      listOfFutureRestaurant.add(es.submit(new TaskFindRestaurantsByItemName(lat,
          lon,str,currentTime,normalHoursServingRadiusInKms)));
      listOfFutureRestaurant.add(es.submit(new TaskFindRestaurantsByName(lat,
          lon,str,currentTime,normalHoursServingRadiusInKms)));
    } 

    restaurant.addAll(set);    

    System.out.println("findRestaurantsBySearchQueryMt : " + restaurant);

    GetRestaurantsResponse restaurantsResponse = new GetRestaurantsResponse(restaurant);  

    return restaurantsResponse;
  }
}

