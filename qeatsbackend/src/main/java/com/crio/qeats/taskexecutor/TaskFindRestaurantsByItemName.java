package com.crio.qeats.taskexecutor;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.repositoryservices.RestaurantRepositoryService;

import org.springframework.beans.factory.annotation.Autowired;

public class TaskFindRestaurantsByItemName implements Callable<List<Restaurant>> {

  private Double latitude;
  private Double longitude;
  private String searchString;
  private LocalTime currentTime;
  private Double servingRadiusInKms;
   
  @Autowired
  public RestaurantRepositoryService restaurantRepositoryService;
  
  public TaskFindRestaurantsByItemName(Double latitude, Double longitude,
      String searchString,
        LocalTime currentTime, Double servingRadiusInKms) {

    this.latitude = latitude;
    this.longitude = longitude;
    this.searchString = searchString;
    this.currentTime =  currentTime;
    this.servingRadiusInKms = servingRadiusInKms;
  }
  
  public List<Restaurant> call() {
       
    return restaurantRepositoryService.findRestaurantsByItemName(latitude,
     longitude, searchString, currentTime, servingRadiusInKms);
  }
    

}