package com.datavalley.careerguidance.ai;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;
import com.datavalley.careerguidance.entity.Career;
import com.datavalley.careerguidance.entity.CareerSkill;
import com.datavalley.careerguidance.entity.EducationLevel;
import com.datavalley.careerguidance.entity.PersonalityType;
import com.datavalley.careerguidance.entity.Skill;
import com.datavalley.careerguidance.entity.UserProfile;
import com.datavalley.careerguidance.entity.UserSkill;

class CareerDatasetModelTest {

    private static final String CSV_HEADER =
        "What is your name?,What is your gender?,What was your course in UG?,What is your UG specialization? Major Subject (Eg; Mathematics),"
            + "What are your interests?,What are your skills ? (Select multiple if necessary),"
            + "What was the average CGPA or Percentage obtained in under graduation?,Did you do any certification courses additionally?,"
            + "\"If yes, please specify your certificate course title.\",Are you working?,"
            + "\"If yes, then what is/was your first Job title in your current field of work? If not applicable, write NA.               \","
            + "\"Have you done masters after undergraduation? If yes, mention your field of masters.(Eg; Masters in Mathematics)\"";

    @TempDir
    Path tempDir;

    @Test
    void scoresSoftwareProfileHigherForSoftwareCareer() throws IOException {
        CareerDatasetModel model = new CareerDatasetModel(new DefaultResourceLoader(), writeDataset().toString());

        Career softwareCareer = career("Software Developer", "Software Engineering", PersonalityType.ANALYTICAL);
        Career marketingCareer = career("Digital Marketer", "Marketing", PersonalityType.SOCIAL);
        UserProfile softwareProfile = profile("B.Tech", "Technology", "Build software products", PersonalityType.ANALYTICAL);
        List<UserSkill> softwareSkills = List.of(
            userSkill("Java", 5),
            userSkill("Python", 4),
            userSkill("SQL", 4),
            userSkill("Spring Boot", 4)
        );

        double softwareScore = model.score(softwareProfile, softwareSkills, null, softwareCareer, careerSkills("Java", "SQL", "Python")).score();
        double marketingScore = model.score(softwareProfile, softwareSkills, null, marketingCareer, careerSkills("SEO", "Content Strategy", "Communication")).score();

        assertThat(model.snapshot().prototypes()).containsKey("software_developer");
        assertThat(softwareScore).isGreaterThan(marketingScore);
        assertThat(softwareScore).isGreaterThan(20.0);
    }

    @Test
    void scoresDataProfileHigherForDataCareer() throws IOException {
        CareerDatasetModel model = new CareerDatasetModel(new DefaultResourceLoader(), writeDataset().toString());

        Career dataCareer = career("Data Analyst", "Data & Analytics", PersonalityType.STRUCTURED);
        Career softwareCareer = career("Software Developer", "Software Engineering", PersonalityType.ANALYTICAL);
        UserProfile dataProfile = profile("B.Com", "Data analytics", "Work with business dashboards and reporting", PersonalityType.STRUCTURED);
        List<UserSkill> dataSkills = List.of(
            userSkill("Excel", 5),
            userSkill("SQL", 4),
            userSkill("Statistics", 4),
            userSkill("Power BI", 4)
        );

        double dataScore = model.score(dataProfile, dataSkills, null, dataCareer, careerSkills("Excel", "SQL", "Statistics", "Power BI")).score();
        double softwareScore = model.score(dataProfile, dataSkills, null, softwareCareer, careerSkills("Java", "Spring Boot", "Python")).score();

        assertThat(model.snapshot().prototypes()).containsKey("data_analyst");
        assertThat(dataScore).isGreaterThan(softwareScore);
        assertThat(dataScore).isGreaterThan(20.0);
    }

    @Test
    void loadsRepositoryDataset() {
        CareerDatasetModel model = new CareerDatasetModel(new DefaultResourceLoader(), "data/career_recommender.csv");

        assertThat(model.snapshot().prototypes()).isNotEmpty();
        assertThat(model.snapshot().prototypes()).containsKeys("software_developer", "data_analyst");
        assertThat(model.snapshot().prototypes().get("software_developer").support()).isGreaterThan(0);
    }

