package uk.org.tdars.toolbox;

import java.io.IOException;
import java.util.Arrays;
import java.util.ResourceBundle;

import org.apache.log4j.PropertyConfigurator;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import joptsimple.OptionParser;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class App extends Application {
    private final ResourceBundle messageBundle = ResourceBundle.getBundle("uk.org.tdars.toolbox.messages");

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("menu.fxml"), messageBundle);
        val scene = new Scene(root);

        primaryStage.setTitle(messageBundle.getString("windowTitle"));
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        val parser = new OptionParser();

        val optHelp = parser
            .acceptsAll(Arrays.asList("?", "h", "help"), "Prints this help message");

        val optLoggingConfiguration = parser
            .acceptsAll(Arrays.asList("l", "logging-configuration"), "Override the default logging configuration file for advanced configurations")
            .withRequiredArg();

        val optVerbose = parser
            .acceptsAll(Arrays.asList("v", "verbose"), "Increase logging verbosity")
            .availableUnless(optLoggingConfiguration);

        val options = parser.parse(args);
        if (options.has(optHelp)) {
            try {
                parser.printHelpOn(System.err);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.exit(0);
        }

        if (options.has(optLoggingConfiguration)) {
            PropertyConfigurator.configure((String) options.valueOf(optLoggingConfiguration));
        } else if (options.has(optVerbose)) {
            PropertyConfigurator.configure(App.class.getResource("logging_verbose.properties"));
            log.debug("Verbose logging enabled!");
        } else {
            PropertyConfigurator.configure(App.class.getResource("logging.properties"));
        }

        log.info("Starting TDARS Toolkit...");
        launch(args);
    }
}
