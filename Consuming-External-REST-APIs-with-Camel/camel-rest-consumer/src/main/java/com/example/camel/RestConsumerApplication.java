package com.example.camel;

import org.apache.camel.main.Main;

public class RestConsumerApplication {
    
    public static void main(String[] args) throws Exception {
        Main main = new Main();
        
        // Add route builders
        main.addRouteBuilder(new RestConsumerRoute());
        main.addRouteBuilder(new AdvancedRestConsumerRoute());
        
        // Configure main
        main.configure().setDurationMaxMessages(50);
        main.configure().setShutdownTimeout(30);
        
        System.out.println("Starting Camel REST Consumer Application...");
        System.out.println("Check the output directory for generated files.");
        
        // Start and keep the application running
        main.run(args);
    }
}

package com.example.camel;

import org.apache.camel.main.Main;

public class RestConsumerApplication {
    
    public static void main(String[] args) throws Exception {
        Main main = new Main();
        
        // Add all route builders
        main.addRouteBuilder(new RestConsumerRoute());
        main.addRouteBuilder(new AdvancedRestConsumerRoute());
        main.addRouteBuilder(new MonitoringRoute());
        
        // Configure main
        main.configure().setDurationMaxMessages(100);
        main.configure().setShutdownTimeout(30);
        
        System.out.println("=== Camel REST Consumer Application ===");
        System.out.println("Features:");
        System.out.println("- Basic REST API consumption");
        System.out.println("- Data processing and transformation");
        System.out.println("- File-based integration");
        System.out.println("- Health monitoring");
        System.out.println("- Performance tracking");
        System.out.println("Check the output directory for generated files.");
        System.out.println("=======================================");
        
        // Start and keep the application running
        main.run(args);
    }
}
