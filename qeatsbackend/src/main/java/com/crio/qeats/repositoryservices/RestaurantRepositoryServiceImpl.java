/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.repositoryservices;

import ch.hsr.geohash.GeoHash;
import com.crio.qeats.configs.RedisConfiguration;
import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.globals.GlobalConstants;
import com.crio.qeats.models.ItemEntity;
import com.crio.qeats.models.MenuEntity;
import com.crio.qeats.models.RestaurantEntity;
import com.crio.qeats.repositories.ItemRepository;
import com.crio.qeats.repositories.MenuRepository;
import com.crio.qeats.repositories.RestaurantRepository;
import com.crio.qeats.utils.GeoLocation;
import com.crio.qeats.utils.GeoUtils;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

@Primary
@Service
public class RestaurantRepositoryServiceImpl implements RestaurantRepositoryService {

  @Autowired
  RestaurantRepository restaurantRepository;

  @Autowired
  MenuRepository menuRepository;

  @Autowired
  ItemRepository itemRepository;
  @Autowired
  private RedisConfiguration redisConfiguration;

  private ObjectMapper objectMapper = new ObjectMapper();

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private Provider<ModelMapper> modelMapperProvider;

  private boolean isOpenNow(LocalTime time, RestaurantEntity res) {
    LocalTime openingTime = LocalTime.parse(res.getOpensAt());
    LocalTime closingTime = LocalTime.parse(res.getClosesAt());

    return time.isAfter(openingTime) && time.isBefore(closingTime);
  }

  // TODO: CRIO_TASK_MODULE_NOSQL
  // Objectives:
  // 1. Implement findAllRestaurantsCloseby.
  // 2. Remember to keep the precision of GeoHash in mind while using it as a key.
  // Check RestaurantRepositoryService.java file for the interface contract.

  public List<Restaurant> findAllRestaurantsCloseBy(Double latitude, Double longitude,
       LocalTime currentTime, Double servingRadiusInKms) {

    List<Restaurant> restaurants = null;

    if (redisConfiguration.isCacheAvailable()) {
      restaurants = findAllRestaurantsCloseByFromCache(latitude, longitude,
       currentTime, servingRadiusInKms);
    } else {
      restaurants =  findAllRestaurantsCloseFromDb(latitude, longitude,
       currentTime, servingRadiusInKms);
    }

    return restaurants;

  
  }

  private List<Restaurant> findAllRestaurantsCloseByFromCache(Double latitude, Double longitude,
      LocalTime currentTime, Double servingRadiusInKms) {

    GeoLocation geoLocation = new GeoLocation(latitude,longitude);

    GeoHash geoHash = GeoHash.withCharacterPrecision(geoLocation.getLatitude(),
            geoLocation.getLongitude(), 7);
    String key = geoHash.toBase32();
    
    List<Restaurant> restaurants = new ArrayList<>();

    Jedis jedis = redisConfiguration.getJedisPool().getResource();

    if (jedis.get(key) != null) {
    
      try {
            
        restaurants = objectMapper.readValue(jedis.get(key), new TypeReference<List<Restaurant>>() {
    
            });
      } catch (IOException e) {
        e.printStackTrace();
      }
         
    
    } else {
      restaurants = findAllRestaurantsCloseFromDb(geoLocation.getLatitude(),
       geoLocation.getLongitude(), currentTime, servingRadiusInKms);

      try {

        String jsonString = new ObjectMapper().writeValueAsString(restaurants);
        jedis.setex(geoHash.toBase32(),
            GlobalConstants.REDIS_ENTRY_EXPIRY_IN_SECONDS, jsonString);

      } catch (IOException e) {
        e.printStackTrace();
      }

    }      

    return restaurants;
  }
  
