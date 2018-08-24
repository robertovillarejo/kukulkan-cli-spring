package mx.infotec.dads.kukulkan;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import mx.infotec.dads.kukulkan.engine.service.GenerationService;
import mx.infotec.dads.kukulkan.metamodel.context.GeneratorContext;
import mx.infotec.dads.kukulkan.metamodel.foundation.Database;
import mx.infotec.dads.kukulkan.metamodel.foundation.DatabaseType;
import mx.infotec.dads.kukulkan.metamodel.foundation.ProjectConfiguration;
import mx.infotec.dads.kukulkan.metamodel.util.DefaultValues;

@SpringBootApplication
public class KukulkanCliApplication implements CommandLineRunner {
    static final Logger LOGGER = LoggerFactory.getLogger(KukulkanCliApplication.class);
    public static final Pattern APP_NAME_PATTERN = Pattern.compile("^[a-z]*$");
    public static final Pattern PACKAGE_ID_PATTERN = Pattern.compile(
            "^([a-zA-Z_]{1}[a-zA-Z]*){2,10}\\.([a-zA-Z_]{1}[a-zA-Z0-9_]*){1,30}((\\.([a-zA-Z_]{1}[a-zA-Z0-9_]*){1,61})*)?$");

    @Autowired
    private GenerationService generationService;

    public KukulkanCliApplication(GenerationService generationService) {
        this.generationService = generationService;
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(KukulkanCliApplication.class);
        app.setWebEnvironment(false);
        app.run(args);
    }

    public void run(String... args) throws Exception {
        if (args.length < 4) {
            LOGGER.error("Missing arguments!!!");
            LOGGER.info("<project-name> <packaging> <database-type> <path>");
            System.exit(1);
        }
        this.appGenerateProject(args[0], args[1], DatabaseType.valueOf((String) args[2]), args[3]);
    }

    public void appGenerateProject(@NotNull String appName, @NotNull String packaging, DatabaseType databaseType,
            String pathString) throws URISyntaxException {
        LOGGER.info("Generating Project from Archetype...");
        GeneratorContext genCtx = new GeneratorContext();
        Path path = Paths.get(pathString, new String[0]);
        ProjectConfiguration projectConf = DefaultValues.getDefaulProjectConfiguration();
        projectConf.setId(appName);
        projectConf.setPackaging(packaging);
        projectConf.setOutputDir(path);
        projectConf.setTargetDatabase(new Database(databaseType));
        projectConf.addLayers(
                new String[] { "angular-js", "spring-rest", "spring-service", "spring-repository", "domain-core" });
        if (projectConf.getTargetDatabase().getDatabaseType().equals((Object) DatabaseType.SQL_MYSQL)) {
            projectConf.addLayer("liquibase");
        }
        genCtx.put(ProjectConfiguration.class, (Object) projectConf);
        this.generationService.findGeneratorByName("angular-js-archetype-generator").ifPresent(generator -> {
            this.generationService.process(genCtx, generator);
            LOGGER.info("Execute the command app-config --type FRONT_END");
            LOGGER.info(path.toString());
        });
    }

    public static void validateParams(String appName, String packaging) {
        Objects.requireNonNull(appName);
        Objects.requireNonNull(packaging);
        StringBuilder message = new StringBuilder();
        if (!APP_NAME_PATTERN.matcher(appName).matches()) {
            message.append("\n\n\t").append("[app-name] no fullfilment the pattern ").append("^[a-z]*$");
        }
        if (!PACKAGE_ID_PATTERN.matcher(packaging).matches()) {
            message.append("\n\n\t").append("[group-id] no fullfilment the pattern ").append(
                    "^([a-zA-Z_]{1}[a-zA-Z]*){2,10}\\.([a-zA-Z_]{1}[a-zA-Z0-9_]*){1,30}((\\.([a-zA-Z_]{1}[a-zA-Z0-9_]*){1,61})*)?$")
                    .append("\n\n");
        }
    }
}