    private Path writeDataset() throws IOException {
        Path path = tempDir.resolve("career_dataset_sample.csv");
        List<String> lines = List.of(
            CSV_HEADER,
            row("Asha", "Female", "B.Tech", "Computer Science Engineering", "Technology;Software Job", "Java;Python;SQL;Spring Boot", "82", "Yes", "AWS", "Yes", "Software Developer", ""),
            row("Rahul", "Male", "B.Com", "Commerce", "Financial Analysis;Data analytics", "Excel;SQL;Statistics;Power BI", "76", "Yes", "Google Data Analytics", "Yes", "Data Analyst", ""),
            row("Neha", "Female", "B.Tech", "Computer Science Engineering", "Cybersecurity;Technology", "Networking;Linux;Python", "79", "Yes", "Security+", "Yes", "Cyber Security Analyst", ""),
            row("Ishita", "Female", "BBA", "Marketing", "Sales/Marketing;Digital marketing", "Communication Skills;SEO;Content Strategy", "74", "Yes", "HubSpot", "Yes", "Digital Marketing Executive", ""),
            row("Arun", "Male", "B.Tech", "Computer Science Engineering", "Cloud computing;Technology", "Linux;Networking;Cloud Basics;Python", "81", "Yes", "AWS Cloud Practitioner", "Yes", "Cloud Engineer", "")
        );
        Files.write(path, lines);
        return path;
    }

    private String row(String name,
                       String gender,
                       String course,
                       String specialization,
                       String interests,
                       String skills,
                       String cgpa,
                       String certified,
                       String certificateTitle,
                       String working,
                       String jobTitle,
                       String masters) {
        return csv(name) + "," + csv(gender) + "," + csv(course) + "," + csv(specialization) + ","
            + csv(interests) + "," + csv(skills) + "," + csv(cgpa) + "," + csv(certified) + ","
            + csv(certificateTitle) + "," + csv(working) + "," + csv(jobTitle) + "," + csv(masters);
    }

    private String csv(String value) {
        String safe = value == null ? "" : value.replace("\"", "\"\"");
        return "\"" + safe + "\"";
    }

    private UserProfile profile(String course,
                                String preferredIndustry,
                                String longTermGoal,
                                PersonalityType personalityType) {
        UserProfile profile = new UserProfile();
        profile.setCourse(course);
        profile.setPreferredIndustry(preferredIndustry);
        profile.setLongTermGoal(longTermGoal);
        profile.setEducationLevel(EducationLevel.UNDERGRADUATE);
        profile.setPersonalityType(personalityType);
        profile.setInterests(new LinkedHashSet<>(List.of(preferredIndustry)));
        return profile;
    }

    private Career career(String name, String domain, PersonalityType personalityType) {
        Career career = new Career();
        career.setName(name);
        career.setDomain(domain);
        career.setDescription(domain + " projects and career growth");
        career.setRequiredEducation(EducationLevel.UNDERGRADUATE);
        career.setPreferredPersonality(personalityType);
        career.setRelatedIndustries(new LinkedHashSet<>(List.of(domain)));
        return career;
    }

    private List<CareerSkill> careerSkills(String... skillNames) {
        return List.of(skillNames).stream().map(name -> {
            Skill skill = new Skill();
            skill.setName(name);

            CareerSkill careerSkill = new CareerSkill();
            careerSkill.setSkill(skill);
            careerSkill.setImportanceWeight(20);
            return careerSkill;
        }).toList();
    }

    private UserSkill userSkill(String name, int proficiency) {
        Skill skill = new Skill();
        skill.setName(name);

        UserSkill userSkill = new UserSkill();
        userSkill.setSkill(skill);
        userSkill.setProficiencyLevel(proficiency);
        return userSkill;
    }
}
