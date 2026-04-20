package com.cms.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.SearchResponse;
import com.cms.dto.SearchResultItem;
import com.cms.repository.DepartmentRepository;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.StudentRepository;

/**
 * REST controller providing global search across students, faculty, enquiries, and departments.
 */
@RestController
@RequestMapping("/search")
public class SearchController {

    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final EnquiryRepository enquiryRepository;
    private final DepartmentRepository departmentRepository;

    public SearchController(StudentRepository studentRepository,
                            FacultyRepository facultyRepository,
                            EnquiryRepository enquiryRepository,
                            DepartmentRepository departmentRepository) {
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.enquiryRepository = enquiryRepository;
        this.departmentRepository = departmentRepository;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SearchResponse> search(
            @RequestParam(name = "q", defaultValue = "") String q,
            @RequestParam(name = "limit", defaultValue = "10") int limit) {

        if (q == null || q.trim().length() < 2) {
            return ResponseEntity.ok(new SearchResponse(List.of()));
        }

        int effectiveLimit = Math.min(limit, 20);
        String term = q.trim().toLowerCase(Locale.ROOT);
        List<SearchResultItem> results = new ArrayList<>();

        studentRepository.findAll().stream()
            .filter(s -> {
                String fullName = (s.getFirstName() + " " + s.getLastName()).toLowerCase(Locale.ROOT);
                String roll = s.getRollNumber() != null ? s.getRollNumber().toLowerCase(Locale.ROOT) : "";
                return fullName.contains(term) || roll.contains(term);
            })
            .map(s -> new SearchResultItem(
                "STUDENT",
                s.getId(),
                s.getFirstName() + " " + s.getLastName(),
                s.getRollNumber(),
                "/students/" + s.getId()))
            .forEach(results::add);

        facultyRepository.findAll().stream()
            .filter(f -> (f.getFirstName() + " " + f.getLastName()).toLowerCase(Locale.ROOT).contains(term))
            .map(f -> new SearchResultItem(
                "FACULTY",
                f.getId(),
                f.getFirstName() + " " + f.getLastName(),
                f.getDepartment() != null ? f.getDepartment().getName() : "",
                "/faculty/" + f.getId()))
            .forEach(results::add);

        enquiryRepository.findAll().stream()
            .filter(e -> {
                String name = e.getName() != null ? e.getName().toLowerCase(Locale.ROOT) : "";
                String phone = e.getPhone() != null ? e.getPhone().toLowerCase(Locale.ROOT) : "";
                return name.contains(term) || phone.contains(term);
            })
            .map(e -> new SearchResultItem(
                "ENQUIRY",
                e.getId(),
                e.getName(),
                e.getPhone(),
                "/enquiries/" + e.getId()))
            .forEach(results::add);

        departmentRepository.findAll().stream()
            .filter(d -> d.getName().toLowerCase(Locale.ROOT).contains(term))
            .map(d -> new SearchResultItem(
                "DEPARTMENT",
                d.getId(),
                d.getName(),
                "",
                "/departments/" + d.getId()))
            .forEach(results::add);

        List<SearchResultItem> limited = results.size() > effectiveLimit
            ? results.subList(0, effectiveLimit)
            : results;

        return ResponseEntity.ok(new SearchResponse(limited));
    }
}
