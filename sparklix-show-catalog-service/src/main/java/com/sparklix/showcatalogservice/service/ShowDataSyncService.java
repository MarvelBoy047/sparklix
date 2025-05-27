package com.sparklix.showcatalogservice.service;

import com.fasterxml.jackson.core.type.TypeReference; // For ObjectMapper
import com.fasterxml.jackson.databind.ObjectMapper;   // For ObjectMapper
import com.sparklix.showcatalogservice.client.CatalogShowDataDto;
import com.sparklix.showcatalogservice.entity.Show;
import com.sparklix.showcatalogservice.entity.Venue;
import com.sparklix.showcatalogservice.entity.Showtime;
import com.sparklix.showcatalogservice.repository.ShowRepository;
import com.sparklix.showcatalogservice.repository.VenueRepository;
import com.sparklix.showcatalogservice.repository.ShowtimeRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*; // Includes HttpMethod
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64; // For JWT parsing
import java.util.HashMap; // For requestBody map
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ShowDataSyncService {
    private static final Logger logger = LoggerFactory.getLogger(ShowDataSyncService.class);

    private final RestTemplate loadBalancedRestTemplate;
    private final RestTemplate plainRestTemplate;
    private final ShowRepository localShowRepository;
    private final VenueRepository localVenueRepository;
    private final ShowtimeRepository localShowtimeRepository;
    private final ObjectMapper objectMapper; // For parsing JWT payload

    @Value("${sparklix.service-account.username}")
    private String serviceUsername;

    @Value("${sparklix.service-account.password}")
    private String servicePassword;

    @Value("${sparklix.user-service.login-url}")
    private String userServiceLoginUrl;

    private String serviceAuthToken = null;
    private long serviceTokenExpiryTime = 0;

    public ShowDataSyncService(@Qualifier("loadBalancedRestTemplate") RestTemplate loadBalancedRestTemplate,
                               @Qualifier("plainRestTemplate") RestTemplate plainRestTemplate,
                               ShowRepository localShowRepository,
                               VenueRepository localVenueRepository,
                               ShowtimeRepository localShowtimeRepository) {
        this.loadBalancedRestTemplate = loadBalancedRestTemplate;
        this.plainRestTemplate = plainRestTemplate;
        this.localShowRepository = localShowRepository;
        this.localVenueRepository = localVenueRepository;
        this.localShowtimeRepository = localShowtimeRepository;
        this.objectMapper = new ObjectMapper(); // Initialize ObjectMapper
    }

    private String fetchNewServiceToken() {
        logger.info("CATALOG-SYNC: Attempting to fetch new service token from: {}", userServiceLoginUrl);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("usernameOrEmail", serviceUsername);
        requestBody.put("password", servicePassword);
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
        	logger.info("CATALOG-SYNC: Calling User Service Login URL: {}", userServiceLoginUrl);
            ResponseEntity<Map<String, Object>> responseEntity = plainRestTemplate.exchange( // Using exchange
                userServiceLoginUrl,
                HttpMethod.POST, // Specify HTTP method
                requestEntity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                Map<String, Object> responseBodyMap = responseEntity.getBody();
                String token = (String) responseBodyMap.get("token");
                if (token != null) {
                    this.serviceAuthToken = token;
                    try {
                        String[] chunks = token.split("\\.");
                        Base64.Decoder decoder = Base64.getUrlDecoder();
                        String payloadJson = new String(decoder.decode(chunks[1]));
                        Map<String, Object> claims = objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>(){});
                        Object expClaim = claims.get("exp");
                        if (expClaim instanceof Number) {
                            this.serviceTokenExpiryTime = ((Number) expClaim).longValue() * 1000;
                            logger.info("CATALOG-SYNC: New service token obtained, expires at: {}", new java.util.Date(this.serviceTokenExpiryTime));
                        } else {
                            this.serviceTokenExpiryTime = System.currentTimeMillis() + (30 * 60 * 1000);
                            logger.warn("CATALOG-SYNC: 'exp' claim not found/number in token. Defaulting expiry for cache.");
                        }
                    } catch (Exception e) {
                        this.serviceTokenExpiryTime = System.currentTimeMillis() + (30 * 60 * 1000);
                        logger.warn("CATALOG-SYNC: Could not parse token expiry for service account token, using default cache time: {}", e.getMessage());
                    }
                    return "Bearer " + this.serviceAuthToken;
                }
            }
            logger.error("CATALOG-SYNC: Failed to fetch service token from user-service. Status: {}", responseEntity.getStatusCode());
        } catch (Exception e) {
            logger.error("CATALOG-SYNC: Error while fetching service token for service account: {}", e.getMessage(), e);
        }
        this.serviceAuthToken = null; 
        this.serviceTokenExpiryTime = 0;
        return null;
    }

    private String getValidServiceAuthToken() {
        if (serviceAuthToken != null && System.currentTimeMillis() < (serviceTokenExpiryTime - (5 * 60 * 1000))) { // 5 min buffer
            return "Bearer " + serviceAuthToken;
        }
        return fetchNewServiceToken();
    }

    @Scheduled(fixedRateString = "${catalog.sync.rate.ms:300000}", initialDelayString = "${catalog.sync.initial.delay.ms:20000}")
    @Transactional
    public void syncShowData() {
        logger.info("CATALOG-SYNC: Starting show data synchronization using RestTemplate...");
        String bearerToken = getValidServiceAuthToken();

        if (bearerToken == null) {
            logger.error("CATALOG-SYNC: Failed to obtain valid service token for admin-service. Skipping sync cycle.");
            return;
        }

        String adminServiceUrl = "http://ADMIN-SERVICE/api/internal/data/admin/shows-for-catalog";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", bearerToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<List<CatalogShowDataDto>> responseEntity = loadBalancedRestTemplate.exchange(
                adminServiceUrl, HttpMethod.GET, entity,
                new ParameterizedTypeReference<List<CatalogShowDataDto>>() {}
            );
            
            if (!responseEntity.getStatusCode().is2xxSuccessful() || responseEntity.getBody() == null) {
                logger.error("CATALOG-SYNC: Failed to fetch data from admin service. Status: {}", responseEntity.getStatusCode());
                return;
            }
            List<CatalogShowDataDto> dataFromAdmin = responseEntity.getBody();
            
            if (dataFromAdmin.isEmpty()) {
                logger.info("CATALOG-SYNC: No show data received from admin service. Clearing local catalog.");
                localShowtimeRepository.deleteAllInBatch(); 
                localShowRepository.deleteAllInBatch(); 
                localVenueRepository.deleteAllInBatch();
                logger.info("CATALOG-SYNC: Local catalog cleared."); 
                return;
            }
            logger.warn("CATALOG-SYNC: Performing naive sync: clearing all existing catalog data.");
            localShowtimeRepository.deleteAllInBatch(); 
            localShowRepository.deleteAllInBatch(); 
            localVenueRepository.deleteAllInBatch();
            
            Map<Long, Venue> savedVenues = dataFromAdmin.stream()
                .filter(dto -> dto.venueId() != null)
                .map(dto -> new Venue(null, dto.venueId(), dto.venueName(), dto.venueAddress(), dto.venueCity(), dto.venueCapacity()))
                .collect(Collectors.toMap(Venue::getOriginalVenueId, Function.identity(), (v1, v2) -> v1))
                .values().stream().map(localVenueRepository::save)
                .collect(Collectors.toMap(Venue::getOriginalVenueId, Function.identity()));

            Map<Long, Show> savedShows = dataFromAdmin.stream()
                .filter(dto -> dto.showId() != null)
                .map(dto -> new Show(null, dto.showId(), dto.title(), dto.description(), dto.genre(), dto.language(), dto.durationMinutes(), dto.releaseDate(), dto.posterUrl()))
                .collect(Collectors.toMap(Show::getOriginalShowId, Function.identity(), (s1, s2) -> s1))
                .values().stream().map(localShowRepository::save)
                .collect(Collectors.toMap(Show::getOriginalShowId, Function.identity()));
            
            for (CatalogShowDataDto dto : dataFromAdmin) {
                if (dto.showId() == null || dto.venueId() == null || dto.showtimeId() == null) {
                    logger.warn("CATALOG-SYNC: Skipping DTO due to null original ID(s): {}", dto);
                    continue;
                }
                Show localShow = savedShows.get(dto.showId()); 
                Venue localVenue = savedVenues.get(dto.venueId());
                if (localShow != null && localVenue != null) {
                    Showtime localShowtime = new Showtime(null, dto.showtimeId(), localShow, localVenue, dto.showDateTime(), dto.pricePerSeat(), dto.totalSeats());
                    localShowtimeRepository.save(localShowtime);
                } else {
                    logger.warn("CATALOG-SYNC: Could not save showtime (orig ID {}) due to missing local Show (orig ID {}) or Venue (orig ID {}).", 
                        dto.showtimeId(), dto.showId(), dto.venueId());
                }
            }
            logger.info("CATALOG-SYNC: Show data synchronization completed. Processed {} DTO items from admin service.", dataFromAdmin.size());

        } catch (HttpClientErrorException e) {
            logger.error("CATALOG-SYNC: HTTP error during sync to {}: {} - Response: {}", adminServiceUrl, e.getStatusCode(), e.getResponseBodyAsString(), e);
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                logger.warn("CATALOG-SYNC: Authentication/Authorization error calling admin-service. Invalidating cached service token.");
                this.serviceAuthToken = null; 
                this.serviceTokenExpiryTime = 0;
            }
        } catch (Exception e) {
            logger.error("CATALOG-SYNC: Generic error during show data synchronization with {}: {}", adminServiceUrl, e.getMessage(), e);
        }
    }
}