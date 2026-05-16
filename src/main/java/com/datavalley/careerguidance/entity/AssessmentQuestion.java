package com.datavalley.careerguidance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "assessment_question")
public class AssessmentQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TraitCategory traitCategory;

    @Column(nullable = false)
    private String optionAText;

    @Column(nullable = false)
    private Integer optionAWeight;

    @Column(nullable = false)
    private String optionBText;

    @Column(nullable = false)
    private Integer optionBWeight;

    @Column(nullable = false)
    private String optionCText;

    @Column(nullable = false)
    private Integer optionCWeight;

    @Column(nullable = false)
    private String optionDText;

    @Column(nullable = false)
    private Integer optionDWeight;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public TraitCategory getTraitCategory() {
        return traitCategory;
    }

    public void setTraitCategory(TraitCategory traitCategory) {
        this.traitCategory = traitCategory;
    }

    public String getOptionAText() {
        return optionAText;
    }

    public void setOptionAText(String optionAText) {
        this.optionAText = optionAText;
    }

    public Integer getOptionAWeight() {
        return optionAWeight;
    }

    public void setOptionAWeight(Integer optionAWeight) {
        this.optionAWeight = optionAWeight;
    }

    public String getOptionBText() {
        return optionBText;
    }

    public void setOptionBText(String optionBText) {
        this.optionBText = optionBText;
    }

    public Integer getOptionBWeight() {
        return optionBWeight;
    }

    public void setOptionBWeight(Integer optionBWeight) {
        this.optionBWeight = optionBWeight;
    }

    public String getOptionCText() {
        return optionCText;
    }

    public void setOptionCText(String optionCText) {
        this.optionCText = optionCText;
    }

    public Integer getOptionCWeight() {
        return optionCWeight;
    }

    public void setOptionCWeight(Integer optionCWeight) {
        this.optionCWeight = optionCWeight;
    }

    public String getOptionDText() {
        return optionDText;
    }

    public void setOptionDText(String optionDText) {
        this.optionDText = optionDText;
    }

    public Integer getOptionDWeight() {
        return optionDWeight;
    }

    public void setOptionDWeight(Integer optionDWeight) {
        this.optionDWeight = optionDWeight;
    }
}
