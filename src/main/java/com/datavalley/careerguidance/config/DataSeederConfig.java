package com.datavalley.careerguidance.config;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.datavalley.careerguidance.entity.AssessmentQuestion;
import com.datavalley.careerguidance.entity.Career;
import com.datavalley.careerguidance.entity.CareerSkill;
import com.datavalley.careerguidance.entity.EducationLevel;
import com.datavalley.careerguidance.entity.LearningResource;
import com.datavalley.careerguidance.entity.PersonalityType;
import com.datavalley.careerguidance.entity.Role;
import com.datavalley.careerguidance.entity.Skill;
import com.datavalley.careerguidance.entity.TraitCategory;
import com.datavalley.careerguidance.entity.User;
import com.datavalley.careerguidance.entity.UserProfile;
import com.datavalley.careerguidance.repository.AssessmentQuestionRepository;
import com.datavalley.careerguidance.repository.CareerRepository;
import com.datavalley.careerguidance.repository.CareerSkillRepository;
import com.datavalley.careerguidance.repository.LearningResourceRepository;
import com.datavalley.careerguidance.repository.SkillRepository;
import com.datavalley.careerguidance.repository.UserProfileRepository;
import com.datavalley.careerguidance.repository.UserRepository;

@Configuration
public class DataSeederConfig {

