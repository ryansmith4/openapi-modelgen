package com.guidedbyte.testapp.controller;

import com.guidedbyte.testapp.model.pets.ApiPetDto;
import com.guidedbyte.testapp.model.pets.ApiCategoryDto;
import com.guidedbyte.testapp.model.pets.ApiTagDto;
import com.guidedbyte.testapp.model.pets.ApiPetSummaryDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/pets")
public class PetController {
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiPetDto> getPet(@PathVariable Long id) {
        // Create a sample pet using generated DTOs
        ApiCategoryDto category = new ApiCategoryDto()
            .id(1L)
            .name("Dogs");
        
        ApiTagDto tag = new ApiTagDto()
            .id(10L)
            .name("friendly")
            .color("#FF5733");
        
        List<ApiTagDto> tags = new ArrayList<>();
        tags.add(tag);
        
        List<URI> photoUrls = new ArrayList<>();
        photoUrls.add( URI.create( "https://example.com/photo1.jpg" ) );
        
        ApiPetDto pet = new ApiPetDto()
            .id(id)
            .name("Fluffy")
            .category(category)
            .status(ApiPetDto.StatusEnum.AVAILABLE)
            .tags(tags)
            .photoUrls(photoUrls)
            .birthDate(LocalDate.of(2020, 1, 15))
            .weight(12.5);
        
        return ResponseEntity.ok(pet);
    }
    
    @PostMapping
    public ResponseEntity<ApiPetDto> createPet(@Valid @RequestBody ApiPetDto pet) {
        // In a real app, this would save to database
        pet.id(System.currentTimeMillis());
        return ResponseEntity.ok(pet);
    }
    
    @GetMapping("/summary")
    public ResponseEntity<List<ApiPetSummaryDto>> getPetSummaries() {
        List<ApiPetSummaryDto> summaries = new ArrayList<>();
        
        ApiPetSummaryDto summary = new ApiPetSummaryDto()
            .id(1L)
            .name("Fluffy")
            .status(ApiPetSummaryDto.StatusEnum.AVAILABLE);
        
        summaries.add(summary);
        
        return ResponseEntity.ok(summaries);
    }
}