  private List<Restaurant> findAllRestaurantsCloseFromDb(Double latitude, Double longitude,
       LocalTime currentTime, Double servingRadiusInKms) {

    List<RestaurantEntity> restaurantEntity = restaurantRepository.findAll();

    // List<RestaurantEntity> restaurantEntity =
    // mongoTemplate.findAll(RestaurantEntity.class);
  
    List<Restaurant> restaurants = new ArrayList<>();
    
    for (RestaurantEntity re : restaurantEntity) {
    
      if (isRestaurantCloseByAndOpen(re, currentTime, latitude, longitude, servingRadiusInKms)) {
    
        Restaurant res = modelMapperProvider.get().map(re, Restaurant.class);
           
        restaurants.add(res);
      }
    }
        
    System.out.println("RestaurantRepositoryServiceImpl" + restaurants);
    
    try {
      String jsonString = objectMapper.writeValueAsString(restaurants);
      
    } catch (IOException e) {
      e.printStackTrace();
    }
        
    return restaurants;      
  }

  /**
   * Utility method to check if a restaurant is within the serving radius at a given time.
   * @return boolean True if restaurant falls within serving radius and is open, false otherwise
   */
  private boolean isRestaurantCloseByAndOpen(RestaurantEntity restaurantEntity,
      LocalTime currentTime, Double latitude, Double longitude, Double servingRadiusInKms) {
    if (isOpenNow(currentTime, restaurantEntity)) {
      return GeoUtils.findDistanceInKm(latitude, longitude,
          restaurantEntity.getLatitude(), restaurantEntity.getLongitude())
          < servingRadiusInKms;
    }

    return false;
  }

  @Override
  public List<Restaurant> findRestaurantsByName(Double latitude, Double longitude,
       String searchString,LocalTime currentTime, Double servingRadiusInKms) {
    
    List<RestaurantEntity> restaurantEntity = restaurantRepository
        .findRestaurantsByNameExact(searchString).get();

    List<Restaurant> restaurants = new ArrayList<>();

    for (RestaurantEntity re : restaurantEntity) {

      if (isRestaurantCloseByAndOpen(re, currentTime, latitude, longitude, servingRadiusInKms)) {

        Restaurant res = modelMapperProvider.get().map(re, Restaurant.class);
       
        restaurants.add(res);
      }
    }
    
 
    return restaurants;
  }

  @Override
  public List<Restaurant> findRestaurantsByAttributes(Double latitude, Double longitude,
      String searchString,LocalTime currentTime, Double servingRadiusInKms) {

    List<RestaurantEntity> restaurantEntity = restaurantRepository.findByAttributes(searchString);
    List<Restaurant> restaurants = new ArrayList<>();

    for (RestaurantEntity re : restaurantEntity) {

      if (isRestaurantCloseByAndOpen(re, currentTime, latitude, longitude, servingRadiusInKms)) {

        Restaurant res = modelMapperProvider.get().map(re, Restaurant.class);
       
        restaurants.add(res);
      }
    }    
    return restaurants;
  }

  @Override
  public List<Restaurant> findRestaurantsByItemName(Double latitude, Double longitude,
      String searchString,LocalTime currentTime, Double servingRadiusInKms) {

    List<MenuEntity> menu = menuRepository.findByItems(searchString); 

    List<Restaurant> restaurants = new ArrayList<>();

    for (MenuEntity me : menu) {

      String id = me.getRestaurantId();
    
      RestaurantEntity re = restaurantRepository.findById(id).get();

      if (isRestaurantCloseByAndOpen(re, currentTime, latitude, longitude, servingRadiusInKms)) {

        Restaurant res = modelMapperProvider.get().map(re, Restaurant.class);
       
        restaurants.add(res);
      }
    }
    return restaurants;
  }

  @Override
  public List<Restaurant> findRestaurantsByItemAttributes(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {

    
    List<ItemEntity> itemEntity = itemRepository.findByAttributes(searchString);
   

    List<Restaurant> restaurants = new ArrayList<>();
    List<String> itemId = new ArrayList<>();

    for (ItemEntity re : itemEntity) {
      itemId.add(re.getId());
      
    }

    List<MenuEntity> menuEntity = menuRepository.findMenusByItemsItemIdIn(itemId).get();

    for (MenuEntity me : menuEntity) {

      String id = me.getRestaurantId();
      RestaurantEntity re = restaurantRepository.findById(id).get();

      if (isRestaurantCloseByAndOpen(re, currentTime, latitude, longitude, servingRadiusInKms)) {

        Restaurant res = modelMapperProvider.get().map(re, Restaurant.class);
       
        restaurants.add(res);
      }
    }
    return restaurants;
  }
}