    @Bean
    CommandLineRunner seedData(UserRepository userRepository,
                               UserProfileRepository userProfileRepository,
                               SkillRepository skillRepository,
                               CareerRepository careerRepository,
                               CareerSkillRepository careerSkillRepository,
                               LearningResourceRepository learningResourceRepository,
                               AssessmentQuestionRepository assessmentQuestionRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            seedAdmin(userRepository, userProfileRepository, passwordEncoder);
            Map<String, Skill> skills = seedSkills(skillRepository);
            seedCareers(careerRepository, careerSkillRepository, learningResourceRepository, skills);
            seedQuestions(assessmentQuestionRepository);
        };
    }

    private void seedAdmin(UserRepository userRepository,
                           UserProfileRepository userProfileRepository,
                           PasswordEncoder passwordEncoder) {
        if (userRepository.existsByEmail("admin@careerpilot.local")) {
            return;
        }

        User admin = new User();
        admin.setFullName("Career Pilot Admin");
        admin.setEmail("admin@careerpilot.local");
        admin.setPassword(passwordEncoder.encode("Admin@123"));
        admin.setRole(Role.ROLE_ADMIN);
        User savedAdmin = userRepository.save(admin);

        UserProfile profile = new UserProfile();
        profile.setUser(savedAdmin);
        userProfileRepository.save(profile);
    }

    private Map<String, Skill> seedSkills(SkillRepository skillRepository) {
        if (skillRepository.count() > 0) {
            return skillRepository.findAll().stream().collect(java.util.stream.Collectors.toMap(Skill::getName, skill -> skill));
        }

        return List.of(
            skill("Java", "Programming"),
            skill("Spring Boot", "Framework"),
            skill("SQL", "Database"),
            skill("Python", "Programming"),
            skill("Data Visualization", "Analytics"),
            skill("Excel", "Analytics"),
            skill("Power BI", "Analytics"),
            skill("Statistics", "Analytics"),
            skill("Figma", "Design"),
            skill("UI Research", "Design"),
            skill("HTML/CSS", "Frontend"),
            skill("JavaScript", "Frontend"),
            skill("Networking", "Infrastructure"),
            skill("Linux", "Infrastructure"),
            skill("Cloud Basics", "Cloud"),
            skill("SEO", "Marketing"),
            skill("Content Strategy", "Marketing"),
            skill("Communication", "Soft Skill"),
            skill("Problem Solving", "Soft Skill")
        ).stream()
            .map(skillRepository::save)
            .collect(java.util.stream.Collectors.toMap(Skill::getName, skill -> skill));
    }

    private void seedCareers(CareerRepository careerRepository,
                             CareerSkillRepository careerSkillRepository,
                             LearningResourceRepository learningResourceRepository,
                             Map<String, Skill> skills) {
        if (careerRepository.count() > 0) {
            return;
        }

        Career softwareDeveloper = career(
            "Software Developer",
            "Software Engineering",
            "Build web and backend applications, solve technical problems, and collaborate in product teams.",
            EducationLevel.UNDERGRADUATE,
            "High long-term demand across product, services, fintech, and platform companies.",
            "4 LPA - 18 LPA",
            PersonalityType.ANALYTICAL,
            20, 10, 10, 30, 10, 20,
            setOf("Software", "FinTech", "SaaS"),
            setOf("Strengthen Java and OOP fundamentals", "Build Spring Boot APIs", "Practice SQL and database design", "Create projects and publish on GitHub", "Apply for internships and entry-level developer roles"),
            setOf("Oracle Java Foundations", "AWS Cloud Practitioner")
        );

        Career dataAnalyst = career(
            "Data Analyst",
            "Data & Analytics",
            "Turn raw data into dashboards, business insights, and decision-ready reports.",
            EducationLevel.UNDERGRADUATE,
            "Growing demand in business intelligence, operations analytics, and product teams.",
            "4.5 LPA - 16 LPA",
            PersonalityType.STRUCTURED,
            25, 10, 10, 15, 15, 25,
            setOf("Analytics", "Retail", "Consulting"),
            setOf("Master Excel and data cleaning", "Learn SQL for reporting", "Use Python for analysis", "Build dashboards in Power BI or Tableau", "Create a portfolio with business case studies"),
            setOf("Google Data Analytics", "Microsoft Power BI Data Analyst")
        );

        Career uiUxDesigner = career(
            "UI/UX Designer",
            "Design",
            "Design user journeys, interfaces, and prototypes that make digital products easier to use.",
            EducationLevel.DIPLOMA,
            "Strong demand in startups, agencies, SaaS products, and mobile app teams.",
            "4 LPA - 14 LPA",
            PersonalityType.CREATIVE,
            10, 30, 10, 5, 25, 20,
            setOf("Design", "Product", "E-commerce"),
            setOf("Learn design principles and visual hierarchy", "Practice user research and interviews", "Create wireframes and prototypes in Figma", "Build case studies for a design portfolio", "Present design decisions clearly to teams"),
            setOf("Google UX Design", "Nielsen Norman UX Certification")
        );

        Career cyberSecurityAnalyst = career(
            "Cybersecurity Analyst",
            "Cybersecurity",
            "Protect systems, analyze threats, and improve security posture across digital assets.",
            EducationLevel.UNDERGRADUATE,
            "Security demand continues to rise with cloud adoption and regulatory pressure.",
            "5 LPA - 20 LPA",
            PersonalityType.ANALYTICAL,
            20, 5, 10, 25, 10, 30,
            setOf("Cybersecurity", "Cloud", "Enterprise IT"),
            setOf("Learn networking and Linux fundamentals", "Understand threat models and common vulnerabilities", "Practice log analysis and incident response basics", "Earn an entry-level security certification", "Build lab projects using safe sandbox environments"),
            setOf("CompTIA Security+", "ISC2 CC")
        );

        Career cloudEngineer = career(
            "Cloud Engineer",
            "Cloud Infrastructure",
            "Design, deploy, and maintain cloud platforms, automation, and infrastructure services.",
            EducationLevel.UNDERGRADUATE,
            "Strong future scope driven by migration to cloud-native systems and DevOps practices.",
            "6 LPA - 22 LPA",
            PersonalityType.STRUCTURED,
            20, 5, 10, 30, 10, 25,
            setOf("Cloud", "Infrastructure", "DevOps"),
            setOf("Learn Linux administration", "Understand networking and cloud basics", "Practice deployments on AWS or Azure", "Automate workflows with scripts", "Build portfolio projects for scalable deployments"),
            setOf("AWS Cloud Practitioner", "Azure Fundamentals")
        );

        Career digitalMarketer = career(
            "Digital Marketer",
            "Marketing",
            "Plan campaigns, optimize channels, and measure digital growth across platforms.",
            EducationLevel.DIPLOMA,
            "High opportunity in brands, agencies, and growth-focused startups.",
            "3.5 LPA - 12 LPA",
            PersonalityType.SOCIAL,
            10, 20, 15, 5, 30, 20,
            setOf("Marketing", "Media", "E-commerce"),
            setOf("Understand marketing funnels and audience targeting", "Learn SEO and content planning", "Track campaigns using analytics dashboards", "Build sample campaigns and content calendars", "Practice communication and growth storytelling"),
            setOf("Google Digital Marketing", "HubSpot Content Marketing")
        );

        List<Career> careers = careerRepository.saveAll(List.of(
            softwareDeveloper,
            dataAnalyst,
            uiUxDesigner,
            cyberSecurityAnalyst,
            cloudEngineer,
            digitalMarketer
        ));

        Career savedSoftwareDeveloper = careers.stream().filter(career -> career.getName().equals("Software Developer")).findFirst().orElseThrow();
        Career savedDataAnalyst = careers.stream().filter(career -> career.getName().equals("Data Analyst")).findFirst().orElseThrow();
        Career savedUiUxDesigner = careers.stream().filter(career -> career.getName().equals("UI/UX Designer")).findFirst().orElseThrow();
        Career savedCyberSecurityAnalyst = careers.stream().filter(career -> career.getName().equals("Cybersecurity Analyst")).findFirst().orElseThrow();
        Career savedCloudEngineer = careers.stream().filter(career -> career.getName().equals("Cloud Engineer")).findFirst().orElseThrow();
        Career savedDigitalMarketer = careers.stream().filter(career -> career.getName().equals("Digital Marketer")).findFirst().orElseThrow();

        linkCareerSkill(careerSkillRepository, savedSoftwareDeveloper, skills.get("Java"), 30);
        linkCareerSkill(careerSkillRepository, savedSoftwareDeveloper, skills.get("Spring Boot"), 25);
        linkCareerSkill(careerSkillRepository, savedSoftwareDeveloper, skills.get("SQL"), 20);
        linkCareerSkill(careerSkillRepository, savedSoftwareDeveloper, skills.get("JavaScript"), 10);
        linkCareerSkill(careerSkillRepository, savedSoftwareDeveloper, skills.get("Problem Solving"), 15);

        linkCareerSkill(careerSkillRepository, savedDataAnalyst, skills.get("Excel"), 20);
        linkCareerSkill(careerSkillRepository, savedDataAnalyst, skills.get("SQL"), 25);
        linkCareerSkill(careerSkillRepository, savedDataAnalyst, skills.get("Python"), 20);
        linkCareerSkill(careerSkillRepository, savedDataAnalyst, skills.get("Power BI"), 20);
        linkCareerSkill(careerSkillRepository, savedDataAnalyst, skills.get("Statistics"), 15);

        linkCareerSkill(careerSkillRepository, savedUiUxDesigner, skills.get("Figma"), 30);
        linkCareerSkill(careerSkillRepository, savedUiUxDesigner, skills.get("UI Research"), 25);
        linkCareerSkill(careerSkillRepository, savedUiUxDesigner, skills.get("Communication"), 20);
        linkCareerSkill(careerSkillRepository, savedUiUxDesigner, skills.get("HTML/CSS"), 15);
        linkCareerSkill(careerSkillRepository, savedUiUxDesigner, skills.get("Problem Solving"), 10);

        linkCareerSkill(careerSkillRepository, savedCyberSecurityAnalyst, skills.get("Networking"), 25);
        linkCareerSkill(careerSkillRepository, savedCyberSecurityAnalyst, skills.get("Linux"), 20);
        linkCareerSkill(careerSkillRepository, savedCyberSecurityAnalyst, skills.get("Python"), 10);
        linkCareerSkill(careerSkillRepository, savedCyberSecurityAnalyst, skills.get("Cloud Basics"), 15);
        linkCareerSkill(careerSkillRepository, savedCyberSecurityAnalyst, skills.get("Problem Solving"), 30);

        linkCareerSkill(careerSkillRepository, savedCloudEngineer, skills.get("Linux"), 20);
        linkCareerSkill(careerSkillRepository, savedCloudEngineer, skills.get("Networking"), 20);
        linkCareerSkill(careerSkillRepository, savedCloudEngineer, skills.get("Cloud Basics"), 30);
        linkCareerSkill(careerSkillRepository, savedCloudEngineer, skills.get("Python"), 15);
        linkCareerSkill(careerSkillRepository, savedCloudEngineer, skills.get("SQL"), 15);

        linkCareerSkill(careerSkillRepository, savedDigitalMarketer, skills.get("SEO"), 25);
        linkCareerSkill(careerSkillRepository, savedDigitalMarketer, skills.get("Content Strategy"), 25);
        linkCareerSkill(careerSkillRepository, savedDigitalMarketer, skills.get("Communication"), 20);
        linkCareerSkill(careerSkillRepository, savedDigitalMarketer, skills.get("Excel"), 15);
        linkCareerSkill(careerSkillRepository, savedDigitalMarketer, skills.get("Data Visualization"), 15);

        learningResourceRepository.saveAll(List.of(
            resource(savedSoftwareDeveloper, "Spring Boot REST API Guide", "Course", "https://spring.io/guides/gs/rest-service", "Build a backend portfolio project with Java and Spring Boot."),
            resource(savedSoftwareDeveloper, "SQLBolt", "Practice", "https://sqlbolt.com", "Interactive SQL practice for query fundamentals."),
            resource(savedDataAnalyst, "Google Data Analytics Certificate", "Certification", "https://grow.google/certificates/data-analytics/", "Structured beginner-friendly analytics pathway."),
            resource(savedDataAnalyst, "Power BI Dashboard Project", "Project", "https://learn.microsoft.com/power-bi/", "Build business dashboards using sample sales data."),
            resource(savedUiUxDesigner, "Figma Learn Design", "Course", "https://help.figma.com", "Hands-on interface and prototyping lessons."),
            resource(savedUiUxDesigner, "UX Case Study Template", "Template", "https://www.nngroup.com", "Use a structured format for portfolio storytelling."),
            resource(savedCyberSecurityAnalyst, "OWASP Top 10", "Reference", "https://owasp.org/www-project-top-ten/", "Understand common application security risks."),
            resource(savedCyberSecurityAnalyst, "TryHackMe Intro Labs", "Practice", "https://tryhackme.com", "Safe guided labs for security beginners."),
            resource(savedCloudEngineer, "AWS Skill Builder", "Course", "https://explore.skillbuilder.aws", "Foundational cloud learning paths."),
            resource(savedCloudEngineer, "Linux Journey", "Practice", "https://linuxjourney.com", "Learn shell and Linux administration basics."),
            resource(savedDigitalMarketer, "Google Analytics Academy", "Course", "https://analytics.google.com/analytics/academy/", "Campaign and web analytics basics."),
            resource(savedDigitalMarketer, "HubSpot Content Marketing", "Certification", "https://academy.hubspot.com", "Content planning and inbound marketing training.")
        ));
    }

    private void seedQuestions(AssessmentQuestionRepository questionRepository) {
        if (questionRepository.count() > 0) {
            return;
        }

        questionRepository.saveAll(List.of(
            question("You are given a complex task with unclear steps. What do you do first?", TraitCategory.ANALYTICAL_THINKING,
                "Break it into smaller parts and map dependencies", 4,
                "Ask others to take the lead", 1,
                "Start quickly and adjust later", 2,
                "Look for a creative workaround immediately", 3),
            question("Which activity sounds most engaging to you?", TraitCategory.CREATIVITY,
                "Designing a new interface or concept", 4,
                "Following a process checklist", 2,
                "Debugging server logs", 1,
                "Preparing a presentation", 3),
            question("In group work, how do you naturally contribute?", TraitCategory.LEADERSHIP,
                "Coordinate the team and set direction", 4,
                "Take only individual tasks", 2,
                "Wait until someone assigns work", 1,
                "Support collaboration and keep everyone aligned", 3),
            question("Which task feels most natural?", TraitCategory.TECHNICAL_INCLINATION,
                "Writing code or automating something", 4,
                "Planning a campaign", 2,
                "Designing a poster", 1,
                "Speaking with customers", 3),
            question("When explaining an idea, what matters most to you?", TraitCategory.COMMUNICATION,
                "Making it clear and easy for others to follow", 4,
                "Keeping it short even if some context is missed", 2,
                "Adding technical depth first", 3,
                "Avoiding the explanation unless necessary", 1),
            question("A system is failing in production. What is your default reaction?", TraitCategory.PROBLEM_SOLVING,
                "Investigate the root cause methodically", 4,
                "Escalate immediately without checking details", 1,
                "Try a few fixes and observe patterns", 3,
                "Document the issue and wait", 2)
        ));
    }

    private Skill skill(String name, String category) {
        Skill skill = new Skill();
        skill.setName(name);
        skill.setCategory(category);
        return skill;
    }

    private Career career(String name,
                          String domain,
                          String description,
                          EducationLevel requiredEducation,
                          String futureScope,
                          String salaryRange,
                          PersonalityType preferredPersonality,
                          int analyticalWeight,
                          int creativityWeight,
                          int leadershipWeight,
                          int technicalWeight,
                          int communicationWeight,
                          int problemSolvingWeight,
                          Set<String> relatedIndustries,
                          Set<String> roadmapSteps,
                          Set<String> certifications) {
        Career career = new Career();
        career.setName(name);
        career.setDomain(domain);
        career.setDescription(description);
        career.setRequiredEducation(requiredEducation);
        career.setFutureScope(futureScope);
        career.setSalaryRange(salaryRange);
        career.setPreferredPersonality(preferredPersonality);
        career.setAnalyticalWeight(analyticalWeight);
        career.setCreativityWeight(creativityWeight);
        career.setLeadershipWeight(leadershipWeight);
        career.setTechnicalWeight(technicalWeight);
        career.setCommunicationWeight(communicationWeight);
        career.setProblemSolvingWeight(problemSolvingWeight);
        career.setRelatedIndustries(relatedIndustries);
        career.setRoadmapSteps(roadmapSteps);
        career.setRecommendedCertifications(certifications);
        return career;
    }

    private void linkCareerSkill(CareerSkillRepository repository, Career career, Skill skill, int importanceWeight) {
        CareerSkill careerSkill = new CareerSkill();
        careerSkill.setCareer(career);
        careerSkill.setSkill(skill);
        careerSkill.setImportanceWeight(importanceWeight);
        repository.save(careerSkill);
    }

    private LearningResource resource(Career career, String title, String type, String url, String description) {
        LearningResource resource = new LearningResource();
        resource.setCareer(career);
        resource.setTitle(title);
        resource.setType(type);
        resource.setUrl(url);
        resource.setDescription(description);
        return resource;
    }

    private AssessmentQuestion question(String text,
                                        TraitCategory category,
                                        String optionAText,
                                        int optionAWeight,
                                        String optionBText,
                                        int optionBWeight,
                                        String optionCText,
                                        int optionCWeight,
                                        String optionDText,
                                        int optionDWeight) {
        AssessmentQuestion question = new AssessmentQuestion();
        question.setQuestionText(text);
        question.setTraitCategory(category);
        question.setOptionAText(optionAText);
        question.setOptionAWeight(optionAWeight);
        question.setOptionBText(optionBText);
        question.setOptionBWeight(optionBWeight);
        question.setOptionCText(optionCText);
        question.setOptionCWeight(optionCWeight);
        question.setOptionDText(optionDText);
        question.setOptionDWeight(optionDWeight);
        return question;
    }

    private Set<String> setOf(String... values) {
        return new LinkedHashSet<>(List.of(values));
    }
}
