package com.sparklix.showcatalogservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparklix.showcatalogservice.client.CatalogShowDataDto; // Assuming this is your DTO from admin
import com.sparklix.showcatalogservice.entity.Show;
import com.sparklix.showcatalogservice.entity.Venue;
import com.sparklix.showcatalogservice.entity.Showtime;
import com.sparklix.showcatalogservice.repository.ShowRepository;
import com.sparklix.showcatalogservice.repository.VenueRepository;
import com.sparklix.showcatalogservice.repository.ShowtimeRepository;
import com.sparklix.showcatalogservice.repository.ReviewRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList; // Added for lists
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final ReviewRepository localReviewRepository;
    private final ObjectMapper objectMapper;


    @Value("${sparklix.service-account.username}")
    private String serviceUsername;

    @Value("${sparklix.service-account.password}")
    private String servicePassword;

    @Value("${sparklix.user-service.login-url}")
    private String userServiceLoginUrl;

    private String serviceAuthToken = null;
    private long serviceTokenExpiryTime = 0;
    // private static final Logger log = LoggerFactory.getLogger(ShowDataSyncService.class); // Duplicate logger removed


    public ShowDataSyncService(@Qualifier("loadBalancedRestTemplate") RestTemplate loadBalancedRestTemplate,
                               @Qualifier("plainRestTemplate") RestTemplate plainRestTemplate,
                               ShowRepository localShowRepository,
                               VenueRepository localVenueRepository,
                               ShowtimeRepository localShowtimeRepository,
                               ReviewRepository localReviewRepository) {
        this.loadBalancedRestTemplate = loadBalancedRestTemplate;
        this.plainRestTemplate = plainRestTemplate;
        this.localShowRepository = localShowRepository;
        this.localVenueRepository = localVenueRepository;
        this.localShowtimeRepository = localShowtimeRepository;
        this.localReviewRepository = localReviewRepository;
        this.objectMapper = new ObjectMapper();
    }

    private String fetchNewServiceToken() {
        logger.debug("SYNC_TOKEN_FETCH_START: Attempting to fetch new service token from: {}", userServiceLoginUrl);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("usernameOrEmail", serviceUsername);
        requestBody.put("password", servicePassword);
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map<String, Object>> responseEntity = plainRestTemplate.exchange(
                userServiceLoginUrl,
                HttpMethod.POST,
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
                            logger.info("SYNC_TOKEN_FETCH_SUCCESS: New service token obtained, expires at: {}", new java.util.Date(this.serviceTokenExpiryTime));
                        } else {
                            this.serviceTokenExpiryTime = System.currentTimeMillis() + (30 * 60 * 1000); // Default 30 mins
                            logger.warn("SYNC_TOKEN_FETCH_WARN: 'exp' claim not found/number in token. Defaulting expiry for cache.");
                        }
                    } catch (Exception e) {
                        this.serviceTokenExpiryTime = System.currentTimeMillis() + (30 * 60 * 1000); // Default 30 mins
                        logger.warn("SYNC_TOKEN_FETCH_WARN: Could not parse token expiry for service account token, using default cache time: {}", e.getMessage());
                    }
                    return "Bearer " + this.serviceAuthToken;
                }
            }
            logger.error("SYNC_TOKEN_FETCH_FAIL: Failed to fetch service token from user-service. Status: {}", responseEntity.getStatusCode());
        } catch (Exception e) {
            logger.error("SYNC_TOKEN_FETCH_ERROR: Error while fetching service token for service account: {}", e.getMessage(), e);
        }
        this.serviceAuthToken = null;
        this.serviceTokenExpiryTime = 0;
        return null;
    }

    private String getValidServiceAuthToken() {
        logger.debug("SYNC_TOKEN_VALIDATE: Checking service token validity.");
        if (serviceAuthToken != null && System.currentTimeMillis() < (serviceTokenExpiryTime - (5 * 60 * 1000))) { // 5 min buffer
            logger.debug("SYNC_TOKEN_VALIDATE: Existing token is still valid.");
            return "Bearer " + serviceAuthToken;
        }
        logger.debug("SYNC_TOKEN_VALIDATE: Existing token is null or expired/expiring soon. Fetching new token.");
        return fetchNewServiceToken();
    }

    @Scheduled(fixedRateString = "${catalog.sync.rate.ms:300000}", initialDelayString = "${catalog.sync.initial.delay.ms:20000}")
    @Transactional
    public void syncShowData() {
        logger.info("SYNC_CYCLE_START: ShowDataSyncService attempting to sync data.");
        String bearerToken = getValidServiceAuthToken();

        if (bearerToken == null) {
            logger.error("SYNC_CYCLE_ABORT_NO_TOKEN: Failed to obtain valid service token for admin-service. Skipping sync cycle.");
            return;
        }
        logger.debug("SYNC_TOKEN_OBTAINED: Successfully got service token for catalog-sync-agent.");

        String adminServiceUrl = "http://ADMIN-SERVICE/api/internal/data/admin/shows-for-catalog";
        logger.debug("SYNC_ADMIN_CALL_INIT: Calling admin service to fetch catalog data from URL: {}", adminServiceUrl);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", bearerToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<List<CatalogShowDataDto>> responseEntity = loadBalancedRestTemplate.exchange(
                adminServiceUrl, HttpMethod.GET, entity,
                new ParameterizedTypeReference<List<CatalogShowDataDto>>() {}
            );
            
            if (!responseEntity.getStatusCode().is2xxSuccessful() || responseEntity.getBody() == null) {
                logger.error("SYNC_ADMIN_CALL_FAILED: Failed to fetch data from admin service. Status: {}. Body: {}", 
                             responseEntity.getStatusCode(), responseEntity.hasBody() ? responseEntity.getBody() : "N/A");
                return;
            }
            List<CatalogShowDataDto> dataFromAdmin = responseEntity.getBody();
            logger.info("SYNC_ADMIN_DATA_RECEIVED: Received {} DTO items from admin service.", dataFromAdmin.size());
             if (logger.isDebugEnabled()) { // Log all DTOs if debug is enabled
                dataFromAdmin.forEach(dto -> logger.debug("SYNC_ADMIN_DTO_RECEIVED: {}", dto));
            }
            
            logger.debug("SYNC_DELETE_START: Performing sync: clearing all existing catalog data before repopulating.");
            localReviewRepository.deleteAllInBatch();
            logger.debug("SYNC_DELETE_REVIEWS_COMPLETE: Cleared local reviews catalog.");
            localShowtimeRepository.deleteAllInBatch();
            logger.debug("SYNC_DELETE_SHOWTIMES_COMPLETE: Cleared local showtimes catalog.");
            localShowRepository.deleteAllInBatch();
            logger.debug("SYNC_DELETE_SHOWS_COMPLETE: Cleared local shows catalog.");
            localVenueRepository.deleteAllInBatch();
            logger.debug("SYNC_DELETE_VENUES_COMPLETE: Cleared local venues catalog.");
            logger.info("SYNC_DELETE_ALL_COMPLETE: Local catalog tables (reviews, showtimes, shows, venues) cleared.");
            
            if (dataFromAdmin.isEmpty()) {
                logger.info("SYNC_NO_DATA_RECEIVED: No show data received from admin service to populate. Catalog remains empty.");
                logger.info("SYNC_CYCLE_END_EMPTY: ShowDataSyncService finished sync attempt with no data to process.");
                return;
            }

            // --- Venue Sync Phase ---
            logger.debug("SYNC_VENUES_START: Processing and saving Venues...");
            Map<Long, Venue> savedVenuesMap = new HashMap<>();
            for (CatalogShowDataDto dto : dataFromAdmin) {
                if (dto.venueId() != null && !savedVenuesMap.containsKey(dto.venueId())) {
                    Venue venueToSave = new Venue(null, dto.venueId(), dto.venueName(), dto.venueAddress(), dto.venueCity(), dto.venueCapacity());
                    logger.debug("SYNC_PROCESSING_VENUE: Preparing to save Venue. OriginalVenueId: {}, Name: {}", venueToSave.getOriginalVenueId(), venueToSave.getName());
                    Venue savedVenue = localVenueRepository.save(venueToSave);
                    savedVenuesMap.put(savedVenue.getOriginalVenueId(), savedVenue);
                    logger.debug("SYNC_SAVED_VENUE: Saved Venue. Local ID: {}, OriginalVenueId: {}, Name: {}", savedVenue.getId(), savedVenue.getOriginalVenueId(), savedVenue.getName());
                }
            }
            logger.info("SYNC_VENUES_COMPLETE: Saved {} unique venues.", savedVenuesMap.size());

            // --- Show Sync Phase ---
            logger.debug("SYNC_SHOWS_START: Processing and saving Shows...");
            Map<Long, Show> savedShowsMap = new HashMap<>();
            for (CatalogShowDataDto dto : dataFromAdmin) {
                 if (dto.showId() != null && !savedShowsMap.containsKey(dto.showId())) {
                    Show showToSave = new Show(null, dto.showId(), dto.title(), dto.description(), dto.genre(), dto.language(), dto.durationMinutes(), dto.releaseDate(), dto.posterUrl());
                    logger.debug("SYNC_PROCESSING_SHOW: Preparing to save Show. OriginalShowId: {}, Title: {}", showToSave.getOriginalShowId(), showToSave.getTitle());
                    Show savedShow = localShowRepository.save(showToSave);
                    savedShowsMap.put(savedShow.getOriginalShowId(), savedShow);
                    logger.debug("SYNC_SAVED_SHOW: Saved Show. Local ID: {}, OriginalShowId: {}, Title: {}", savedShow.getId(), savedShow.getOriginalShowId(), savedShow.getTitle());
                 }
            }
            logger.info("SYNC_SHOWS_COMPLETE: Saved {} unique shows.", savedShowsMap.size());
            
            // --- Showtime Sync Phase ---
            logger.debug("SYNC_SHOWTIMES_START: Processing and saving Showtimes...");
            int showtimesSavedCount = 0;
            for (CatalogShowDataDto dto : dataFromAdmin) {
                if (dto.showtimeId() == null) { 
                    logger.warn("SYNC_SKIPPING_SHOWTIME_NULL_ID: Skipping DTO for showtime processing as originalShowtimeId is null. DTO: {}", dto);
                    continue;
                }
                
                Show localShow = savedShowsMap.get(dto.showId()); 
                Venue localVenue = savedVenuesMap.get(dto.venueId());

                if (localShow != null && localVenue != null) {
                    Showtime showtimeToSave = new Showtime(
                        null, 
                        dto.showtimeId(), 
                        localShow,        
                        localVenue,       
                        dto.showDateTime(),
                        dto.pricePerSeat(),
                        dto.totalSeats()
                    );
                    logger.debug("SYNC_PROCESSING_SHOWTIME: Preparing to save Showtime. OriginalShowtimeId: {}, for Show (Original ID: {}, Local ID: {}) and Venue (Original ID: {}, Local ID: {})",
                        showtimeToSave.getOriginalShowtimeId(),
                        localShow.getOriginalShowId(), localShow.getId(),
                        localVenue.getOriginalVenueId(), localVenue.getId());
                    Showtime savedShowtime = localShowtimeRepository.save(showtimeToSave);
                    showtimesSavedCount++;
                    logger.debug("SYNC_SAVED_SHOWTIME: Saved Showtime. Local ID: {}, OriginalShowtimeId: {}, Linked to LocalShowID: {}, LocalVenueID: {}",
                        savedShowtime.getId(), savedShowtime.getOriginalShowtimeId(), localShow.getId(), localVenue.getId());
                } else {
                    logger.warn("SYNC_SKIPPING_SHOWTIME_LINK_FAIL: Could not save showtime (Original ID: {}) due to missing local Show (Original ID: {}) or Venue (Original ID: {}) in current batch's saved maps. Admin DTO: {}", 
                        dto.showtimeId(), dto.showId(), dto.venueId(), dto);
                }
            }
            logger.info("SYNC_SHOWTIMES_COMPLETE: Saved {} showtimes.", showtimesSavedCount);
            logger.info("SYNC_CYCLE_SUCCESS: Show data synchronization completed successfully.");

        } catch (HttpClientErrorException e) {
            logger.error("SYNC_ERROR_HTTP: HTTP error during sync with admin-service (URL: {}): {} - Response: {}", adminServiceUrl, e.getStatusCode(), e.getResponseBodyAsString(), e);
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                logger.warn("SYNC_ERROR_HTTP_AUTH: Authentication/Authorization error calling admin-service. Invalidating cached service token.");
                this.serviceAuthToken = null; 
                this.serviceTokenExpiryTime = 0;
            }
        } catch (Exception e) {
            logger.error("SYNC_ERROR_UNEXPECTED: Generic error during show data synchronization with admin-service (URL: {}): {}", adminServiceUrl, e.getMessage(), e);
        }
        logger.info("SYNC_CYCLE_END: ShowDataSyncService finished sync attempt.");
    }
}