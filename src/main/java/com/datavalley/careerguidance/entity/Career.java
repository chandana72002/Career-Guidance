package com.datavalley.careerguidance.entity;

import java.util.LinkedHashSet;
import java.util.Set;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "career")
public class Career {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String domain;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EducationLevel requiredEducation;

    @Column(nullable = false)
    private String futureScope;

    @Column(nullable = false)
    private String salaryRange;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PersonalityType preferredPersonality;

    private Integer analyticalWeight;

    private Integer creativityWeight;

    private Integer leadershipWeight;

    private Integer technicalWeight;

    private Integer communicationWeight;

    private Integer problemSolvingWeight;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "career_industry", joinColumns = @JoinColumn(name = "career_id"))
    @Column(name = "industry")
    private Set<String> relatedIndustries = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "career_roadmap", joinColumns = @JoinColumn(name = "career_id"))
    @Column(name = "roadmap_step")
    private Set<String> roadmapSteps = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "career_certification", joinColumns = @JoinColumn(name = "career_id"))
    @Column(name = "certification")
    private Set<String> recommendedCertifications = new LinkedHashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public EducationLevel getRequiredEducation() {
        return requiredEducation;
    }

    public void setRequiredEducation(EducationLevel requiredEducation) {
        this.requiredEducation = requiredEducation;
    }

    public String getFutureScope() {
        return futureScope;
    }

    public void setFutureScope(String futureScope) {
        this.futureScope = futureScope;
    }

    public String getSalaryRange() {
        return salaryRange;
    }

    public void setSalaryRange(String salaryRange) {
        this.salaryRange = salaryRange;
    }

    public PersonalityType getPreferredPersonality() {
        return preferredPersonality;
    }

    public void setPreferredPersonality(PersonalityType preferredPersonality) {
        this.preferredPersonality = preferredPersonality;
    }

    public Integer getAnalyticalWeight() {
        return analyticalWeight;
    }

    public void setAnalyticalWeight(Integer analyticalWeight) {
        this.analyticalWeight = analyticalWeight;
    }

    public Integer getCreativityWeight() {
        return creativityWeight;
    }

    public void setCreativityWeight(Integer creativityWeight) {
        this.creativityWeight = creativityWeight;
    }

    public Integer getLeadershipWeight() {
        return leadershipWeight;
    }

    public void setLeadershipWeight(Integer leadershipWeight) {
        this.leadershipWeight = leadershipWeight;
    }

    public Integer getTechnicalWeight() {
        return technicalWeight;
    }

    public void setTechnicalWeight(Integer technicalWeight) {
        this.technicalWeight = technicalWeight;
    }

    public Integer getCommunicationWeight() {
        return communicationWeight;
    }

    public void setCommunicationWeight(Integer communicationWeight) {
        this.communicationWeight = communicationWeight;
    }

    public Integer getProblemSolvingWeight() {
        return problemSolvingWeight;
    }

    public void setProblemSolvingWeight(Integer problemSolvingWeight) {
        this.problemSolvingWeight = problemSolvingWeight;
    }

    public Set<String> getRelatedIndustries() {
        return relatedIndustries;
    }

    public void setRelatedIndustries(Set<String> relatedIndustries) {
        this.relatedIndustries = relatedIndustries;
    }

    public Set<String> getRoadmapSteps() {
        return roadmapSteps;
    }

    public void setRoadmapSteps(Set<String> roadmapSteps) {
        this.roadmapSteps = roadmapSteps;
    }

    public Set<String> getRecommendedCertifications() {
        return recommendedCertifications;
    }

    public void setRecommendedCertifications(Set<String> recommendedCertifications) {
        this.recommendedCertifications = recommendedCertifications;
    }
}
