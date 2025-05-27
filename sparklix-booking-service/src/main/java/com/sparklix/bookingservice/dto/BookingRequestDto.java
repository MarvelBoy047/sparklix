package com.sparklix.bookingservice.dto;
import jakarta.validation.constraints.Min;
 import jakarta.validation.constraints.NotNull;
 import lombok.Data;
 import lombok.NoArgsConstructor;
 import lombok.AllArgsConstructor;

 @Data
 @NoArgsConstructor
 @AllArgsConstructor
 public class BookingRequestDto {

     @NotNull(message = "Showtime ID is required.")
     private Long showtimeId; // This ID refers to the Showtime in the Show Catalog service

     @NotNull(message = "Number of tickets is required.")
     @Min(value = 1, message = "At least one ticket must be booked.")
     private Integer numberOfTickets;

     // Note: userId will be extracted from the JWT in the service layer, not passed in the request body.
     // Other details like show title, venue name, price will be fetched by the BookingService
     // from the ShowCatalogService based on the showtimeId.
 }