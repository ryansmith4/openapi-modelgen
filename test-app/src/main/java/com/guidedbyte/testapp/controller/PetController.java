package com.guidedbyte.testapp.controller;

import com.guidedbyte.testapp.model.pets.PetDto;
import com.guidedbyte.testapp.model.pets.CategoryDto;
import com.guidedbyte.testapp.model.pets.TagDto;
import com.guidedbyte.testapp.model.pets.PetSummaryDto;
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
    public ResponseEntity<PetDto> getPet(@PathVariable Long id) {
        // Create a sample pet using generated DTOs
        CategoryDto category = new CategoryDto()
            .id(1L)
            .name("Dogs");
        
        TagDto tag = new TagDto()
            .id(10L)
            .name("friendly")
            .color("#FF5733");
        
        List<TagDto> tags = new ArrayList<>();
        tags.add(tag);
        
        List<URI> photoUrls = new ArrayList<>();
        photoUrls.add( URI.create( "https://example.com/photo1.jpg" ) );
        
        PetDto pet = new PetDto()
            .id(id)
            .name("Fluffy")
            .category(category)
            .status(PetDto.StatusEnum.AVAILABLE)
            .tags(tags)
            .photoUrls(photoUrls)
            .birthDate(LocalDate.of(2020, 1, 15))
            .weight(12.5);
        
        return ResponseEntity.ok(pet);
    }
    
    @PostMapping
    public ResponseEntity<PetDto> createPet(@Valid @RequestBody PetDto pet) {
        // In a real app, this would save to database
        pet.id(System.currentTimeMillis());
        return ResponseEntity.ok(pet);
    }
    
    @GetMapping("/summary")
    public ResponseEntity<List<PetSummaryDto>> getPetSummaries() {
        List<PetSummaryDto> summaries = new ArrayList<>();
        
        PetSummaryDto summary = new PetSummaryDto()
            .id(1L)
            .name("Fluffy")
            .status(PetSummaryDto.StatusEnum.AVAILABLE);
        
        summaries.add(summary);
        
        return ResponseEntity.ok(summaries);
    }
}