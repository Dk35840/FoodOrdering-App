package com.crio.qeats.taskexecutor;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.repositoryservices.RestaurantRepositoryService;

import org.springframework.beans.factory.annotation.Autowired;

public class TaskFindRestaurantsByAttributes implements Callable<List<Restaurant>> {

  private Double latitude;
  private Double longitude;
  private String searchString;
  private LocalTime currentTime;
  private Double servingRadiusInKms;
  private RestaurantRepositoryService restaurantRepositoryService; 

  
  public TaskFindRestaurantsByAttributes(Double latitude, Double longitude, String searchString,
        LocalTime currentTime, Double servingRadiusInKms,
        RestaurantRepositoryService restaurantRepositoryService) {

    this.latitude = latitude;
    this.longitude = longitude;
    this.searchString = searchString;
    this.currentTime =  currentTime;
    this.servingRadiusInKms = servingRadiusInKms;
    this.restaurantRepositoryService=restaurantRepositoryService;
  }
  
  public List<Restaurant> call() {
       
    return restaurantRepositoryService.findRestaurantsByAttributes(latitude,
     longitude, searchString, currentTime, servingRadiusInKms);
  }
    

}